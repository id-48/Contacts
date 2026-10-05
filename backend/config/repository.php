<?php
declare(strict_types=1);

require_once __DIR__ . '/database.php';

const BOOL_SETTING_KEYS = [
    'force_update',
    'splash_after_fullscreen_ad',
    'custom_ads_enabled',
    'app_open_on_resume',
    'launcher_enabled',
    'in_app_review_enabled',
    'welcome_enabled',
    'theme_selection_enabled',
    'language_selection_enabled',
    'after_call_organic_enabled',
    'after_call_marketing_enabled',
];

const STRING_SETTING_KEYS = [
    'privacy_policy_url' => 500,
    'app_version' => 20,
    'app_link' => 500,
    'minimum_version' => 20,
    'force_update_message' => 500,
    'custom_ads_url' => 500,
    'facebook_app_id' => 100,
    'facebook_client_token' => 100,
    'intro_flow_organic' => 20,
    'intro_flow_marketing' => 20,
];

const URL_SETTING_KEYS = ['privacy_policy_url', 'app_link', 'custom_ads_url'];
const INTRO_FLOW_KEYS = ['organic' => 'intro_flow_organic', 'marketing' => 'intro_flow_marketing'];

/** Adds settings columns introduced after the database was created. */
function ensure_settings_schema(): void
{
    static $ready = false;
    if ($ready) {
        return;
    }
    $columns = [
        'intro_flow_organic' => "VARCHAR(20) NOT NULL DEFAULT ''",
        'intro_flow_marketing' => "VARCHAR(20) NOT NULL DEFAULT ''",
        'in_app_review_enabled' => 'TINYINT(1) NOT NULL DEFAULT 1',
    ];
    foreach ($columns as $column => $definition) {
        if (!has_column('app_settings', $column)) {
            db()->exec("ALTER TABLE app_settings ADD COLUMN `$column` $definition");
        }
    }
    $ready = true;
}

/**
 * Accepts "4,1,2,3", "[3, 2, 1]" or an array of screen numbers. Returns null when an entry is not an
 * intro screen number or appears twice.
 */
function parse_intro_flow($value): ?array
{
    if (is_array($value)) {
        $parts = array_map(static fn($v): string => trim((string) $v), array_values($value));
    } else {
        $text = trim((string) ($value ?? ''), " \t\n\r[]");
        $parts = $text === '' ? [] : array_map('trim', explode(',', $text));
    }
    $list = [];
    foreach ($parts as $part) {
        if (!preg_match('/^\d$/', $part) || !isset(INTRO_SCREENS[(int) $part]) || in_array((int) $part, $list, true)) {
            return null;
        }
        $list[] = (int) $part;
    }
    return $list;
}

/** The saved intro flow of a source, or the order the old Welcome/Theme/Language switches gave. */
function intro_flow(array $settings, string $source): array
{
    $flow = parse_intro_flow($settings[INTRO_FLOW_KEYS[$source]] ?? '');
    if ($flow && in_array(INTRO_REQUIRED_SCREEN, $flow, true)) {
        return $flow;
    }
    $legacy = [];
    if ($settings['language_selection_enabled']) $legacy[] = 4;
    if ($settings['welcome_enabled']) $legacy[] = 1;
    if ($settings['theme_selection_enabled']) $legacy[] = 2;
    $legacy[] = INTRO_REQUIRED_SCREEN;
    return $legacy;
}

function get_settings(): array
{
    ensure_settings_schema();
    $row = db()->query('SELECT * FROM app_settings WHERE id = 1')->fetch();
    if (!$row) {
        db()->exec('INSERT IGNORE INTO app_settings (id) VALUES (1)');
        $row = db()->query('SELECT * FROM app_settings WHERE id = 1')->fetch();
    }
    foreach (BOOL_SETTING_KEYS as $key) {
        $row[$key] = (bool) $row[$key];
    }
    foreach (INTRO_FLOW_KEYS as $source => $column) {
        $row[$column] = implode(',', intro_flow($row, $source));
    }
    $row['config_version'] = (int) $row['config_version'];
    return $row;
}

/** Settings safe to show in the admin UI (the Facebook secret is replaced with a flag). */
function get_admin_settings(): array
{
    $settings = get_settings();
    $settings['facebook_secret_set'] = $settings['facebook_secret_key'] !== '';
    unset($settings['facebook_secret_key'], $settings['id']);
    return $settings;
}

