<?php
declare(strict_types=1);

require_once __DIR__ . '/../../config/repository.php';
require_once __DIR__ . '/../../config/auth.php';

api_bootstrap();
require_method('POST');
require_admin_api();
verify_csrf();

const MAX_ADX_IDS_PER_TYPE = 20;
const MAX_SEQUENCE_ITEMS = 50;
const MAX_SCREENS = 300;

$body = read_json_body();
$errors = [];

// Ad units (AdMob + TradPlus)
$units = [];
foreach (['admob' => ADMOB_TYPES, 'tradplus' => TRADPLUS_TYPES] as $provider => $types) {
    foreach ($types as $type) {
        $item = $body[$provider][$type] ?? [];
        $id = clean_string(is_array($item) ? ($item['id'] ?? '') : '', 191);
        $enabled = to_bool(is_array($item) ? ($item['enabled'] ?? false) : false);
        if (!is_valid_ad_unit_id($id)) {
            $errors["$provider.$type.id"] = 'Ad unit ID cannot contain spaces';
        } elseif ($enabled && $id === '') {
            $errors["$provider.$type.id"] = 'Enter an ad unit ID or turn this off';
        }
        $units[] = [$provider, $type, $id, $enabled];
    }
}

// Custom ads
$customEnabled = to_bool($body['custom']['enabled'] ?? false);
$customUrl = clean_string($body['custom']['url'] ?? '', 500);
if ($customUrl !== '' && !is_valid_url($customUrl)) {
    $errors['custom.url'] = 'Enter a valid URL (https://...)';
} elseif ($customEnabled && $customUrl === '') {
    $errors['custom.url'] = 'Custom Ads URL is required when enabled';
}

// Per user source: ADX weighted IDs, network priority, fullscreen sequence, per-screen ads
$adxRows = [];
$priorityRows = [];
$sequenceRows = [];
$screenRows = [];
foreach (USER_SOURCES as $source) {
    $sourceBody = $body['by_source'][$source] ?? [];
    if (!is_array($sourceBody)) {
        $sourceBody = [];
    }

    foreach (ADX_TYPES as $type) {
        $list = $sourceBody['adx'][$type] ?? [];
        if (!is_array($list)) {
            $list = [];
        }
        if (count($list) > MAX_ADX_IDS_PER_TYPE) {
            $errors["$source.adx.$type"] = 'Maximum ' . MAX_ADX_IDS_PER_TYPE . ' IDs per ad type';
            continue;
        }
        foreach (array_values($list) as $index => $row) {
            $id = clean_string(is_array($row) ? ($row['id'] ?? '') : '', 191);
            $weightRaw = is_array($row) ? ($row['weight'] ?? '') : '';
            if ($id === '' || !is_valid_ad_unit_id($id)) {
                $errors["$source.adx.$type.$index.id"] = $id === '' ? 'Enter an ad unit ID or remove the row' : 'Ad unit ID cannot contain spaces';
            }
            if (!is_numeric($weightRaw) || (int) $weightRaw != $weightRaw) {
                $errors["$source.adx.$type.$index.weight"] = 'Weight must be a whole number';
            } elseif ((int) $weightRaw < 0) {
                $errors["$source.adx.$type.$index.weight"] = 'Weight cannot be negative';
            } elseif ((int) $weightRaw > 1000000) {
                $errors["$source.adx.$type.$index.weight"] = 'Weight is too large';
            }
            $adxRows[] = [$source, $type, $id, max(0, (int) $weightRaw), $index];
        }
    }

    foreach (AD_TYPES as $type) {
        $item = $sourceBody['priority'][$type] ?? [];
        $main = parse_priority(is_array($item) ? ($item['main'] ?? '') : '', true);
        $failed = parse_priority(is_array($item) ? ($item['failed'] ?? '') : '');
        if ($main === null) {
            $errors["$source.priority.$type.main"] = 'Use numbers 1-4 (up to ' . PRIORITY_SEQUENCE_MAX . '), e.g. 1,1,2,3,1';
        } elseif (!$main) {
            $errors["$source.priority.$type.main"] = 'Enter at least one network, e.g. 1,1,2,3,1';
        }
        if ($failed === null) {
            $errors["$source.priority.$type.failed"] = 'Use numbers 1-4 without repeats, e.g. 3,4';
        }
        $priorityRows[] = [$source, $type, implode(',', $main ?? []), implode(',', $failed ?? [])];
    }

    $sequence = $sourceBody['fullscreen_sequence'] ?? [];
    if (!is_array($sequence)) {
        $sequence = [];
    }
    $sequence = array_values($sequence);
    if (count($sequence) > MAX_SEQUENCE_ITEMS) {
        $errors["$source.fullscreen_sequence"] = 'Maximum ' . MAX_SEQUENCE_ITEMS . ' sequence items';
    }
    foreach ($sequence as $index => $type) {
        if (!in_array($type, FULLSCREEN_TYPES, true)) {
            $errors["$source.fullscreen_sequence"] = 'Unknown fullscreen ad type';
            break;
        }
        $sequenceRows[] = [$source, $type, $index + 1];
    }

    $screens = $sourceBody['screen_ads'] ?? [];
    if (!is_array($screens)) {
        $screens = [];
    }
    $byKey = [];
    foreach (array_slice(array_values($screens), 0, MAX_SCREENS) as $screen) {
        if (is_array($screen) && is_string($screen['screen_key'] ?? null)) {
            $byKey[$screen['screen_key']] = $screen;
        }
    }
    $order = 0;
    foreach (DEFAULT_SCREENS as $key => [$label, $flags]) {
        $order++;
        $screen = $byKey[$key] ?? [];
        $enabled = static fn(string $flag): bool => in_array($flag, $flags, true) && to_bool($screen[$flag] ?? false);
        $screenRows[] = [
            $source,
            $key,
            $label,
            $enabled('native_big'),
            $enabled('native_small'),
            $enabled('banner'),
            $enabled('fullscreen'),
            $order,
        ];
    }
}

