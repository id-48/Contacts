<?php
declare(strict_types=1);

/*
 * Creates the initial admin account with a secure password hash.
 * Run once after importing sql/contacts_admin.sql:
 *   php setup_admin.php
 * Browser access only works while no admin exists. Delete this file after use.
 */

require_once __DIR__ . '/config/database.php';

const INITIAL_ADMIN_EMAIL = 'admin@gmail.com';
const INITIAL_ADMIN_PASSWORD = 'Yagnik2324@';

$isCli = PHP_SAPI === 'cli';
if (!$isCli) {
    header('Content-Type: text/plain; charset=utf-8');
}

try {
    $pdo = db();
    $count = (int) $pdo->query('SELECT COUNT(*) FROM admins')->fetchColumn();

    if ($count > 0) {
        if (!$isCli) {
            http_response_code(403);
        }
        echo "An admin already exists. Nothing to do.\n";
        exit;
    }

    $stmt = $pdo->prepare('INSERT INTO admins (email, password_hash) VALUES (:email, :hash)');
    $stmt->execute([
        ':email' => INITIAL_ADMIN_EMAIL,
        ':hash' => password_hash(INITIAL_ADMIN_PASSWORD, PASSWORD_DEFAULT),
    ]);

    echo "Admin created: " . INITIAL_ADMIN_EMAIL . "\n";
    echo "Delete setup_admin.php from the server now.\n";
} catch (Throwable $e) {
    error_log('[contacts-admin] setup failed: ' . $e->getMessage());
    if (!$isCli) {
        http_response_code(500);
    }
    echo "Setup failed. Check database credentials in config/database.php.\n\n";
    echo "Database: " . DB_NAME . " | User: " . DB_USER . " | Host: " . DB_HOST . ':' . DB_PORT . "\n";
    echo "Error: " . $e->getMessage() . "\n";
    exit(1);
}
