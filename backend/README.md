# Contacts Admin Panel and Remote Config API

Core PHP 8 + MySQL admin panel that controls the Android app's screens, ads, force update,
launcher mode and Facebook marketing. The Android app reads one public endpoint
(`api/app/config.php`) and caches the result, so the app keeps working when the server is down.

```text
backend/
├── admin/          Admin pages (login, dashboard, settings, ads, users, Facebook)
├── api/            JSON endpoints (admin endpoints need a session + CSRF token)
├── assets/         admin.css / admin.js (no build step)
├── config/         config.php, database.php, auth.php, repository.php (not web-accessible)
├── sql/            contacts_admin.sql (schema + seed data)
└── setup_admin.php One-time admin account creation
```

## 1. Database import

1. Create the database and import the schema and seed rows:

   ```bash
   mysql -u root -p -e "CREATE DATABASE contacts_admin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
   mysql -u root -p contacts_admin < sql/contacts_admin.sql
   ```

2. The seed creates the single `app_settings` row, empty ad units and the default fullscreen
   sequence. Every screen listed in `DEFAULT_SCREENS` (`config/config.php`) is added to
   **Per-screen ads** automatically, for both Organic and Marketing, with every ad switch off.

## 2. PHP configuration

- PHP 8.0+ with `pdo_mysql`, `json`, `curl`, `openssl` and `fileinfo` (the last three for push
  notifications). `uploads/` must be writable by PHP (notification images are stored in
  `uploads/notifications/`). Apache with `mod_rewrite`/`.htaccess` support, or the
  equivalent deny rules on Nginx for `config/`, `sql/`, `admin/includes/` and `*.md`.
- Database credentials come from environment variables, with local defaults in
  `config/database.php`:

  | Variable  | Default          |
  |-----------|------------------|
  | `DB_HOST` | `127.0.0.1`      |
  | `DB_PORT` | `3306`           |
  | `DB_NAME` | `contacts_admin` |
  | `DB_USER` | `root`           |
  | `DB_PASS` | *(empty)*        |
  | `APP_ENV` | `production`     |

- In `production` PHP errors are hidden and APIs return a generic message; set `APP_ENV=development`
  locally to see them.
- Serve the panel over HTTPS. Session cookies are `HttpOnly` and `SameSite=Lax`, and become `Secure`
  automatically on HTTPS.

## 3. Admin login

1. After importing the database run, once:

   ```bash
   php setup_admin.php
   ```

   It creates `admin@gmail.com` with the initial password defined in `setup_admin.php`, stored with
   `password_hash()`. The script refuses to run if an admin already exists.
2. **Delete `setup_admin.php` from the server** after it has run.
3. Login uses `password_verify()`. Five failed attempts lock login for five minutes per session.

## 4. API base URL

All endpoints live under the folder you deploy `backend/` to, for example
`https://your-domain.com/backend/`. Responses always use:

```json
{ "success": true, "message": "OK", "data": {} }
```

Validation errors return HTTP 422 with `data.errors` keyed by field.

| Endpoint | Method | Auth | Purpose |
|---|---|---|---|
| `api/app/config.php` | GET | public | Full remote config for Android |
| `api/users/register_or_update.php` | POST | public | Android user registration (`device_id`, `source`, `app_version`) |
| `api/auth/login.php` | POST | CSRF | Admin login |
| `api/dashboard/stats.php` | GET | admin | User counts |
| `api/app/get_settings.php`, `save_settings.php` | GET / POST | admin | App settings |
| `api/ads/get_config.php`, `save_config.php` | GET / POST | admin | Ads configuration |
| `api/users/user_stats.php` | GET | admin | Paginated user list |
| `api/marketing/facebook_config.php` | GET / POST | admin | Facebook App ID / token / secret |

