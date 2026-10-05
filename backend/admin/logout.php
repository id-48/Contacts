<?php
declare(strict_types=1);

require_once __DIR__ . '/../config/auth.php';

// The token stops other sites from logging the admin out with a forged link.
if (!is_valid_csrf($_GET['token'] ?? null)) {
    header('Location: index.php');
    exit;
}

logout_admin();
header('Location: login.php?logged_out=1');
exit;