/**
 * Older app versions used a per-install ID. Moves that row to the stable device ID (or merges it into an
 * existing row) so reinstalling on the same device never counts as a new user.
 */
function adopt_previous_device(string $deviceId, string $previousId): void
{
    if ($previousId === '' || $previousId === $deviceId || !preg_match(DEVICE_ID_PATTERN, $previousId)) {
        return;
    }
    $pdo = db();
    $find = $pdo->prepare('SELECT id, source, created_at FROM users WHERE device_id = :id');
    $find->execute([':id' => $previousId]);
    $old = $find->fetch();
    if (!$old) {
        return;
    }
    $find->execute([':id' => $deviceId]);
    $current = $find->fetch();
    if (!$current) {
        $pdo->prepare('UPDATE users SET device_id = :new WHERE id = :id')->execute([':new' => $deviceId, ':id' => $old['id']]);
        return;
    }
    $pdo->prepare(
        "UPDATE users SET created_at = LEAST(created_at, :created),
            source = IF(source = 'marketing' OR :source = 'marketing', 'marketing', source)
         WHERE id = :id"
    )->execute([':created' => $old['created_at'], ':source' => $old['source'], ':id' => $current['id']]);
    $pdo->prepare('DELETE FROM users WHERE id = :id')->execute([':id' => $old['id']]);
}

function bump_config_version(): void
{
    db()->exec('UPDATE app_settings SET config_version = config_version + 1 WHERE id = 1');
}

/** Adds default screens introduced after the database was created, with all ad flags off. */
function ensure_default_screens(): void
{
    static $ready = false;
    if ($ready) {
        return;
    }
    ensure_ads_schema();
    $pdo = db();
    $existing = [];
    foreach ($pdo->query('SELECT source, screen_key FROM screen_ad_config')->fetchAll() as $r) {
        $existing[$r['source']][$r['screen_key']] = true;
    }
    $copyLegacy = $pdo->prepare(
        'INSERT IGNORE INTO screen_ad_config (source, screen_key, label, native_big_enabled, native_small_enabled, banner_enabled, fullscreen_enabled, sort_order)
         SELECT source, :screen_key, :label, native_big_enabled, native_small_enabled, banner_enabled, fullscreen_enabled, :sort_order
         FROM screen_ad_config WHERE source = :source AND screen_key = :legacy_key'
    );
    $insert = $pdo->prepare(
        'INSERT IGNORE INTO screen_ad_config (source, screen_key, label, sort_order) VALUES (:source, :screen_key, :label, :sort_order)'
    );
    $order = 0;
    foreach (DEFAULT_SCREENS as $key => [$label]) {
        $order++;
        foreach (USER_SOURCES as $source) {
            if (isset($existing[$source][$key])) {
                continue;
            }
            $legacyKey = LEGACY_SCREEN_KEYS[$key][$source] ?? null;
            if ($legacyKey !== null && isset($existing[$source][$legacyKey])) {
                $copyLegacy->execute([
                    ':screen_key' => $key,
                    ':label' => $label,
                    ':sort_order' => $order,
                    ':source' => $source,
                    ':legacy_key' => $legacyKey,
                ]);
                continue;
            }
            $insert->execute([':source' => $source, ':screen_key' => $key, ':label' => $label, ':sort_order' => $order]);
        }
    }
    $stale = [];
    foreach ($existing as $sourceKeys) {
        foreach (array_keys($sourceKeys) as $key) {
            if (!isset(DEFAULT_SCREENS[$key])) {
                $stale[$key] = true;
            }
        }
    }
    if ($stale) {
        $keys = array_keys($stale);
        $placeholders = implode(',', array_fill(0, count($keys), '?'));
        $pdo->prepare("DELETE FROM screen_ad_config WHERE screen_key IN ($placeholders)")->execute($keys);
    }
    $ready = true;
}

