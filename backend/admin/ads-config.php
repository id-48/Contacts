<?php
declare(strict_types=1);

require_once __DIR__ . '/includes/auth_check.php';

$pageTitle = 'Ads Configuration';
$pageKey = 'ads';
require __DIR__ . '/includes/header.php';
?>
<form id="adsForm" class="stack" novalidate>
    <div class="loading-overlay" data-loading-overlay><span class="spinner"></span></div>

    <section class="grid-2">
        <div class="card">
            <div class="card-head">
                <h2>AdMob</h2>
                <p class="muted">Turn on a format and enter its ad unit ID.</p>
            </div>
            <div data-units="admob"></div>
        </div>
        <div class="card">
            <div class="card-head">
                <h2>TradPlus</h2>
                <p class="muted">Turn on a format and enter its TradPlus unit ID.</p>
            </div>
            <div data-units="tradplus"></div>
        </div>
    </section>

    <section class="card">
        <div class="card-head">
            <h2>Custom Ads</h2>
            <p class="muted">When Custom (4) is picked for a fullscreen ad, the app opens this link in a Chrome tab, then shows an App Open ad after the tab is closed. Custom is skipped for Banner and Native.</p>
        </div>
        <div class="toggle-row">
            <div><p class="toggle-title">Enabled</p></div>
            <label class="switch">
                <input type="checkbox" data-key="custom.enabled">
                <span class="slider"></span>
            </label>
        </div>
        <div class="field">
            <label for="customUrl">Custom Ads URL</label>
            <input id="customUrl" type="url" data-key="custom.url" placeholder="https://example.com">
        </div>
    </section>

    <section class="card">
        <div class="card-head">
            <h2>Organic / Marketing</h2>
            <p class="muted">Priority, ADX Weighted IDs, Fullscreen sequence and Per-screen ads are set separately for each user type. Organic users get the Organic values, users from marketing campaigns get the Marketing values.</p>
        </div>
        <div class="source-bar">
            <div class="segmented" role="tablist" aria-label="User type">
                <button type="button" role="tab" class="segmented-btn is-active" data-source-tab="organic" aria-selected="true">Organic</button>
                <button type="button" role="tab" class="segmented-btn" data-source-tab="marketing" aria-selected="false">Marketing</button>
            </div>
            <button class="btn btn-ghost btn-sm" type="button" data-copy-source>Copy to Marketing</button>
        </div>
    </section>

    <?php foreach (['organic' => 'Organic', 'marketing' => 'Marketing'] as $source => $sourceLabel): ?>
        <div class="stack" role="tabpanel" data-source-panel="<?= e($source) ?>"<?= $source === 'organic' ? '' : ' hidden' ?>>
            <section class="card">
                <div class="card-head">
                    <h2>Priority <span class="source-badge source-<?= e($source) ?>"><?= e($sourceLabel) ?></span></h2>
                    <p class="muted">The first field is a sequence: each ad request uses the next network in it, then it starts again. Repeats are allowed, e.g. 1,1,2,3,1 gives AdMob, AdMob, ADX, TradPlus, AdMob. If that network fails, the app tries the "failed" list.</p>
                </div>
                <ul class="priority-legend">
                    <li><span class="chip-num">1</span>AdMob</li>
                    <li><span class="chip-num">2</span>ADX</li>
                    <li><span class="chip-num">3</span>TradPlus</li>
                    <li><span class="chip-num">4</span>Custom</li>
                </ul>
                <div class="priority-grid">
                    <?php foreach (['banner' => 'Banner', 'interstitial' => 'Interstitial', 'native' => 'Native', 'reward' => 'Reward', 'app_open' => 'App Open'] as $type => $label): ?>
                        <?php foreach (['main' => [$label, '1,1,2,3,1', 60], 'failed' => [$label . ' failed', '3,4', 20]] as $field => [$fieldLabel, $placeholder, $maxLength]): ?>
                            <?php $inputId = "$source-priority-$type-$field"; ?>
                            <div class="field">
                                <label for="<?= e($inputId) ?>"><?= e($fieldLabel) ?></label>
                                <input id="<?= e($inputId) ?>" type="text" data-key="<?= e("$source.priority.$type.$field") ?>"
                                       placeholder="<?= e($placeholder) ?>" maxlength="<?= (int) $maxLength ?>" spellcheck="false" autocomplete="off">
                            </div>
                        <?php endforeach; ?>
                    <?php endforeach; ?>
                </div>
            </section>

            <section class="card">
                <div class="card-head">
                    <h2>ADX Weighted IDs <span class="source-badge source-<?= e($source) ?>"><?= e($sourceLabel) ?></span></h2>
                    <p class="muted">Each request picks one ID at random, in proportion to its weight. Weights do not need to add up to 100.</p>
                </div>
                <div class="adx-grid" data-adx-groups></div>
            </section>

            <section class="card">
                <div class="card-head">
                    <h2>Fullscreen sequence <span class="source-badge source-<?= e($source) ?>"><?= e($sourceLabel) ?></span></h2>
                    <p class="muted">Click a type to append it. Click a chip in the sequence to remove it. The app shows them in this order and loops.</p>
                </div>
                <div class="chip-palette" data-sequence-palette></div>
                <div class="sequence" data-sequence-list></div>
                <p class="field-error" data-error-for="<?= e("$source.fullscreen_sequence") ?>"></p>
                <button class="btn btn-ghost btn-sm" type="button" data-sequence-reset>Reset to default</button>
            </section>

            <section class="card">
                <div class="card-head">
                    <h2>Per-screen ads <span class="source-badge source-<?= e($source) ?>"><?= e($sourceLabel) ?></span></h2>
                    <p class="muted">Choose which ad formats appear on each screen. "Back" rows also apply to the phone's system back on that screen.</p>
                </div>
                <div class="table-wrap">
                    <table class="table screen-table">
                        <thead>
                        <tr>
                            <th>Screen</th>
                            <th>Native Big</th>
                            <th>Native Small</th>
                            <th>Banner</th>
                            <th>Fullscreen</th>
                        </tr>
                        </thead>
                        <tbody data-screen-rows></tbody>
                    </table>
                </div>
                <p class="field-error" data-error-for="<?= e("$source.screen_ads") ?>"></p>
            </section>
        </div>
    <?php endforeach; ?>

    <div class="save-bar">
        <button class="btn btn-primary" type="submit">Save ads configuration</button>
    </div>
</form>
<?php require __DIR__ . '/includes/footer.php'; ?>
