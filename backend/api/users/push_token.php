<?php
declare(strict_types=1);

// Public endpoint called by the Android app whenever its Firebase Cloud Messaging token changes.
require_once __DIR__ . '/../../config/push.php';

api_bootstrap();
require_method('POST');

$body = read_json_body();
$deviceId = clean_string($body['device_id'] ?? '', 100);
$source = strtolower(clean_string($body['source'] ?? '', 20));
$appVersion = clean_string($body['app_version'] ?? '', 20);
$token = clean_string($body['fcm_token'] ?? '', 255);
$previousId = clean_string($body['previous_device_id'] ?? '', 100);

if (!preg_match(DEVICE_ID_PATTERN, $deviceId)) {
    json_response(false, 'Invalid device_id', null, 422);
}
if (!preg_match('/^[A-Za-z0-9_:\-]{20,255}$/', $token)) {
    json_response(false, 'Invalid fcm_token', null, 422);
}
if (!in_array($source, USER_SOURCES, true)) {
    $source = 'organic';
}
if ($appVersion !== '' && !preg_match('/^[0-9A-Za-z.\-]{1,20}$/', $appVersion)) {
    $appVersion = '';
}

ensure_push_schema();
adopt_previous_device($deviceId, $previousId);
$pdo = db();
$pdo->prepare('UPDATE users SET fcm_token = NULL WHERE fcm_token = :token AND device_id <> :device_id')
    ->execute([':token' => $token, ':device_id' => $deviceId]);
$pdo->prepare(
    "INSERT INTO users (device_id, source, app_version, fcm_token, last_seen_at)
     VALUES (:device_id, :source, :app_version, :token, NOW())
     ON DUPLICATE KEY UPDATE
        source = IF(source = 'marketing', 'marketing', VALUES(source)),
        app_version = IF(VALUES(app_version) = '', app_version, VALUES(app_version)),
        fcm_token = VALUES(fcm_token),
        last_seen_at = NOW()"
)->execute([':device_id' => $deviceId, ':source' => $source, ':app_version' => $appVersion, ':token' => $token]);

json_response(true, 'Token saved');
