<?php
declare(strict_types=1);

require_once __DIR__ . '/../../config/database.php';
require_once __DIR__ . '/../../config/auth.php';

api_bootstrap();
require_method('POST');
start_admin_session();
verify_csrf();

if (is_login_locked()) {
    json_response(false, 'Too many attempts. Please wait a few minutes and try again.', null, 429);
}

$body = read_json_body();
$email = strtolower(clean_string($body['email'] ?? '', 191));
$password = (string) ($body['password'] ?? '');

$errors = [];
if ($email === '' || filter_var($email, FILTER_VALIDATE_EMAIL) === false) {
    $errors['email'] = 'Enter a valid email address';
}
if ($password === '') {
    $errors['password'] = 'Enter your password';
}
if ($errors) {
    validation_error($errors);
}

$stmt = db()->prepare('SELECT id, email, password_hash FROM admins WHERE email = :email LIMIT 1');
$stmt->execute([':email' => $email]);
$admin = $stmt->fetch();

if (!$admin || !password_verify($password, $admin['password_hash'])) {
    register_failed_login();
    json_response(false, 'Invalid email or password', null, 401);
}

if (password_needs_rehash($admin['password_hash'], PASSWORD_DEFAULT)) {
    $update = db()->prepare('UPDATE admins SET password_hash = :hash WHERE id = :id');
    $update->execute([':hash' => password_hash($password, PASSWORD_DEFAULT), ':id' => $admin['id']]);
}

login_admin((int) $admin['id'], $admin['email']);
json_response(true, 'Login successful', ['redirect' => 'index.php', 'csrf_token' => csrf_token()]);