if ($errors) {
    validation_error($errors);
}

$pdo = db();
ensure_ads_schema();
$pdo->beginTransaction();

$unitStmt = $pdo->prepare(
    'INSERT INTO ad_units (provider, ad_type, ad_unit_id, enabled) VALUES (:p, :t, :id, :en)
     ON DUPLICATE KEY UPDATE ad_unit_id = VALUES(ad_unit_id), enabled = VALUES(enabled)'
);
foreach ($units as [$provider, $type, $id, $enabled]) {
    $unitStmt->execute([':p' => $provider, ':t' => $type, ':id' => $id, ':en' => (int) $enabled]);
}

$pdo->prepare('UPDATE app_settings SET custom_ads_enabled = :en, custom_ads_url = :url WHERE id = 1')
    ->execute([':en' => (int) $customEnabled, ':url' => $customUrl]);

$pdo->exec('DELETE FROM adx_weighted_ids');
$adxStmt = $pdo->prepare('INSERT INTO adx_weighted_ids (source, ad_type, ad_unit_id, weight, enabled, sort_order) VALUES (:s, :t, :id, :w, 1, :o)');
foreach ($adxRows as [$source, $type, $id, $weight, $order]) {
    $adxStmt->execute([':s' => $source, ':t' => $type, ':id' => $id, ':w' => $weight, ':o' => $order]);
}

$priorityStmt = $pdo->prepare(
    'INSERT INTO ad_priority (source, ad_type, priority, failed_priority) VALUES (:s, :t, :p, :f)
     ON DUPLICATE KEY UPDATE priority = VALUES(priority), failed_priority = VALUES(failed_priority)'
);
foreach ($priorityRows as [$source, $type, $main, $failed]) {
    $priorityStmt->execute([':s' => $source, ':t' => $type, ':p' => $main, ':f' => $failed]);
}

$pdo->exec('DELETE FROM fullscreen_sequence');
$seqStmt = $pdo->prepare('INSERT INTO fullscreen_sequence (source, ad_type, sort_order, enabled) VALUES (:s, :t, :o, 1)');
foreach ($sequenceRows as [$source, $type, $order]) {
    $seqStmt->execute([':s' => $source, ':t' => $type, ':o' => $order]);
}

$screenStmt = $pdo->prepare(
    'INSERT INTO screen_ad_config (source, screen_key, label, native_big_enabled, native_small_enabled, banner_enabled, fullscreen_enabled, sort_order)
     VALUES (:s, :k, :l, :nb, :ns, :b, :f, :o)
     ON DUPLICATE KEY UPDATE label = VALUES(label), native_big_enabled = VALUES(native_big_enabled),
        native_small_enabled = VALUES(native_small_enabled), banner_enabled = VALUES(banner_enabled),
        fullscreen_enabled = VALUES(fullscreen_enabled), sort_order = VALUES(sort_order)'
);
foreach ($screenRows as [$source, $key, $label, $nb, $ns, $b, $f, $order]) {
    $screenStmt->execute([
        ':s' => $source, ':k' => $key, ':l' => $label, ':nb' => (int) $nb, ':ns' => (int) $ns,
        ':b' => (int) $b, ':f' => (int) $f, ':o' => $order,
    ]);
}
$keys = array_keys(DEFAULT_SCREENS);
$placeholders = implode(',', array_fill(0, count($keys), '?'));
$pdo->prepare("DELETE FROM screen_ad_config WHERE screen_key NOT IN ($placeholders)")->execute($keys);

bump_config_version();
$pdo->commit();

json_response(true, 'Ads configuration saved', get_ads_config());