function get_screen_configs(string $source): array
{
    ensure_default_screens();
    $stmt = db()->prepare('SELECT * FROM screen_ad_config WHERE source = :s');
    $stmt->execute([':s' => $source]);
    $rows = [];
    foreach ($stmt->fetchAll() as $r) {
        $rows[$r['screen_key']] = $r;
    }
    $screens = [];
    foreach (DEFAULT_SCREENS as $key => [$label, $flags]) {
        $r = $rows[$key] ?? [];
        $screens[] = [
            'screen_key' => $key,
            'label' => $label,
            'flags' => $flags,
            'native_big' => in_array('native_big', $flags, true) && !empty($r['native_big_enabled']),
            'native_small' => in_array('native_small', $flags, true) && !empty($r['native_small_enabled']),
            'banner' => in_array('banner', $flags, true) && !empty($r['banner_enabled']),
            'fullscreen' => in_array('fullscreen', $flags, true) && !empty($r['fullscreen_enabled']),
        ];
    }
    return $screens;
}

function get_ad_units(): array
{
    $units = ['admob' => [], 'tradplus' => []];
    foreach (ADMOB_TYPES as $type) {
        $units['admob'][$type] = ['enabled' => false, 'id' => ''];
    }
    foreach (TRADPLUS_TYPES as $type) {
        $units['tradplus'][$type] = ['enabled' => false, 'id' => ''];
    }
    $rows = db()->query("SELECT provider, ad_type, ad_unit_id, enabled FROM ad_units WHERE provider IN ('admob', 'tradplus')")->fetchAll();
    foreach ($rows as $r) {
        if (isset($units[$r['provider']][$r['ad_type']])) {
            $units[$r['provider']][$r['ad_type']] = [
                'enabled' => (bool) $r['enabled'],
                'id' => $r['ad_unit_id'],
            ];
        }
    }
    return $units;
}

function get_adx_ids(string $source, bool $onlyUsable = false): array
{
    ensure_ads_schema();
    $adx = array_fill_keys(ADX_TYPES, []);
    $stmt = db()->prepare('SELECT ad_type, ad_unit_id, weight, enabled FROM adx_weighted_ids WHERE source = :s ORDER BY ad_type, sort_order, id');
    $stmt->execute([':s' => $source]);
    foreach ($stmt->fetchAll() as $r) {
        if ($onlyUsable && (!(bool) $r['enabled'] || (int) $r['weight'] <= 0 || $r['ad_unit_id'] === '')) {
            continue;
        }
        $adx[$r['ad_type']][] = [
            'id' => $r['ad_unit_id'],
            'weight' => (int) $r['weight'],
        ];
    }
    return $adx;
}

function get_fullscreen_sequence(string $source): array
{
    ensure_ads_schema();
    $stmt = db()->prepare('SELECT ad_type FROM fullscreen_sequence WHERE source = :s AND enabled = 1 ORDER BY sort_order, id');
    $stmt->execute([':s' => $source]);
    return array_column($stmt->fetchAll(), 'ad_type');
}

function has_column(string $table, string $column): bool
{
    $stmt = db()->prepare(
        'SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = :t AND COLUMN_NAME = :c'
    );
    $stmt->execute([':t' => $table, ':c' => $column]);
    return (bool) $stmt->fetchColumn();
}

/**
 * Brings databases created before per-source ads up to date. Must run outside a transaction (DDL
 * commits implicitly). Existing values are kept for organic users and copied to marketing users.
 */
function ensure_ads_schema(): void
{
    static $ready = false;
    if ($ready) {
        return;
    }
    $pdo = db();
    $sourceEnum = "ENUM('organic', 'marketing') NOT NULL DEFAULT 'organic'";

    $pdo->exec(
        "CREATE TABLE IF NOT EXISTS ad_priority (
            source $sourceEnum,
            ad_type ENUM('banner', 'interstitial', 'native', 'reward', 'app_open') NOT NULL,
            priority VARCHAR(50) NOT NULL DEFAULT '',
            failed_priority VARCHAR(50) NOT NULL DEFAULT '',
            updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            PRIMARY KEY (source, ad_type)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
    );
    $tables = ['ad_priority', 'adx_weighted_ids', 'fullscreen_sequence', 'screen_ad_config'];
    if (count(array_filter($tables, static fn(string $t): bool => has_column($t, 'source'))) === count($tables)) {
        $ready = true;
        return;
    }
    $pdo->query("SELECT GET_LOCK('contacts_ads_schema', 30)")->fetchColumn();
    try {
        migrate_ads_schema($sourceEnum);
    } finally {
        $pdo->query("SELECT RELEASE_LOCK('contacts_ads_schema')")->fetchColumn();
    }
    $ready = true;
}

