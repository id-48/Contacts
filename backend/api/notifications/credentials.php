<?php
declare(strict_types=1);

// Admin-only. The service account private key is write-only and never returned.
require_once __DIR__ . '/../../config/push.php';
require_once __DIR__ . '/../../config/auth.php';

api_bootstrap();
require_admin_api();

$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';
if ($method === 'GET') {
    json_response(true, 'Firebase configuration loaded', service_account_summary());
}
require_method('POST');
verify_csrf();

$file = $_FILES['service_account'] ?? null;
if (!is_array($file) || ($file['error'] ?? UPLOAD_ERR_NO_FILE) !== UPLOAD_ERR_OK || !is_uploaded_file($file['tmp_name'])) {
    validation_error(['service_account' => 'Choose the service account JSON file']);
}
if ($file['size'] > 64 * 1024) {
    validation_error(['service_account' => 'This file is too large for a service account JSON']);
}
$json = (string) file_get_contents($file['tmp_name']);
$parsed = parse_service_account($json);
if (!is_array($parsed)) {
    validation_error(['service_account' => $parsed]);
}

save_service_account($json);
json_response(true, 'Firebase service account saved', service_account_summary());
