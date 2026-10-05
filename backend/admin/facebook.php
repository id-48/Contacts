<?php
declare(strict_types=1);

require_once __DIR__ . '/includes/auth_check.php';

$pageTitle = 'Facebook Marketing';
$pageKey = 'facebook';
require __DIR__ . '/includes/header.php';
?>
<form id="facebookForm" class="stack" novalidate>
    <div class="loading-overlay" data-loading-overlay><span class="spinner"></span></div>
    <section class="card">
        <div class="card-head">
            <h2>Facebook SDK</h2>
            <p class="muted">The App ID and Client Token are sent to the app. The Secret Key stays on this server and is never returned.</p>
        </div>
        <div class="form-grid">
            <div class="field">
                <label for="facebook_app_id">Facebook App ID</label>
                <input id="facebook_app_id" type="text" inputmode="numeric" data-key="facebook_app_id" placeholder="123456789012345">
            </div>
            <div class="field">
                <label for="facebook_client_token">Facebook Client Token</label>
                <input id="facebook_client_token" type="text" data-key="facebook_client_token" placeholder="Settings → Advanced → Client token">
            </div>
            <div class="field field-full">
                <label for="facebook_secret_key">Facebook Secret Key</label>
                <div class="input-group">
                    <input id="facebook_secret_key" type="password" data-key="facebook_secret_key" autocomplete="new-password">
                    <button type="button" class="input-addon" data-toggle-password="facebook_secret_key">Show</button>
                </div>
                <p class="field-hint" id="secretHint">Leave empty to keep the current secret.</p>
            </div>
        </div>
        <label class="checkbox">
            <input type="checkbox" data-key="clear_secret"> Remove the saved secret key
        </label>
    </section>
    <div class="save-bar">
        <button class="btn btn-primary" type="submit">Save Facebook settings</button>
    </div>
</form>
<?php require __DIR__ . '/includes/footer.php'; ?>
