<?php
declare(strict_types=1);

require_once __DIR__ . '/includes/auth_check.php';

$pageTitle = 'Send Notification';
$pageKey = 'notifications';
require __DIR__ . '/includes/header.php';
?>
<div class="stack">
    <section class="stat-grid stat-grid-compact">
        <div class="card stat-card"><div><p class="stat-label">Reachable - All</p><p class="stat-value" data-reach="all">–</p></div></div>
        <div class="card stat-card"><div><p class="stat-label">Reachable - Organic</p><p class="stat-value" data-reach="organic">–</p></div></div>
        <div class="card stat-card"><div><p class="stat-label">Reachable - Marketing</p><p class="stat-value" data-reach="marketing">–</p></div></div>
    </section>

    <form id="firebaseForm" class="card" novalidate>
        <div class="card-head">
            <h2>Firebase service account</h2>
            <p class="muted">Firebase Console → Project settings → Service accounts → Generate new private key. Use the same Firebase project as the app. The private key stays on this server and is never shown again.</p>
        </div>
        <p id="firebaseStatus" class="muted">Loading…</p>
        <div class="form-grid">
            <div class="field field-full">
                <label for="service_account">Service account JSON</label>
                <input id="service_account" type="file" data-key="service_account" accept="application/json,.json">
            </div>
        </div>
        <div class="save-bar">
            <button class="btn btn-ghost" type="submit">Upload service account</button>
        </div>
    </form>

    <form id="notificationForm" class="card" novalidate>
        <div class="card-head">
            <h2>Send notification</h2>
            <p class="muted">Tapping the notification opens the link in a Chrome tab, or the app when the link is empty.</p>
        </div>
        <div class="form-grid">
            <div class="field">
                <label for="push_title">Title</label>
                <input id="push_title" type="text" data-key="title" maxlength="100">
            </div>
            <div class="field">
                <label for="push_target">Send to</label>
                <select id="push_target" data-key="target">
                    <option value="all">All users</option>
                    <option value="organic">Organic users</option>
                    <option value="marketing">Marketing users</option>
                </select>
            </div>
            <div class="field field-full">
                <label for="push_body">Description</label>
                <textarea id="push_body" rows="3" data-key="body" maxlength="500"></textarea>
            </div>
            <div class="field">
                <label for="push_image">Image (optional)</label>
                <input id="push_image" type="file" data-key="image" accept="image/jpeg,image/png,image/webp">
                <p class="field-hint">JPG, PNG or WebP, up to 2 MB. A 2:1 image looks best.</p>
            </div>
            <div class="field">
                <label for="push_link">Link (optional)</label>
                <input id="push_link" type="url" data-key="link" placeholder="https://example.com">
            </div>
        </div>
        <p class="field-error" data-error-for="service_account"></p>
        <div class="save-bar">
            <button class="btn btn-primary" type="submit">Send notification</button>
        </div>
    </form>

    <section class="card">
        <div class="card-head"><h2>History</h2></div>
        <div class="table-wrap">
            <table class="table responsive-table">
                <thead>
                <tr>
                    <th>Notification</th>
                    <th>Sent to</th>
                    <th>Users</th>
                    <th>Delivered</th>
                    <th>Failed</th>
                    <th>Date</th>
                </tr>
                </thead>
                <tbody id="pushRows"></tbody>
            </table>
        </div>
    </section>
</div>
<?php require __DIR__ . '/includes/footer.php'; ?>
