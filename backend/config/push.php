<?php
declare(strict_types=1);

require_once __DIR__ . '/repository.php';

const PUSH_TARGETS = ['all', 'organic', 'marketing'];
const PUSH_TITLE_MAX = 100;
const PUSH_BODY_MAX = 500;
const PUSH_IMAGE_MAX_BYTES = 2 * 1024 * 1024;
const PUSH_IMAGE_TYPES = ['image/jpeg' => 'jpg', 'image/png' => 'png', 'image/webp' => 'webp'];
const PUSH_UPLOAD_DIR = __DIR__ . '/../uploads/notifications';
const PUSH_BATCH_SIZE = 100;
const FCM_SCOPE = 'https://www.googleapis.com/auth/firebase.messaging';

function ensure_push_schema(): void
{
    static $ready = false;
    if ($ready) {
        return;
    }
    $pdo = db();
    if (!has_column('users', 'fcm_token')) {
        $pdo->exec('ALTER TABLE users ADD COLUMN fcm_token VARCHAR(255) NULL DEFAULT NULL, ADD KEY idx_users_fcm_token (fcm_token(32))');
    }
    $pdo->exec(
        "CREATE TABLE IF NOT EXISTS push_credentials (
            id TINYINT UNSIGNED NOT NULL,
            service_account MEDIUMTEXT NOT NULL,
            updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            PRIMARY KEY (id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
    );
    $pdo->exec(
        "CREATE TABLE IF NOT EXISTS push_notifications (
            id INT UNSIGNED NOT NULL AUTO_INCREMENT,
            title VARCHAR(100) NOT NULL,
            body VARCHAR(500) NOT NULL,
            image_url VARCHAR(500) NOT NULL DEFAULT '',
            link VARCHAR(500) NOT NULL DEFAULT '',
            target ENUM('all', 'organic', 'marketing') NOT NULL DEFAULT 'all',
            total_count INT UNSIGNED NOT NULL DEFAULT 0,
            success_count INT UNSIGNED NOT NULL DEFAULT 0,
            failure_count INT UNSIGNED NOT NULL DEFAULT 0,
            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (id),
            KEY idx_push_created_at (created_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
    );
    $ready = true;
}

/** Returns the parsed service account, or an error message when the JSON is not usable. */
function parse_service_account(string $json)
{
    $data = json_decode($json, true);
    if (!is_array($data) || ($data['type'] ?? '') !== 'service_account') {
        return 'This is not a Firebase service account JSON file';
    }
    foreach (['project_id', 'client_email', 'private_key'] as $key) {
        if (!is_string($data[$key] ?? null) || trim($data[$key]) === '') {
            return "The service account JSON has no $key";
        }
    }
    if (openssl_pkey_get_private($data['private_key']) === false) {
        return 'The private key in the service account JSON is not valid';
    }
    return $data;
}

function get_service_account(): ?array
{
    ensure_push_schema();
    $json = db()->query('SELECT service_account FROM push_credentials WHERE id = 1')->fetchColumn();
    if (!is_string($json) || $json === '') {
        return null;
    }
    $data = parse_service_account($json);
    return is_array($data) ? $data : null;
}

function save_service_account(string $json): void
{
    ensure_push_schema();
    db()->prepare(
        'INSERT INTO push_credentials (id, service_account) VALUES (1, :json)
         ON DUPLICATE KEY UPDATE service_account = VALUES(service_account)'
    )->execute([':json' => $json]);
}

/** Safe summary for the admin UI; the private key is never returned. */
function service_account_summary(): array
{
    $sa = get_service_account();
    return [
        'set' => $sa !== null,
        'project_id' => $sa['project_id'] ?? '',
        'client_email' => $sa['client_email'] ?? '',
    ];
}

function base64url(string $data): string
{
    return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
}

function fcm_access_token(array $sa): string
{
    $now = time();
    $tokenUri = $sa['token_uri'] ?? 'https://oauth2.googleapis.com/token';
    $header = base64url(json_encode(['alg' => 'RS256', 'typ' => 'JWT']));
    $claims = base64url(json_encode([
        'iss' => $sa['client_email'],
        'scope' => FCM_SCOPE,
        'aud' => $tokenUri,
        'iat' => $now,
        'exp' => $now + 3600,
    ]));
    $signature = '';
    if (!openssl_sign("$header.$claims", $signature, $sa['private_key'], OPENSSL_ALGO_SHA256)) {
        throw new RuntimeException('Could not sign the Firebase token request');
    }
    $ch = curl_init($tokenUri);
    curl_setopt_array($ch, [
        CURLOPT_POST => true,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_TIMEOUT => 20,
        CURLOPT_POSTFIELDS => http_build_query([
            'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
            'assertion' => "$header.$claims." . base64url($signature),
        ]),
    ]);
    $response = curl_exec($ch);
    $status = (int) curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $data = is_string($response) ? json_decode($response, true) : null;
    if ($status !== 200 || !is_string($data['access_token'] ?? null)) {
        throw new RuntimeException('Firebase rejected the service account (HTTP ' . $status . ')');
    }
    return $data['access_token'];
}

function push_tokens(string $target): array
{
    ensure_push_schema();
    $sql = "SELECT id, fcm_token FROM users WHERE fcm_token IS NOT NULL AND fcm_token <> ''";
    $params = [];
    if ($target !== 'all') {
        $sql .= ' AND source = :source';
        $params[':source'] = $target;
    }
    $stmt = db()->prepare($sql);
    $stmt->execute($params);
    return $stmt->fetchAll(PDO::FETCH_KEY_PAIR);
}

function push_reachable_counts(): array
{
    ensure_push_schema();
    $rows = db()->query(
        "SELECT source, COUNT(*) AS n FROM users WHERE fcm_token IS NOT NULL AND fcm_token <> '' GROUP BY source"
    )->fetchAll(PDO::FETCH_KEY_PAIR);
    $organic = (int) ($rows['organic'] ?? 0);
    $marketing = (int) ($rows['marketing'] ?? 0);
    return ['all' => $organic + $marketing, 'organic' => $organic, 'marketing' => $marketing];
}

function is_unregistered_token_error(int $status, ?array $body): bool
{
    if ($status === 404) {
        return true;
    }
    foreach ($body['error']['details'] ?? [] as $detail) {
        if (($detail['errorCode'] ?? '') === 'UNREGISTERED') {
            return true;
        }
    }
    return false;
}

/** Sends one data message per token. Returns [success, failure]; dead tokens are cleared. */
function fcm_send(array $sa, array $tokens, array $data): array
{
    $accessToken = fcm_access_token($sa);
    $url = 'https://fcm.googleapis.com/v1/projects/' . rawurlencode($sa['project_id']) . '/messages:send';
    $success = 0;
    $failure = 0;
    $deadUserIds = [];

    foreach (array_chunk($tokens, PUSH_BATCH_SIZE, true) as $batch) {
        $multi = curl_multi_init();
        $handles = [];
        foreach ($batch as $userId => $token) {
            $ch = curl_init($url);
            curl_setopt_array($ch, [
                CURLOPT_POST => true,
                CURLOPT_RETURNTRANSFER => true,
                CURLOPT_TIMEOUT => 20,
                CURLOPT_HTTPHEADER => ['Authorization: Bearer ' . $accessToken, 'Content-Type: application/json'],
                CURLOPT_POSTFIELDS => json_encode([
                    'message' => [
                        'token' => $token,
                        'data' => $data,
                        'android' => ['priority' => 'high'],
                    ],
                ], JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE),
            ]);
            curl_multi_add_handle($multi, $ch);
            $handles[$userId] = $ch;
        }
        do {
            $status = curl_multi_exec($multi, $running);
            if ($running) {
                curl_multi_select($multi, 1.0);
            }
        } while ($running && $status === CURLM_OK);

        foreach ($handles as $userId => $ch) {
            $code = (int) curl_getinfo($ch, CURLINFO_HTTP_CODE);
            if ($code === 200) {
                $success++;
            } else {
                $failure++;
                $body = json_decode((string) curl_multi_getcontent($ch), true);
                if (is_unregistered_token_error($code, is_array($body) ? $body : null)) {
                    $deadUserIds[] = (int) $userId;
                }
            }
            curl_multi_remove_handle($multi, $ch);
        }
        curl_multi_close($multi);
    }

    foreach (array_chunk($deadUserIds, 500) as $ids) {
        $placeholders = implode(',', array_fill(0, count($ids), '?'));
        db()->prepare("UPDATE users SET fcm_token = NULL WHERE id IN ($placeholders)")->execute($ids);
    }
    return [$success, $failure];
}

/** Stores an uploaded image under uploads/notifications and returns its public URL. */
function store_push_image(array $file): string
{
    if (($file['error'] ?? UPLOAD_ERR_NO_FILE) !== UPLOAD_ERR_OK || !is_uploaded_file($file['tmp_name'])) {
        throw new InvalidArgumentException('The image could not be uploaded');
    }
    if ($file['size'] > PUSH_IMAGE_MAX_BYTES) {
        throw new InvalidArgumentException('The image must be 2 MB or smaller');
    }
    $mime = (string) (new finfo(FILEINFO_MIME_TYPE))->file($file['tmp_name']);
    if (!isset(PUSH_IMAGE_TYPES[$mime]) || @getimagesize($file['tmp_name']) === false) {
        throw new InvalidArgumentException('Use a JPG, PNG or WebP image');
    }
    if (!is_dir(PUSH_UPLOAD_DIR) && !mkdir(PUSH_UPLOAD_DIR, 0755, true) && !is_dir(PUSH_UPLOAD_DIR)) {
        throw new RuntimeException('The uploads folder is not writable');
    }
    $name = bin2hex(random_bytes(16)) . '.' . PUSH_IMAGE_TYPES[$mime];
    if (!move_uploaded_file($file['tmp_name'], PUSH_UPLOAD_DIR . '/' . $name)) {
        throw new RuntimeException('The uploads folder is not writable');
    }
    return backend_base_url() . '/uploads/notifications/' . $name;
}

function backend_base_url(): string
{
    $https = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off')
        || (($_SERVER['HTTP_X_FORWARDED_PROTO'] ?? '') === 'https');
    $root = rtrim(str_replace('\\', '/', dirname($_SERVER['SCRIPT_NAME'] ?? '/', 3)), '/');
    return ($https ? 'https' : 'http') . '://' . ($_SERVER['HTTP_HOST'] ?? 'localhost') . $root;
}

function push_history(int $limit = 50): array
{
    ensure_push_schema();
    $stmt = db()->prepare(
        'SELECT id, title, body, image_url, link, target, total_count, success_count, failure_count, created_at
         FROM push_notifications ORDER BY id DESC LIMIT :limit'
    );
    $stmt->bindValue(':limit', $limit, PDO::PARAM_INT);
    $stmt->execute();
    $rows = $stmt->fetchAll();
    foreach ($rows as &$row) {
        foreach (['id', 'total_count', 'success_count', 'failure_count'] as $key) {
            $row[$key] = (int) $row[$key];
        }
    }
    return $rows;
}
