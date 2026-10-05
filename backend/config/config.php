<?php
declare(strict_types=1);

// 'production' hides PHP warnings/notices from every response.
define('APP_ENV', getenv('APP_ENV') ?: 'production');
define('APP_NAME', 'Contacts Admin');

const SCREEN_AD_FLAGS = ['native_big', 'native_small', 'banner', 'fullscreen'];
const DEFAULT_SCREENS = [
    'welcome' => ['Welcome', SCREEN_AD_FLAGS],
    'theme_selection' => ['Theme Selection', SCREEN_AD_FLAGS],
    'default_phone' => ['Default Phone', SCREEN_AD_FLAGS],
    'launcher' => ['Launcher', ['native_small']],
    'launcher_app' => ['Launcher - App click', ['fullscreen']],
    'language_selection' => ['Language', ['native_small']],
    'language_selection_back' => ['Language - Back', ['fullscreen']],
    'language_selection_done' => ['Language - Done', ['fullscreen']],
    'main' => ['Main', ['native_small']],
    'main_search' => ['Main - Search icon', ['fullscreen']],
    'main_settings' => ['Main - Settings icon', ['fullscreen']],
    'contacts_create' => ['Contacts - Create contact', ['fullscreen']],
    'contacts_add' => ['Contacts - + button', ['fullscreen']],
    'contacts_info' => ['Contacts - Contact info', ['fullscreen']],
    'favorites_add' => ['Favorites - Add card', ['fullscreen']],
    'contact_details' => ['Contact Details', ['native_big', 'banner']],
    'contact_details_back' => ['Contact Details - Back', ['fullscreen']],
    'contact_details_block' => ['Contact Details - Block', ['fullscreen']],
    'contact_details_delete' => ['Contact Details - Delete', ['fullscreen']],
    'contact_details_delete_cancel' => ['Contact Details - Delete cancel', ['fullscreen']],
    'contact_details_call_history' => ['Contact Details - Call history (menu)', ['fullscreen']],
    'contact_details_show_more' => ['Contact Details - Show more', ['fullscreen']],
    'edit_contact_close' => ['Edit Contact - Close', ['fullscreen']],
    'edit_contact_done' => ['Edit Contact - Done', ['fullscreen']],
    'call_history' => ['Call History', ['native_small']],
    'call_history_back' => ['Call History - Back', ['fullscreen']],
    'call_history_delete' => ['Call History - Delete', ['fullscreen']],
    'call_history_delete_cancel' => ['Call History - Delete cancel', ['fullscreen']],
    'search' => ['Search', ['native_small']],
    'search_back' => ['Search - Back', ['fullscreen']],
    'settings' => ['Settings', ['native_small']],
    'settings_back' => ['Settings - Back', ['fullscreen']],
    'settings_language' => ['Settings - Language', ['fullscreen']],
    'settings_theme' => ['Settings - Theme', ['fullscreen']],
    'settings_blocked' => ['Settings - Blocked numbers', ['fullscreen']],
    'settings_quick_response' => ['Settings - Quick responses', ['fullscreen']],
    'settings_widget' => ['Settings - Widget', ['fullscreen']],
    'theme' => ['Theme', ['native_small']],
    'theme_back' => ['Theme - Back', ['fullscreen']],
    'blocked_numbers' => ['Blocked Numbers', ['native_small']],
    'blocked_numbers_back' => ['Blocked Numbers - Back', ['fullscreen']],
    'blocked_numbers_add' => ['Blocked Numbers - Add number', ['fullscreen']],
    'blocked_numbers_block' => ['Blocked Numbers - Block (dialog)', ['fullscreen']],
    'quick_response' => ['Quick Responses', ['native_small']],
    'quick_response_back' => ['Quick Responses - Back', ['fullscreen']],
    'quick_response_add' => ['Quick Responses - Add (dialog)', ['fullscreen']],
    'after_call' => ['After Call', ['native_big']],
    'after_call_close' => ['After Call - Close', ['fullscreen']],
    'dialer_back' => ['Dial Pad - Back', ['fullscreen']],
    'call' => ['Call', ['native_small']],
];
const LEGACY_SCREEN_KEYS = ['after_call' => ['organic' => 'after_call_organic', 'marketing' => 'after_call_marketing']];
const AD_PROVIDERS = ['admob', 'tradplus', 'adx', 'custom'];
const AD_TYPES = ['banner', 'interstitial', 'native', 'reward', 'app_open'];
const ADMOB_TYPES = ['banner', 'interstitial', 'native', 'reward', 'app_open'];
const TRADPLUS_TYPES = ['banner', 'interstitial', 'native', 'reward'];
const ADX_TYPES = ['banner', 'interstitial', 'native', 'app_open', 'reward'];
const FULLSCREEN_TYPES = ['interstitial', 'reward', 'app_open', 'custom'];
const PRIORITY_PROVIDERS = [1 => 'admob', 2 => 'adx', 3 => 'tradplus', 4 => 'custom'];
const DEFAULT_PRIORITY = [1, 2, 3, 4];
const PRIORITY_SEQUENCE_MAX = 25;
const USER_SOURCES = ['organic', 'marketing'];
const INTRO_SCREENS = [1 => 'welcome', 2 => 'theme_selection', 3 => 'default_phone', 4 => 'language_selection'];
const INTRO_REQUIRED_SCREEN = 3;
const DEVICE_ID_PATTERN = '/^[A-Za-z0-9._:-]{8,100}$/';

