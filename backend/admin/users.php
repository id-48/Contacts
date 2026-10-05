<?php
declare(strict_types=1);

require_once __DIR__ . '/includes/auth_check.php';

$pageTitle = 'Users';
$pageKey = 'users';
require __DIR__ . '/includes/header.php';
?>
<section class="stat-grid stat-grid-compact">
    <div class="card stat-card"><div><p class="stat-label">Organic</p><p class="stat-value" data-stat="organic">–</p></div></div>
    <div class="card stat-card"><div><p class="stat-label">Marketing</p><p class="stat-value" data-stat="marketing">–</p></div></div>
    <div class="card stat-card"><div><p class="stat-label">Total</p><p class="stat-value" data-stat="total">–</p></div></div>
</section>

<section class="card">
    <div class="toolbar">
        <input type="search" id="userSearch" placeholder="Search device ID" maxlength="100">
        <select id="userSource">
            <option value="">All sources</option>
            <option value="organic">Organic</option>
            <option value="marketing">Marketing</option>
        </select>
    </div>
    <div class="table-wrap">
        <table class="table responsive-table">
            <thead>
            <tr>
                <th>Device ID</th>
                <th>Source</th>
                <th>App version</th>
                <th>Installed</th>
                <th>Last seen</th>
            </tr>
            </thead>
            <tbody id="userRows"></tbody>
        </table>
    </div>
    <div class="pagination">
        <button class="btn btn-ghost btn-sm" type="button" id="prevPage">Previous</button>
        <span id="pageInfo" class="muted"></span>
        <button class="btn btn-ghost btn-sm" type="button" id="nextPage">Next</button>
    </div>
</section>
<?php require __DIR__ . '/includes/footer.php'; ?>
