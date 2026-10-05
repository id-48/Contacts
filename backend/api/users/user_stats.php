<?php
declare(strict_types=1);

require_once __DIR__ . '/../../config/database.php';
require_once __DIR__ . '/../../config/auth.php';

api_bootstrap();
require_method('GET');
require_admin_api();

const PAGE_SIZE = 25;

$page = max(1, (int) ($_GET['page'] ?? 1));
$source = (string) ($_GET['source'] ?? '');
$search = clean_string($_GET['search'] ?? '', 100);

$where = [];
$params = [];
if (in_array($source, USER_SOURCES, true)) {
    $where[] = 'source = :source';
    $params[':source'] = $source;
}
if ($search !== '') {
    $where[] = 'device_id LIKE :search';
    $params[':search'] = '%' . addcslashes($search, '%_\\') . '%';
}
$whereSql = $where ? 'WHERE ' . implode(' AND ', $where) : '';

$countStmt = db()->prepare("SELECT COUNT(*) FROM users $whereSql");
$countStmt->execute($params);
$total = (int) $countStmt->fetchColumn();

$offset = ($page - 1) * PAGE_SIZE;
$listStmt = db()->prepare(
    "SELECT device_id, source, app_version, created_at, last_seen_at
     FROM users $whereSql
     ORDER BY created_at DESC, id DESC
     LIMIT " . PAGE_SIZE . " OFFSET $offset"
);
$listStmt->execute($params);

$summary = ['organic' => 0, 'marketing' => 0];
foreach (db()->query('SELECT source, COUNT(*) AS total FROM users GROUP BY source')->fetchAll() as $row) {
    $summary[$row['source']] = (int) $row['total'];
}

json_response(true, 'Users loaded', [
    'summary' => $summary + ['total' => $summary['organic'] + $summary['marketing']],
    'users' => $listStmt->fetchAll(),
    'page' => $page,
    'page_size' => PAGE_SIZE,
    'total' => $total,
    'pages' => max(1, (int) ceil($total / PAGE_SIZE)),
]);
