<?php
declare(strict_types=1);

require_once __DIR__ . '/../../config/auth.php';

require_admin_page();
header('X-Frame-Options: DENY');
header('X-Content-Type-Options: nosniff');
header('Referrer-Policy: same-origin');
