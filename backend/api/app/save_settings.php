<?php
declare(strict_types=1);

require_once __DIR__ . '/../../config/repository.php';
require_once __DIR__ . '/../../config/auth.php';

api_bootstrap();
require_method('POST');
require_admin_api();
verify_csrf();

// Facebook fields are managed by api/marketing/facebook_config.php only.
const EXCLUDED_KEYS = ['facebook_app_id', 'facebook_client_token'];

$body = read_json_body();
$current = get_settings();
$updates = [];

foreach (BOOL_SETTING_KEYS as $key) {
    if (array_key_exists($key, $body)) {
        $updates[$key] = to_bool($body[$key]);
    }
}
foreach (STRING_SETTING_KEYS as $key => $maxLength) {
    if (!in_array($key, EXCLUDED_KEYS, true) && array_key_exists($key, $body)) {
        $updates[$key] = clean_string($body[$key], $maxLength);
    }
}

if (!$updates) {
    json_response(false, 'Nothing to save', null, 400);
}

// Validate against the merged state so cross-field rules hold.
$merged = array_merge($current, $updates);
$errors = [];

foreach (URL_SETTING_KEYS as $key) {
    if (array_key_exists($key, $updates) && $merged[$key] !== '' && !is_valid_url($merged[$key])) {
        $errors[$key] = 'Enter a valid URL (https://...)';
    }
}
if ($merged['app_version'] !== '' && !is_valid_version($merged['app_version'])) {
    $errors['app_version'] = 'Use a version like 1.0.0';
}
if ($merged['force_update']) {
    if ($merged['app_version'] === '') {
        $errors['app_version'] = 'App version is required when force update is enabled';
    }
    if ($merged['minimum_version'] === '' || !is_valid_version($merged['minimum_version'])) {
        $errors['minimum_version'] = 'A valid minimum version (e.g. 1.2.0) is required';
    }
    if ($merged['force_update_message'] === '') {
        $errors['force_update_message'] = 'Update message is required when force update is enabled';
    }
    if ($merged['app_link'] === '') {
        $errors['app_link'] = 'App link is required when force update is enabled';
    }
} elseif ($merged['minimum_version'] !== '' && !is_valid_version($merged['minimum_version'])) {
    $errors['minimum_version'] = 'Use a version like 1.0.0';
}
if ($merged['custom_ads_enabled'] && $merged['custom_ads_url'] === '') {
    $errors['custom_ads_url'] = 'Custom Ads URL is required when Custom Ads is enabled';
}
foreach (INTRO_FLOW_KEYS as $key) {
    if (!array_key_exists($key, $updates)) {
        continue;
    }
    $flow = parse_intro_flow($updates[$key]);
    if ($flow === null) {
        $errors[$key] = 'Use numbers 1-4 without repeats, e.g. 4,1,2,3';
    } elseif (!in_array(INTRO_REQUIRED_SCREEN, $flow, true)) {
        $errors[$key] = 'Default (3) is required, e.g. 4,1,2,3';
    } else {
        $updates[$key] = implode(',', $flow);
    }
}
if ($errors) {
    validation_error($errors);
}

$sets = [];
$params = [];
foreach ($updates as $key => $value) {
    $sets[] = "`$key` = :$key";
    $params[":$key"] = is_bool($value) ? (int) $value : $value;
}

$pdo = db();
$pdo->beginTransaction();
$pdo->prepare('UPDATE app_settings SET ' . implode(', ', $sets) . ' WHERE id = 1')->execute($params);
bump_config_version();
$pdo->commit();

json_response(true, 'Settings saved', get_admin_settings());