Admin POST endpoints require the `X-CSRF-Token` header (the panel's JS sends it automatically).
Every save increments `config_version`; Android ignores responses whose version it already has.
The Facebook App Secret is write-only: it is never returned by any endpoint, including to Android.

## 5. Opening the admin panel

Open `https://your-domain.com/backend/admin/` (the backend root redirects there) and sign in.
Menu: Dashboard, App Settings, Ads Configuration, Users, Facebook Marketing.

## 6. Configuring ads

On **Ads Configuration**:

1. **AdMob** and **TradPlus**: enter a unit ID per format and turn it on. A format that is off or
   empty is skipped.
2. **ADX (Google Ad Manager)**: add one or more IDs per format, each with a weight. Each request
   picks one ID with probability `weight / total weight`; weight `0` or a disabled row is never
   used.
3. **Custom ads**: turn on and enter a web link. When Custom is picked for a fullscreen ad
   (Interstitial, Reward, App Open or the `custom` sequence item), the app opens the link in a
   Chrome Custom Tab; after the tab is closed it shows an App Open ad (from AdMob/ADX/TradPlus),
   then continues. Custom is skipped for Banner and Native.
4. **Priority**: for each ad type, a sequence of networks as numbers: `1` AdMob, `2` ADX,
   `3` TradPlus, `4` Custom. Each ad request uses the next entry and the sequence loops; repeats are
   allowed (up to 25 entries), e.g. `1,1,2,3,1` gives AdMob, AdMob, ADX, TradPlus, AdMob. If that
   network fails, the "failed" list is tried (networks already tried are skipped). Networks without
   an enabled ID are skipped too. Default: `1,2,3,4` (brackets like `[1,2,3,4]` are accepted too).
5. **Fullscreen sequence**: the order of fullscreen ads (`interstitial`, `reward`, `app_open`,
   `custom`). Each fullscreen trigger shows the next item; the position is remembered on the device
   and wraps around.
6. **Per-screen ads**: for each screen choose Native Big, Native Small, Banner and Fullscreen.

**Organic / Marketing**: Priority, ADX weighted IDs, Fullscreen sequence and Per-screen ads are
stored separately per user source. Use the Organic / Marketing switch to edit each set; "Copy to …"
copies the visible set to the other one (click Save afterwards). Adding or removing a screen applies
to both sources; its ad switches stay separate. The app uses the set matching its install source
(marketing when the Play Install Referrer shows a campaign, organic otherwise). AdMob/TradPlus IDs
and Custom ads are shared. When upgrading an existing database, the current values are
kept for organic users and copied to marketing users automatically on the first request.

On the device, providers are tried in the Priority order until one fills. A config without a
priority (an older backend) uses **AdMob → TradPlus → ADX → Custom**. While a banner or native loads, a matching shimmer is shown; on no-fill the slot is hidden.
Fullscreen ads show an "Ads Loading..." card and give up after about 9 seconds, so the user is never
blocked.

App Settings also controls: Splash Screen After Full Screen Ad, App Open Ad On Resume, In-App
Review (Play review prompt, asked once on the second app open after onboarding), Launcher Mode
(the Default Phone screen also requires the default home app role, so the Home button opens the
Launcher screen), force update (minimum version + message), and the after-call screen switches.

**Intro flow** (App Settings): the onboarding screens after the splash, separately for organic and
marketing users, as numbers in order: `1` Welcome, `2` Theme, `3` Default (phone app), `4` Language.
Example: organic `3,2,1`, marketing `4,1,2,3`. A screen not in the list is not shown; `3` is
required and numbers cannot repeat. Until a flow is saved, it follows the old Welcome / Theme /
Language switches (Language, Welcome, Theme, Default).

**Send Notification**: upload the Firebase service account JSON once (Firebase Console → Project
settings → Service accounts → Generate new private key, same project as the app's
`google-services.json`; the private key is never shown again). Then send a title, description,
optional image (JPG/PNG/WebP up to 2 MB) and optional link to all, organic or marketing users. The
app registers its Firebase Cloud Messaging token through `api/users/push_token.php`; the page shows
how many users can be reached and a history with users, delivered and failed counts. Tokens Firebase
reports as unregistered are removed. Separately, the app shows a built-in message every 3-4 hours
on its own; that one is not controlled from the panel.

## 7. Android API integration

- Set the API base URL at build time (it must end with `/`):

  ```properties
  # ~/.gradle/gradle.properties or the project's gradle.properties
  contactsApiBaseUrl=https://your-domain.com/backend/
  admobAppId=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
  ```

  Without `contactsApiBaseUrl` the app skips network calls and runs on cached/default config.
  Without `admobAppId` the Google sample App ID is used, which only serves test ads.
- `RemoteConfigManager` loads the cached config instantly, refreshes in the background (at most every
  30 minutes) and merges valid sections only, so a broken section never replaces a good cache.
- `UserSourceManager` classifies the install once via the Play Install Referrer (`organic` or
  `marketing`) and registers the device; an organic user can later become marketing, never the
  reverse.
- Ads are shown only through `AdsManager` (`showBanner`, `showNativeBig`, `showNativeSmall`,
  `showFullscreen`). Today they are integrated only into `WelcomeActivity`.
- Debug builds log the flow under the logcat tag `AdManagerContact` (`Ad Request`, `Ad Loaded`,
  `Ad Failed`, `Ad Impression`, plus config and guard messages). Release builds log nothing.

### Android dependency changes

Added in `gradle/libs.versions.toml` / `app/build.gradle`:

| Library | Why |
|---|---|
| `com.google.android.gms:play-services-ads` 23.6.0 | AdMob and Ad Manager (ADX) |
| `com.tradplusad:tradplus` 16.8.40.1, `tradplus-googlex`, `tradplus-facebook`, `com.facebook.android:audience-network-sdk` 6.22.0 | TradPlus mediation and its Google / Meta adapters |
| `com.android.installreferrer:installreferrer` 2.2 | Organic vs marketing detection |
| `com.facebook.android:facebook-core` 18.0.3 | Facebook marketing (initialized from the admin's App ID / Client Token; auto-init is off in the manifest) |
| `androidx.lifecycle:lifecycle-process` | App-open-on-resume detection |
| `com.google.firebase:firebase-messaging` | Push notifications from Send Notification |
| `androidx.work:work-runtime` 2.10.3 | The built-in notification every 3-4 hours |
| `org.json:json` (tests only) | JSON parsing in JVM unit tests |

The manifest also gained the `AD_ID` and `REQUEST_DELETE_PACKAGES` permissions (the latter for
uninstalling apps from Launcher Mode), the AdMob App ID and the TradPlus App ID meta-data.

## 8. Adding a new screen

1. Add the screen key with its label and supported ad formats to `DEFAULT_SCREENS` in
   `config/config.php` (e.g. `'settings_home' => ['Settings Home', ['native_big', 'banner']]`). The
   admin panel shows it under **Ads Configuration → Per-screen ads** for both Organic and Marketing.
   Each fullscreen trigger gets its own row (e.g. `settings_home_back`); a `*_back` row also covers
   the system back of that screen.
2. In the Android screen, add `FrameLayout` containers (`visibility="gone"`) and call:

   ```java
   AdsManager.showNativeBig(this, binding.nativeBigContainer, "settings_home");
   AdsManager.showBanner(this, binding.bannerContainer, "settings_home");
   AdsManager.showFullscreen(this, "settings_home", this::goNext);
   ```

   Unknown or disabled screen keys simply show nothing and `showFullscreen` continues at once.
3. If the screen itself should be switchable (like Welcome), add a boolean column to
   `app_settings`, expose it under `screens` in `build_public_config()`, add it to App Settings, and
   check `RemoteConfigManager.get().isScreenEnabled("key")` on Android.

## 9. Adding a new ad provider

1. Backend: add the key to `AD_PROVIDERS` (and a `*_TYPES` list) in `config/config.php`, store its
   IDs in `ad_units`, expose them in `build_public_config()` under `ads`, and add the inputs to
   `admin/ads-config.php` / `assets/js/admin.js`. Validate IDs in `api/ads/save_config.php`. Give it
   the next number in `PRIORITY_PROVIDERS`, raise `PRIORITY_MAX` in `admin.js`, and add it to the
   Priority legend.
2. Android: add a value to `AdProviderType` (its `key` must match `PRIORITY_PROVIDERS`), parse its IDs
   in `AdsConfig.fromJson`, implement `ads/providers/AdProvider` (banner, native, fullscreen; call the
   callback exactly once and return ads that can be destroyed), then add it to `AdsManager.PROVIDERS`
   at the desired default position.

## 10. Adding a new fullscreen sequence type

1. Backend: add the key to `FULLSCREEN_TYPES` in `config/config.php` (and to the `fullscreen_sequence`
   ENUM in the SQL if you keep the ENUM) and to the sequence chip choices in `assets/js/admin.js`.
2. Android: add a value to `FullscreenType` with its key and the `AdType` to request (or `null` for a
   custom-only type, then route it in `FullscreenAdManager.show`). Unknown keys from the server are
   ignored, so older app versions keep working.
