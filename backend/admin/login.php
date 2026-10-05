<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/auth.php';

if (is_admin_logged_in()) {
    header('Location: index.php');
    exit;
}
header('X-Frame-Options: DENY');
$loggedOut = isset($_GET['logged_out']);
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="csrf-token" content="<?= e(csrf_token()) ?>">
    <title>Login · <?= e(APP_NAME) ?></title>
    <link rel="stylesheet" href="<?= e(asset('css/admin.css')) ?>">
</head>
<body data-page="login" class="login-body">
<div class="login-wrap">
    <form class="card login-card" id="loginForm" novalidate>
        <div class="login-brand">
            <span class="brand-logo brand-logo-lg">
                <svg viewBox="0 0 24 24" width="26" height="26"><path fill="currentColor" d="M6.6 10.8a15 15 0 0 0 6.6 6.6l2.2-2.2c.3-.3.7-.4 1-.2 1.1.4 2.3.6 3.6.6.6 0 1 .4 1 1V20c0 .6-.4 1-1 1A17 17 0 0 1 3 4c0-.6.4-1 1-1h3.5c.6 0 1 .4 1 1 0 1.3.2 2.5.6 3.6.1.3 0 .7-.2 1l-2.3 2.2z"/></svg>
            </span>
            <h1><?= e(APP_NAME) ?></h1>
            <p class="muted">Sign in to manage the Contacts app</p>
        </div>
        <?php if ($loggedOut): ?>
            <div class="alert alert-success">You have been logged out.</div>
        <?php endif; ?>
        <div class="field">
            <label for="email">Email</label>
            <input id="email" type="email" data-key="email" autocomplete="username" required>
        </div>
        <div class="field">
            <label for="password">Password</label>
            <div class="input-group">
                <input id="password" type="password" data-key="password" autocomplete="current-password" required>
                <button type="button" class="input-addon" data-toggle-password="password">Show</button>
            </div>
        </div>
        <button class="btn btn-primary btn-block" type="submit">Sign in</button>
    </form>
</div>
<div class="toast-stack" id="toastStack" aria-live="polite"></div>
<script src="<?= e(asset('js/admin.js')) ?>"></script>
</body>
</html>
