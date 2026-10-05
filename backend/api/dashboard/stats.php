<?php
declare(strict_types=1);

require_once __DIR__ . '/../../config/database.php';
require_once __DIR__ . '/../../config/auth.php';

api_bootstrap();
require_method('GET');
require_admin_api();

$counts = ['organic' => 0, 'marketing' => 0];
foreach (db()->query('SELECT source, COUNT(*) AS total FROM users GROUP BY source')->fetchAll() as $row) {
    $counts[$row['source']] = (int) $row['total'];
}
$total = $counts['organic'] + $counts['marketing'];

$percent = static fn(int $value): float => $total > 0 ? round($value * 100 / $total, 1) : 0.0;

// New users per day for the last 7 days, split by source.
$days = [];
for ($i = 6; $i >= 0; $i--) {
    $date = date('Y-m-d', strtotime("-{$i} days"));
    $days[$date] = ['date' => $date, 'organic' => 0, 'marketing' => 0];
}
$stmt = db()->prepare(
    'SELECT DATE(created_at) AS day, source, COUNT(*) AS total
     FROM users
     WHERE created_at >= :since
     GROUP BY DATE(created_at), source'
);
$stmt->execute([':since' => array_key_first($days) . ' 00:00:00']);
foreach ($stmt->fetchAll() as $row) {
    if (isset($days[$row['day']])) {
        $days[$row['day']][$row['source']] = (int) $row['total'];
    }
}

json_response(true, 'Statistics loaded', [
    'organic' => $counts['organic'],
    'marketing' => $counts['marketing'],
    'total' => $total,
    'organic_percent' => $percent($counts['organic']),
    'marketing_percent' => $percent($counts['marketing']),
    'last_7_days' => array_values($days),
]);
