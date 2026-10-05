<?php
declare(strict_types=1);

require_once __DIR__ . '/config.php';

const MAX_LOGIN_ATTEMPTS = 5;
const LOGIN_LOCK_SECONDS = 300;

function start_admin_session(): void
{
    if (session_status() === PHP_SESSION_ACTIVE) {
        return;
    }
    $secure = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off')
        || (($_SERVER['HTTP_X_FORWARDED_PROTO'] ?? '') === 'https');
    session_name('contacts_admin');
    session_set_cookie_params([
        'lifetime' => 0,
        'path' => '/',
        'secure' => $secure,
        'httponly' => true,
        'samesite' => 'Lax',
    ]);
    session_start();
}

function is_admin_logged_in(): bool
{
    start_admin_session();
    return !empty($_SESSION['admin_id']);
}

function current_admin_email(): string
{
    return (string) ($_SESSION['admin_email'] ?? '');
}

function login_admin(int $id, string $email): void
{
    start_admin_session();
    session_regenerate_id(true);
    $_SESSION['admin_id'] = $id;
    $_SESSION['admin_email'] = $email;
    $_SESSION['login_attempts'] = 0;
    unset($_SESSION['login_locked_until']);
}

function logout_admin(): void
{
    start_admin_session();
    $_SESSION = [];
    if (ini_get('session.use_cookies')) {
        $params = session_get_cookie_params();
        setcookie(session_name(), '', time() - 42000, $params['path'], $params['domain'], $params['secure'], $params['httponly']);
    }
    session_destroy();
}

function is_login_locked(): bool
{
    start_admin_session();
    return ($_SESSION['login_locked_until'] ?? 0) > time();
}

function register_failed_login(): void
{
    $_SESSION['login_attempts'] = (int) ($_SESSION['login_attempts'] ?? 0) + 1;
    if ($_SESSION['login_attempts'] >= MAX_LOGIN_ATTEMPTS) {
        $_SESSION['login_locked_until'] = time() + LOGIN_LOCK_SECONDS;
        $_SESSION['login_attempts'] = 0;
    }
}

function require_admin_page(): void
{
    if (!is_admin_logged_in()) {
        header('Location: login.php');
        exit;
    }
}

function require_admin_api(): void
{
    if (!is_admin_logged_in()) {
        json_response(false, 'Unauthorized', null, 401);
    }
}

function csrf_token(): string
{
    start_admin_session();
    if (empty($_SESSION['csrf_token'])) {
        $_SESSION['csrf_token'] = bin2hex(random_bytes(32));
    }
    return $_SESSION['csrf_token'];
}

function is_valid_csrf(?string $token): bool
{
    start_admin_session();
    return is_string($token) && !empty($_SESSION['csrf_token']) && hash_equals($_SESSION['csrf_token'], $token);
}

function verify_csrf(): void
{
    if (!is_valid_csrf($_SERVER['HTTP_X_CSRF_TOKEN'] ?? null)) {
        json_response(false, 'Your session expired. Please reload the page.', null, 403);
    }
}