error_reporting(E_ALL);
ini_set('display_errors', APP_ENV === 'production' ? '0' : '1');
ini_set('log_errors', '1');
date_default_timezone_set('UTC');

function json_response(bool $success, string $message, $data = null, int $status = 200): void
{
    if (!headers_sent()) {
        http_response_code($status);
        header('Content-Type: application/json; charset=utf-8');
        header('Cache-Control: no-store');
        header('X-Content-Type-Options: nosniff');
    }
    echo json_encode(
        ['success' => $success, 'message' => $message, 'data' => $data],
        JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE
    );
    exit;
}

function validation_error(array $errors): void
{
    json_response(false, 'Please fix the highlighted fields', ['errors' => $errors], 422);
}

/** Turns any uncaught error inside an API script into a safe JSON 500. */
function api_bootstrap(): void
{
    set_exception_handler(static function (Throwable $e): void {
        error_log('[contacts-admin] ' . $e->getMessage() . ' @ ' . $e->getFile() . ':' . $e->getLine());
        json_response(false, 'Something went wrong. Please try again.', null, 500);
    });
    set_error_handler(static function (int $severity, string $message, string $file, int $line): bool {
        throw new ErrorException($message, 0, $severity, $file, $line);
    });
}

function require_method(string $method): void
{
    if (($_SERVER['REQUEST_METHOD'] ?? 'GET') !== $method) {
        header('Allow: ' . $method);
        json_response(false, 'Method not allowed', null, 405);
    }
}

function read_json_body(): array
{
    $raw = file_get_contents('php://input');
    if ($raw === false || trim($raw) === '') {
        return [];
    }
    $data = json_decode($raw, true);
    if (!is_array($data)) {
        json_response(false, 'Invalid JSON body', null, 400);
    }
    return $data;
}

function to_bool($value): bool
{
    if (is_bool($value)) {
        return $value;
    }
    if (is_int($value)) {
        return $value === 1;
    }
    return in_array(strtolower(trim((string) $value)), ['1', 'true', 'on', 'yes'], true);
}

function clean_string($value, int $maxLength = 255): string
{
    return mb_substr(trim((string) ($value ?? '')), 0, $maxLength);
}

function is_valid_url(string $url): bool
{
    if (filter_var($url, FILTER_VALIDATE_URL) === false) {
        return false;
    }
    $scheme = strtolower((string) parse_url($url, PHP_URL_SCHEME));
    return in_array($scheme, ['http', 'https', 'market'], true);
}

function is_valid_version(string $version): bool
{
    return (bool) preg_match('/^\d+(\.\d+){0,3}$/', $version);
}

function is_valid_ad_unit_id(string $id): bool
{
    return $id === '' || (bool) preg_match('/^\S{1,191}$/', $id);
}

/** URL of a file in assets/, versioned by its modification time so browsers never use a stale copy. */
function asset(string $path): string
{
    $file = __DIR__ . '/../assets/' . $path;
    $version = is_file($file) ? (string) filemtime($file) : '1';
    return '../assets/' . $path . '?v=' . $version;
}

function e(?string $value): string
{
    return htmlspecialchars((string) $value, ENT_QUOTES, 'UTF-8');
}
