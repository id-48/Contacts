<?php
declare(strict_types=1);

// Public endpoint called by the Android app once per install (and again if the source changes).
require_once __DIR__ . '/../../config/repository.php';

api_bootstrap();
require_method('POST');

$body = read_json_body();
$deviceId = clean_string($body['device_id'] ?? '', 100);
$source = strtolower(clean_string($body['source'] ?? '', 20));
$appVersion = clean_string($body['app_version'] ?? '', 20);
$previousId = clean_string($body['previous_device_id'] ?? '', 100);

if (!preg_match(DEVICE_ID_PATTERN, $deviceId)) {
    json_response(false, 'Invalid device_id', null, 422);
}
if (!in_array($source, USER_SOURCES, true)) {
    $source = 'organic';
}
if ($appVersion !== '' && !preg_match('/^[0-9A-Za-z.\-]{1,20}$/', $appVersion)) {
    $appVersion = '';
}

adopt_previous_device($deviceId, $previousId);

// A user may move from organic to marketing (late attribution), never back.
$stmt = db()->prepare(
    "INSERT INTO users (device_id, source, app_version, last_seen_at)
     VALUES (:device_id, :source, :app_version, NOW())
     ON DUPLICATE KEY UPDATE
        source = IF(source = 'marketing', 'marketing', VALUES(source)),
        app_version = IF(VALUES(app_version) = '', app_version, VALUES(app_version)),
        last_seen_at = NOW()"
);
$stmt->execute([':device_id' => $deviceId, ':source' => $source, ':app_version' => $appVersion]);

$row = db()->prepare('SELECT source FROM users WHERE device_id = :device_id');
$row->execute([':device_id' => $deviceId]);

json_response(true, 'User saved', ['source' => $row->fetchColumn() ?: $source]);
