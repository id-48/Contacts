<?php
declare(strict_types=1);

require_once __DIR__ . '/includes/auth_check.php';

$pageTitle = 'App Settings';
$pageKey = 'settings';

/** Renders one ON/OFF row. */
function toggle_row(string $key, string $title, string $hint = ''): void
{
    ?>
    <div class="toggle-row">
        <div>
            <p class="toggle-title"><?= e($title) ?></p>
            <?php if ($hint !== ''): ?><p class="toggle-hint"><?= e($hint) ?></p><?php endif; ?>
        </div>
        <label class="switch">
            <input type="checkbox" data-key="<?= e($key) ?>">
            <span class="slider"></span>
        </label>
    </div>
    <?php
}

require __DIR__ . '/includes/header.php';
?>
<form id="settingsForm" class="stack" novalidate>
    <div class="loading-overlay" data-loading-overlay><span class="spinner"></span></div>

    <section class="card">
        <div class="card-head">
            <h2>Intro flow</h2>
            <p class="muted">The screens a new user sees after the splash, in this order. A screen not in the list is not shown. Default (3) is required.</p>
        </div>
        <ul class="priority-legend">
            <li><span class="chip-num">1</span>Welcome</li>
            <li><span class="chip-num">2</span>Theme</li>
            <li><span class="chip-num">3</span>Default</li>
            <li><span class="chip-num">4</span>Language</li>
        </ul>
        <div class="form-grid">
            <div class="field">
                <label for="intro_flow_organic">Organic</label>
                <input id="intro_flow_organic" type="text" data-key="intro_flow_organic" placeholder="4,1,2,3" maxlength="20" spellcheck="false" autocomplete="off">
            </div>
            <div class="field">
                <label for="intro_flow_marketing">Marketing</label>
                <input id="intro_flow_marketing" type="text" data-key="intro_flow_marketing" placeholder="4,1,2,3" maxlength="20" spellcheck="false" autocomplete="off">
            </div>
        </div>
    </section>

    <section class="card">
        <div class="card-head">
            <h2>Screens</h2>
            <p class="muted">When a screen is OFF, the app skips it automatically.</p>
        </div>
        <?php
        toggle_row('after_call_organic_enabled', 'After Call Screen - Organic', 'Shown to organic users after a call.');
        toggle_row('after_call_marketing_enabled', 'After Call Screen - Marketing', 'Shown to marketing users after a call.');
        ?>
    </section>

    <section class="card">
        <div class="card-head"><h2>App information</h2></div>
        <div class="form-grid">
            <div class="field">
                <label for="privacy_policy_url">Privacy Policy Link</label>
                <input id="privacy_policy_url" type="url" data-key="privacy_policy_url" placeholder="https://example.com/privacy">
            </div>
            <div class="field">
                <label for="app_version">App Version</label>
                <input id="app_version" type="text" data-key="app_version" placeholder="1.0.0">
            </div>
            <div class="field field-full">
                <label for="app_link">App Link (Play Store)</label>
                <input id="app_link" type="url" data-key="app_link" placeholder="https://play.google.com/store/apps/details?id=...">
            </div>
        </div>
    </section>

    <section class="card">
        <div class="card-head"><h2>Features</h2></div>
        <?php
        toggle_row('splash_after_fullscreen_ad', 'Splash Screen After Full Screen Ad', 'Show one fullscreen ad when the splash screen finishes.');
        toggle_row('custom_ads_enabled', 'Custom Ads', 'The Custom Ads URL is set in Ads Configuration.');
        toggle_row('app_open_on_resume', 'App Open Ad On Resume', 'Show an App Open ad when the user returns to the app.');
        toggle_row('in_app_review_enabled', 'In-App Review', 'Ask for a Play Store review once, the second time the user opens the app after onboarding.');
        toggle_row('launcher_enabled', 'Launcher Mode', 'The Default Phone screen also requires setting the app as the default home app; the Home button then opens the Launcher screen (installed apps).');
        ?>
    </section>

    <section class="card">
        <div class="card-head">
            <h2>Force Update</h2>
            <p class="muted">Users on a version lower than the minimum version must update. The App Link above is used.</p>
        </div>
        <?php toggle_row('force_update', 'Force Update'); ?>
        <div class="form-grid">
            <div class="field">
                <label for="minimum_version">Minimum Version</label>
                <input id="minimum_version" type="text" data-key="minimum_version" placeholder="1.0.0">
            </div>
            <div class="field field-full">
                <label for="force_update_message">Update Message</label>
                <textarea id="force_update_message" rows="3" data-key="force_update_message" maxlength="500"></textarea>
            </div>
        </div>
    </section>

    <div class="save-bar">
        <button class="btn btn-primary" type="submit">Save settings</button>
    </div>
</form>
<?php require __DIR__ . '/includes/footer.php'; ?>
