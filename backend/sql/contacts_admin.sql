-- Contacts Admin Panel schema
-- Import into an empty database, e.g.:
--   mysql -u root -p -e "CREATE DATABASE contacts_admin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
--   mysql -u root -p contacts_admin < sql/contacts_admin.sql
-- The admin account is NOT created here (no plaintext passwords). Run setup_admin.php afterwards.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS admins (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    email VARCHAR(191) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_admins_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    device_id VARCHAR(100) NOT NULL,
    source ENUM('organic', 'marketing') NOT NULL DEFAULT 'organic',
    app_version VARCHAR(20) NOT NULL DEFAULT '',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_seen_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fcm_token VARCHAR(255) NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_device_id (device_id),
    KEY idx_users_source (source),
    KEY idx_users_created_at (created_at),
    KEY idx_users_fcm_token (fcm_token(32))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Firebase service account used to send push notifications (single row, id = 1).
CREATE TABLE IF NOT EXISTS push_credentials (
    id TINYINT UNSIGNED NOT NULL,
    service_account MEDIUMTEXT NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS push_notifications (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    body VARCHAR(500) NOT NULL,
    image_url VARCHAR(500) NOT NULL DEFAULT '',
    link VARCHAR(500) NOT NULL DEFAULT '',
    target ENUM('all', 'organic', 'marketing') NOT NULL DEFAULT 'all',
    total_count INT UNSIGNED NOT NULL DEFAULT 0,
    success_count INT UNSIGNED NOT NULL DEFAULT 0,
    failure_count INT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_push_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Single-row table (id = 1) holding every global setting.
CREATE TABLE IF NOT EXISTS app_settings (
    id TINYINT UNSIGNED NOT NULL,
    config_version INT UNSIGNED NOT NULL DEFAULT 1,
    privacy_policy_url VARCHAR(500) NOT NULL DEFAULT '',
    app_version VARCHAR(20) NOT NULL DEFAULT '1.0',
    app_link VARCHAR(500) NOT NULL DEFAULT '',
    force_update TINYINT(1) NOT NULL DEFAULT 0,
    minimum_version VARCHAR(20) NOT NULL DEFAULT '1.0',
    force_update_message VARCHAR(500) NOT NULL DEFAULT 'A new version is available. Please update the app to continue.',
    splash_after_fullscreen_ad TINYINT(1) NOT NULL DEFAULT 0,
    custom_ads_enabled TINYINT(1) NOT NULL DEFAULT 0,
    custom_ads_url VARCHAR(500) NOT NULL DEFAULT '',
    app_open_on_resume TINYINT(1) NOT NULL DEFAULT 0,
    launcher_enabled TINYINT(1) NOT NULL DEFAULT 0,
    in_app_review_enabled TINYINT(1) NOT NULL DEFAULT 1,
    welcome_enabled TINYINT(1) NOT NULL DEFAULT 1,
    theme_selection_enabled TINYINT(1) NOT NULL DEFAULT 1,
    language_selection_enabled TINYINT(1) NOT NULL DEFAULT 0,
    intro_flow_organic VARCHAR(20) NOT NULL DEFAULT '',
    intro_flow_marketing VARCHAR(20) NOT NULL DEFAULT '',
    after_call_organic_enabled TINYINT(1) NOT NULL DEFAULT 1,
    after_call_marketing_enabled TINYINT(1) NOT NULL DEFAULT 1,
    facebook_app_id VARCHAR(100) NOT NULL DEFAULT '',
    facebook_client_token VARCHAR(100) NOT NULL DEFAULT '',
    facebook_secret_key VARCHAR(255) NOT NULL DEFAULT '',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ad_units (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    provider ENUM('admob', 'tradplus', 'custom', 'adx') NOT NULL,
    ad_type ENUM('banner', 'interstitial', 'native', 'reward', 'app_open') NOT NULL,
    ad_unit_id VARCHAR(191) NOT NULL DEFAULT '',
    enabled TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ad_units_provider_type (provider, ad_type),
    KEY idx_ad_units_provider (provider),
    KEY idx_ad_units_ad_type (ad_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- adx_weighted_ids, ad_priority, fullscreen_sequence and screen_ad_config hold separate rows per user source.
CREATE TABLE IF NOT EXISTS adx_weighted_ids (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    source ENUM('organic', 'marketing') NOT NULL DEFAULT 'organic',
    ad_type ENUM('banner', 'interstitial', 'native', 'app_open', 'reward') NOT NULL,
    ad_unit_id VARCHAR(191) NOT NULL,
    weight INT UNSIGNED NOT NULL DEFAULT 0,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    sort_order INT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_adx_ad_type (ad_type),
    KEY idx_adx_source (source)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Network order per ad type as provider numbers: 1 AdMob, 2 ADX, 3 TradPlus, 4 Custom.
-- The app tries "priority" first, then any network from "failed_priority" not already tried.
CREATE TABLE IF NOT EXISTS ad_priority (
    source ENUM('organic', 'marketing') NOT NULL DEFAULT 'organic',
    ad_type ENUM('banner', 'interstitial', 'native', 'reward', 'app_open') NOT NULL,
    priority VARCHAR(50) NOT NULL DEFAULT '',
    failed_priority VARCHAR(50) NOT NULL DEFAULT '',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (source, ad_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS fullscreen_sequence (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    source ENUM('organic', 'marketing') NOT NULL DEFAULT 'organic',
    ad_type ENUM('interstitial', 'reward', 'app_open', 'custom') NOT NULL,
    sort_order INT UNSIGNED NOT NULL DEFAULT 0,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    KEY idx_fullscreen_sort_order (sort_order),
    KEY idx_fullscreen_source (source)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS screen_ad_config (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    source ENUM('organic', 'marketing') NOT NULL DEFAULT 'organic',
    screen_key VARCHAR(50) NOT NULL,
    label VARCHAR(100) NOT NULL DEFAULT '',
    native_big_enabled TINYINT(1) NOT NULL DEFAULT 0,
    native_small_enabled TINYINT(1) NOT NULL DEFAULT 0,
    banner_enabled TINYINT(1) NOT NULL DEFAULT 0,
    fullscreen_enabled TINYINT(1) NOT NULL DEFAULT 0,
    sort_order INT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_screen_ad_config_source_key (source, screen_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Default settings
INSERT IGNORE INTO app_settings (id) VALUES (1);

-- Ad units start empty and disabled so no ads are requested until real IDs exist.
INSERT IGNORE INTO ad_units (provider, ad_type, ad_unit_id, enabled) VALUES
    ('admob', 'banner', '', 0),
    ('admob', 'interstitial', '', 0),
    ('admob', 'native', '', 0),
    ('admob', 'reward', '', 0),
    ('admob', 'app_open', '', 0),
    ('tradplus', 'banner', '', 0),
    ('tradplus', 'interstitial', '', 0),
    ('tradplus', 'native', '', 0),
    ('tradplus', 'reward', '', 0);

-- Default screens for both sources: every ad flag OFF.
INSERT IGNORE INTO screen_ad_config (source, screen_key, label, sort_order) VALUES
    ('organic', 'welcome', 'Welcome', 1),
    ('organic', 'theme_selection', 'Theme Selection', 2),
    ('organic', 'default_phone', 'Default Phone', 3),
    ('organic', 'launcher', 'Launcher', 4),
    ('organic', 'language_selection', 'Language', 5),
    ('organic', 'after_call', 'After Call', 46),
    ('marketing', 'welcome', 'Welcome', 1),
    ('marketing', 'theme_selection', 'Theme Selection', 2),
    ('marketing', 'default_phone', 'Default Phone', 3),
    ('marketing', 'launcher', 'Launcher', 4),
    ('marketing', 'language_selection', 'Language', 5),
    ('marketing', 'after_call', 'After Call', 46);

-- Default fullscreen sequence for both sources (only seeded when empty).
INSERT INTO fullscreen_sequence (source, ad_type, sort_order, enabled)
SELECT src.source, seed.ad_type, seed.sort_order, 1
FROM (
    SELECT 'interstitial' AS ad_type, 1 AS sort_order
    UNION ALL SELECT 'reward', 2
    UNION ALL SELECT 'app_open', 3
    UNION ALL SELECT 'custom', 4
) AS seed
CROSS JOIN (SELECT 'organic' AS source UNION ALL SELECT 'marketing') AS src
WHERE NOT EXISTS (SELECT 1 FROM fullscreen_sequence);
