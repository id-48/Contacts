<?php
declare(strict_types=1);

/*
 * Temporary database diagnostic. Open it once in the browser, read the result, then DELETE this file.
 * It never prints the database password.
 */

require_once __DIR__ . '/config/database.php';

header('Content-Type: text/plain; charset=utf-8');

echo "PHP version: " . PHP_VERSION . "\n";
echo "pdo_mysql loaded: " . (extension_loaded('pdo_mysql') ? 'yes' : 'NO') . "\n";
echo "DB host/port: " . DB_HOST . ':' . DB_PORT . "\n";
echo "DB name: " . DB_NAME . "\n";
echo "DB user: " . DB_USER . "\n\n";

try {
    $pdo = db();
    echo "Connection: OK\n";
} catch (Throwable $e) {
    echo "Connection: FAILED\n" . $e->getMessage() . "\n";
    exit;
}

$required = ['admins', 'users', 'app_settings', 'ad_units', 'adx_weighted_ids', 'fullscreen_sequence', 'screen_ad_config'];
$existing = $pdo->query('SHOW TABLES')->fetchAll(PDO::FETCH_COLUMN);
foreach ($required as $table) {
    echo str_pad($table, 22) . (in_array($table, $existing, true) ? 'OK' : 'MISSING (import sql/contacts_admin.sql)') . "\n";
}

if (in_array('admins', $existing, true)) {
    $count = (int) $pdo->query('SELECT COUNT(*) FROM admins')->fetchColumn();
    echo "\nAdmins: $count" . ($count === 0 ? " (open setup_admin.php once to create the admin)" : '') . "\n";
}
if (in_array('app_settings', $existing, true)) {
    $row = (int) $pdo->query('SELECT COUNT(*) FROM app_settings WHERE id = 1')->fetchColumn();
    echo "app_settings row: " . ($row ? 'OK' : 'MISSING (re-import sql/contacts_admin.sql)') . "\n";
}
