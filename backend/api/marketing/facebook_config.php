<?php
declare(strict_types=1);

// Admin-only. The secret key is write-only: it is never returned, not even to the admin UI.
require_once __DIR__ . '/../../config/repository.php';
require_once __DIR__ . '/../../config/auth.php';

api_bootstrap();
require_admin_api();

function facebook_payload(): array
{
    $s = get_settings();
    return [
        'facebook_app_id' => $s['facebook_app_id'],
        'facebook_client_token' => $s['facebook_client_token'],
        'facebook_secret_set' => $s['facebook_secret_key'] !== '',
    ];
}

$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';
if ($method === 'GET') {
    json_response(true, 'Facebook configuration loaded', facebook_payload());
}
require_method('POST');
verify_csrf();

$body = read_json_body();
$appId = clean_string($body['facebook_app_id'] ?? '', 100);
$clientToken = clean_string($body['facebook_client_token'] ?? '', 100);
$secret = clean_string($body['facebook_secret_key'] ?? '', 255);
$clearSecret = to_bool($body['clear_secret'] ?? false);

$errors = [];
if ($appId !== '' && !preg_match('/^\d{5,30}$/', $appId)) {
    $errors['facebook_app_id'] = 'Facebook App ID contains digits only';
}
if ($clientToken !== '' && !preg_match('/^[A-Za-z0-9]{8,100}$/', $clientToken)) {
    $errors['facebook_client_token'] = 'Client token contains letters and digits only';
}
if ($secret !== '' && !preg_match('/^\S{8,255}$/', $secret)) {
    $errors['facebook_secret_key'] = 'Secret key cannot contain spaces';
}
if ($errors) {
    validation_error($errors);
}

$sql = 'UPDATE app_settings SET facebook_app_id = :app_id, facebook_client_token = :token';
$params = [':app_id' => $appId, ':token' => $clientToken];
if ($secret !== '') {
    $sql .= ', facebook_secret_key = :secret';
    $params[':secret'] = $secret;
} elseif ($clearSecret) {
    $sql .= ", facebook_secret_key = ''";
}

$pdo = db();
$pdo->beginTransaction();
$pdo->prepare($sql . ' WHERE id = 1')->execute($params);
bump_config_version();
$pdo->commit();

json_response(true, 'Facebook configuration saved', facebook_payload());
