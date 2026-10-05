<?php
declare(strict_types=1);

require_once __DIR__ . '/config.php';

// Prefer environment variables on the server; the fallbacks are for local development.
define('DB_HOST', getenv('DB_HOST') ?: 'localhost');
define('DB_PORT', getenv('DB_PORT') ?: '3306');
define('DB_NAME', getenv('DB_NAME') ?: 'u387533797_contacts');
define('DB_USER', getenv('DB_USER') ?: 'u387533797_contacts');
define('DB_PASS', getenv('DB_PASS') ?: 'U387533797_contacts');

function db(): PDO
{
    static $pdo = null;
    if ($pdo instanceof PDO) {
        return $pdo;
    }
    $dsn = sprintf('mysql:host=%s;port=%s;dbname=%s;charset=utf8mb4', DB_HOST, DB_PORT, DB_NAME);
    $pdo = new PDO($dsn, DB_USER, DB_PASS, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES => false,
    ]);
    // Keep MySQL dates aligned with PHP (UTC) so dashboard day buckets match.
    $pdo->exec("SET time_zone = '+00:00'");
    return $pdo;
}