function migrate_ads_schema(string $sourceEnum): void
{
    $pdo = db();
    if (!has_column('ad_priority', 'source')) {
        $pdo->exec("ALTER TABLE ad_priority ADD COLUMN source $sourceEnum FIRST, DROP PRIMARY KEY, ADD PRIMARY KEY (source, ad_type)");
        $pdo->exec("INSERT INTO ad_priority (source, ad_type, priority, failed_priority)
                    SELECT 'marketing', ad_type, priority, failed_priority FROM ad_priority WHERE source = 'organic'");
    }
    if (!has_column('adx_weighted_ids', 'source')) {
        $pdo->exec("ALTER TABLE adx_weighted_ids ADD COLUMN source $sourceEnum AFTER id, ADD KEY idx_adx_source (source)");
        $pdo->exec("INSERT INTO adx_weighted_ids (source, ad_type, ad_unit_id, weight, enabled, sort_order)
                    SELECT 'marketing', ad_type, ad_unit_id, weight, enabled, sort_order FROM adx_weighted_ids WHERE source = 'organic'");
    }
    if (!has_column('fullscreen_sequence', 'source')) {
        $pdo->exec("ALTER TABLE fullscreen_sequence ADD COLUMN source $sourceEnum AFTER id, ADD KEY idx_fullscreen_source (source)");
        $pdo->exec("INSERT INTO fullscreen_sequence (source, ad_type, sort_order, enabled)
                    SELECT 'marketing', ad_type, sort_order, enabled FROM fullscreen_sequence WHERE source = 'organic'");
    }
    if (!has_column('screen_ad_config', 'source')) {
        $pdo->exec("ALTER TABLE screen_ad_config ADD COLUMN source $sourceEnum AFTER id,
                    DROP INDEX uq_screen_ad_config_screen_key, ADD UNIQUE KEY uq_screen_ad_config_source_key (source, screen_key)");
        $pdo->exec("INSERT INTO screen_ad_config (source, screen_key, label, native_big_enabled, native_small_enabled, banner_enabled, fullscreen_enabled, sort_order)
                    SELECT 'marketing', screen_key, label, native_big_enabled, native_small_enabled, banner_enabled, fullscreen_enabled, sort_order
                    FROM screen_ad_config WHERE source = 'organic'");
    }
}

/**
 * Accepts "[1,2,3,4]", "1, 2, 3" or an array of numbers. Returns null when an entry is not a
 * provider number, or appears twice unless $allowRepeats (then at most PRIORITY_SEQUENCE_MAX entries).
 */
function parse_priority($value, bool $allowRepeats = false): ?array
{
    if (is_array($value)) {
        $parts = array_map(static fn($v): string => trim((string) $v), array_values($value));
    } else {
        $text = trim((string) ($value ?? ''), " \t\n\r[]");
        $parts = $text === '' ? [] : array_map('trim', explode(',', $text));
    }
    $list = [];
    foreach ($parts as $part) {
        if (!preg_match('/^\d$/', $part) || !isset(PRIORITY_PROVIDERS[(int) $part])
            || (!$allowRepeats && in_array((int) $part, $list, true))) {
            return null;
        }
        $list[] = (int) $part;
    }
    return $allowRepeats && count($list) > PRIORITY_SEQUENCE_MAX ? null : $list;
}

function get_ad_priorities(string $source): array
{
    ensure_ads_schema();
    $priorities = [];
    foreach (AD_TYPES as $type) {
        $priorities[$type] = ['main' => DEFAULT_PRIORITY, 'failed' => []];
    }
    $stmt = db()->prepare('SELECT ad_type, priority, failed_priority FROM ad_priority WHERE source = :s');
    $stmt->execute([':s' => $source]);
    foreach ($stmt->fetchAll() as $r) {
        if (!isset($priorities[$r['ad_type']])) {
            continue;
        }
        $main = parse_priority($r['priority'], true) ?: DEFAULT_PRIORITY;
        $priorities[$r['ad_type']] = ['main' => $main, 'failed' => parse_priority($r['failed_priority']) ?? []];
    }
    return $priorities;
}

function get_ads_config(): array
{
    $settings = get_settings();
    $units = get_ad_units();
    return [
        'admob' => $units['admob'],
        'tradplus' => $units['tradplus'],
        'custom' => [
            'enabled' => $settings['custom_ads_enabled'],
            'url' => $settings['custom_ads_url'],
        ],
        'by_source' => [
            'organic' => get_source_ads('organic'),
            'marketing' => get_source_ads('marketing'),
        ],
    ];
}

/** Priority, ADX IDs, fullscreen sequence and per-screen ads of one user source (organic / marketing). */
function get_source_ads(string $source): array
{
    return [
        'priority' => get_ad_priorities($source),
        'adx' => get_adx_ids($source),
        'fullscreen_sequence' => get_fullscreen_sequence($source),
        'screen_ads' => get_screen_configs($source),
    ];
}

function get_public_source_ads(string $source): array
{
    $toKeys = static fn(array $numbers): array => array_map(static fn(int $n): string => PRIORITY_PROVIDERS[$n], $numbers);
    $priority = [];
    foreach (get_ad_priorities($source) as $type => $p) {
        $priority[$type] = ['main' => $toKeys($p['main']), 'failed' => $toKeys($p['failed'])];
    }
    $screenAds = [];
    foreach (get_screen_configs($source) as $screen) {
        $screenAds[$screen['screen_key']] = [
            'native_big' => $screen['native_big'],
            'native_small' => $screen['native_small'],
            'banner' => $screen['banner'],
            'fullscreen' => $screen['fullscreen'],
        ];
    }
    return [
        'priority' => $priority,
        'adx' => get_adx_ids($source, true),
        'fullscreen_sequence' => get_fullscreen_sequence($source),
        'screen_ads' => (object) $screenAds,
    ];
}

/** The public configuration returned to the Android app. Never contains secrets. */
function build_public_config(): array
{
    $s = get_settings();

    $units = get_ad_units();
    $customEnabled = $s['custom_ads_enabled'] && $s['custom_ads_url'] !== '';
    $organic = get_public_source_ads('organic');
    $introFlows = [];
    foreach (USER_SOURCES as $source) {
        $introFlows[$source] = array_map(static fn(int $n): string => INTRO_SCREENS[$n], intro_flow($s, $source));
    }

    return [
        'config_version' => $s['config_version'],
        'app' => [
            'app_version' => $s['app_version'],
            'app_link' => $s['app_link'],
            'privacy_policy' => $s['privacy_policy_url'],
        ],
        'force_update' => [
            'enabled' => $s['force_update'],
            'minimum_version' => $s['minimum_version'],
            'message' => $s['force_update_message'],
            'app_link' => $s['app_link'],
        ],
        'screens' => [
            'welcome' => in_array('welcome', $introFlows['organic'], true),
            'theme_selection' => in_array('theme_selection', $introFlows['organic'], true),
            'language_selection' => in_array('language_selection', $introFlows['organic'], true),
            'after_call_organic' => $s['after_call_organic_enabled'],
            'after_call_marketing' => $s['after_call_marketing_enabled'],
        ],
        'onboarding' => [
            'organic' => $introFlows['organic'],
            'marketing' => $introFlows['marketing'],
        ],
        'features' => [
            'splash_after_fullscreen_ad' => $s['splash_after_fullscreen_ad'],
            'app_open_on_resume' => $s['app_open_on_resume'],
            'launcher_enabled' => $s['launcher_enabled'],
            'in_app_review' => $s['in_app_review_enabled'],
        ],
        'marketing' => [
            'facebook_app_id' => $s['facebook_app_id'],
            'facebook_client_token' => $s['facebook_client_token'],
        ],
        'ads' => [
            'admob' => $units['admob'],
            'tradplus' => $units['tradplus'],
            'custom' => [
                'enabled' => $customEnabled,
                'url' => $customEnabled ? $s['custom_ads_url'] : '',
            ],
            // Organic values at the top level keep app versions without by_source working.
            'screen_ads' => $organic['screen_ads'],
            'adx' => $organic['adx'],
            'priority' => $organic['priority'],
            'fullscreen_sequence' => $organic['fullscreen_sequence'],
            'by_source' => [
                'organic' => $organic,
                'marketing' => get_public_source_ads('marketing'),
            ],
        ],
    ];
}
