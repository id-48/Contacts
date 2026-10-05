<?php
declare(strict_types=1);

require_once __DIR__ . '/includes/auth_check.php';

$pageTitle = 'Dashboard';
$pageKey = 'dashboard';
require __DIR__ . '/includes/header.php';
?>
<section class="stat-grid">
    <div class="card stat-card">
        <span class="stat-icon stat-icon-teal">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm0 3c1.7 0 3 1.3 3 3s-1.3 3-3 3-3-1.3-3-3 1.3-3 3-3zm0 14.2a7.2 7.2 0 0 1-6-3.2c0-2 4-3.1 6-3.1s6 1.1 6 3.1a7.2 7.2 0 0 1-6 3.2z"/></svg>
        </span>
        <div>
            <p class="stat-label">Organic Users</p>
            <p class="stat-value" data-stat="organic"><span class="skeleton-text"></span></p>
        </div>
    </div>
    <div class="card stat-card">
        <span class="stat-icon stat-icon-amber">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M18 11v2h4v-2h-4zm-2 6.6c1 .7 2.2 1.6 3.2 2.4l1.2-1.6c-1-.8-2.2-1.7-3.2-2.4L16 17.6zM20.4 5.6 19.2 4c-1 .7-2.2 1.6-3.2 2.4L17.2 8c1-.8 2.2-1.6 3.2-2.4zM4 9a2 2 0 0 0-2 2v2c0 1.1.9 2 2 2h1v4h2v-4h1l5 3V6L8 9H4zm11.5 3c0-1.3-.6-2.5-1.5-3.4v6.8c.9-.9 1.5-2.1 1.5-3.4z"/></svg>
        </span>
        <div>
            <p class="stat-label">Marketing Users</p>
            <p class="stat-value" data-stat="marketing"><span class="skeleton-text"></span></p>
        </div>
    </div>
    <div class="card stat-card">
        <span class="stat-icon stat-icon-blue">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M16 11a4 4 0 1 0-3.9-5A4 4 0 0 0 16 11zM8 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6zm0 2c-2.7 0-8 1.3-8 4v2h7v-2.2c0-1.4.8-2.6 2-3.5A12 12 0 0 0 8 13zm8 0c-2.7 0-8 1.3-8 4v2h16v-2c0-2.7-5.3-4-8-4z"/></svg>
        </span>
        <div>
            <p class="stat-label">Total Users</p>
            <p class="stat-value" data-stat="total"><span class="skeleton-text"></span></p>
        </div>
    </div>
</section>

<section class="grid-2">
    <div class="card">
        <div class="card-head">
            <h2>Source breakdown</h2>
        </div>
        <div class="breakdown-bar" id="breakdownBar">
            <span class="breakdown-organic" style="width:50%"></span>
            <span class="breakdown-marketing" style="width:50%"></span>
        </div>
        <ul class="legend">
            <li><span class="dot dot-teal"></span>Organic: <strong data-stat="organic_percent">–</strong></li>
            <li><span class="dot dot-amber"></span>Marketing: <strong data-stat="marketing_percent">–</strong></li>
        </ul>
    </div>
    <div class="card">
        <div class="card-head">
            <h2>New users · last 7 days</h2>
        </div>
        <div class="bar-chart" id="weekChart"></div>
        <ul class="legend">
            <li><span class="dot dot-teal"></span>Organic</li>
            <li><span class="dot dot-amber"></span>Marketing</li>
        </ul>
    </div>
</section>
<?php require __DIR__ . '/includes/footer.php'; ?>
