# UI / UX Analysis

Source: the 40 reference screenshots in `assets/screenshots/` (720 x 1600, Samsung One UI device, light theme unless noted). The screenshots are functional references only. Advertisements visible in them are ignored and never reproduced.

## 1. Screenshot inventory

| # | File | Screen | State |
|---|------|--------|-------|
| 01 | `01_splash.jpeg` | Splash | Loading with fake percentage bar |
| 02 | `02_welcome.jpeg` | Welcome / Agree & Continue | Default |
| 03 | `03_welcome_notification_permission.jpeg` | Welcome | System `POST_NOTIFICATIONS` dialog over screen |
| 04 | `04_onboarding_theme_light.jpeg` | Onboarding theme carousel | Light page selected |
| 05 | `05_onboarding_theme_dark.jpeg` | Onboarding theme carousel | Dark page selected |
| 06 | `06_set_default_app.jpeg` | Set default app | Default |
| 07 | `07_permissions_sheet.jpeg` | Permissions explanation | Bottom sheet over default-app screen |
| 08 | `08_recent_all.jpeg` | Recent | All filter |
| 09 | `09_recent_missed.jpeg` | Recent | Missed filter with date sections |
| 10 | `10_recent_expanded_unknown.jpeg` | Recent | Row expanded, unknown number actions |
| 11 | `11_recent_expanded_contact.jpeg` | Recent | Row expanded, saved contact actions |
| 12 | `12_search_results.jpeg` | Search | Query typed, Recent + Contact sections |
| 13 | `13_call_history_unknown.jpeg` | Call history (per number) | Unknown number, multiple dates |
| 14 | `14_call_history_contact.jpeg` | Call history (per number) | Saved contact |
| 15 | `15_call_history_contact_duplicate.jpeg` | Call history (per number) | Duplicate of 14 |
| 16 | `16_contacts_list.jpeg` | Contacts | Alphabetical list with side index, FAB is "+" |
| 17 | `17_contact_details_favorite.jpeg` | Contact details | Favorited contact with photo |
| 18 | `18_contact_details_menu.jpeg` | Contact details | Overflow menu open |
| 19 | `19_contact_details_scrolled.jpeg` | Contact details | Scrolled, recent activity visible |
| 20 | `20_contact_delete_dialog.jpeg` | Contact details | Delete confirmation dialog |
| 21 | `21_system_pin_shortcut.jpeg` | System | Pin shortcut confirmation |
| 22 | `22_system_share_contact.jpeg` | System | Share sheet with contact text |
| 23 | `23_create_contact_empty.jpeg` | Create contact | Prefilled number, empty name |
| 24 | `24_create_contact_scrolled.jpeg` | Create contact | Scrolled, all field groups |
| 25 | `25_create_contact_name_expanded.jpeg` | Create contact | Name group expanded |
| 26 | `26_create_contact_label_dropdown.jpeg` | Create contact | Email type dropdown open |
| 27 | `27_edit_contact_photo_sheet.jpeg` | Edit contact | Set profile photo sheet |
| 28 | `28_system_photo_crop.jpeg` | System / crop | Photo crop screen |
| 29 | `29_dialer_empty.jpeg` | Dial pad | Empty |
| 30 | `30_dialer_matching.jpeg` | Dial pad | Digits typed with matches |
| 31 | `31_incoming_call.jpeg` | In-call | Incoming (ringing) |
| 32 | `32_active_call_keypad.jpeg` | In-call | Active, keypad open |
| 33 | `33_active_call_more.jpeg` | In-call | Active, More panel open, speaker on |
| 34 | `34_settings.jpeg` | Settings | Default |
| 35 | `35_settings_language.jpeg` | Choose language | English selected |
| 36 | `36_settings_theme.jpeg` | Theme | Light selected |
| 37 | `37_quick_response_list.jpeg` | Quick response | List |
| 38 | `38_quick_response_add_dialog.jpeg` | Quick response | Add dialog with keyboard |
| 39 | `39_blocked_numbers_unblock_dialog.jpeg` | Blocked numbers | Unblock dialog |
| 40 | `40_system_share_app.jpeg` | System | Share sheet with app text |

Unique screens: 24 (several screenshots are states of the same screen). System screens (03, 21, 22, 28, 40) are triggered by the app but rendered by Android or a library.

## 2. Screen-by-screen analysis

### Splash (01)
- Purpose: brand moment while the app starts.
- Components: rounded-square blue app icon, app name "Contacts", tagline "Manage Contacts Effortlessly", percentage progress bar.
- Entry: app launch. Destination: Welcome (first run) or Recent.
- Improvement: the percentage bar is fake. Replace with the Android 12 SplashScreen API and a short icon scale/fade.

### Welcome (02, 03)
- Purpose: introduce value and get consent.
- Components: title with brand-colored word ("Welcome to **Contacts!**"), subtitle, four feature rows (outlined icon + title + body): Call Access, Call Blocking, After Call Screen, Smart Notifications. Primary full-width "Agree & Continue" button, Privacy Policy link text.
- After tapping continue the system notification permission dialog appears (03).
- Improvement: icons sit unaligned; use tinted icon containers, consistent spacing, and explain notifications before the system dialog.

### Onboarding theme (04, 05)
- Purpose: pick light or dark theme.
- Components: back arrow, "Theme" title, description, label pill ("Light"/"Dark") that looks like a button, horizontal pager of phone previews with page dots, "Done" button.
- Improvement: no System option; the label pill is ambiguous. Use three selectable preview cards (System / Light / Dark) with a radio and apply the theme live.

### Set default app (06)
- Purpose: request the default dialer role.
- Components: illustration (phone with contact rows), title "Set Default Contacts App", body text, primary "Set as default", text "Cancel".
- Improvement: Android has no default "Contacts" role; the real role is default Phone app. Rename and explain clearly; "Not now" instead of "Cancel".

### Permissions sheet (07)
- Purpose: explain permissions before system dialogs.
- Components: modal bottom card "Permissions" with three rows (Call History, Contact Access, Floating Access) and "Continue".
- Improvement: Floating Access (draw over apps) is not needed when the app is the default dialer. Show only permissions still missing.

### Recent (08 - 11)
- Header: large "Recent" title, search and settings icons.
- Segmented control: All / Missed (pill with white selected thumb).
- Date sections: Today, Yesterday, `28 Sept 2026`.
- Row: circular avatar (person glyph for unknown, initial letter for named contacts), name or number with group count "112 (2)", call-type arrow + type label + relative or clock time. Missed rows are red (text and arrow). Trailing phone icon.
- Expanded row (10, 11): row background becomes a rounded card, three actions appear: unknown number gets Add Contact / Messages / Call History; saved contact gets Video Call / Messages / Call History.
- Bottom navigation: floating pill with Recent / Contacts / Favorites, selected tab gets a tinted pill, separate circular blue FAB with dial pad glyph.
- Improvement: consecutive repeated calls from the same number are listed separately (rejected/missed alternation); group consecutive calls per number per day and show the latest type.

### Search (12)
- Rounded search field in the toolbar with clear button, back arrow.
- Results grouped as "Recent" and "Contact" sections, rows with avatar, name, number, call icon.

### Call history per number (13 - 15)
- Toolbar: back, avatar, name and number, blue call icon.
- Date sections with grouped rounded cards; each entry shows arrow, type, relative time and duration ("4 hours ago - 0s").
- Bottom action bar: unknown number gets Add Contact / Messages / Delete; contact gets Video Call / Messages / Delete.

### Contacts (16)
- Large "Contacts" title, search and settings icons.
- Unnamed numbers and emoji names first, then sections A, F, J, N with single letter headers.
- Avatar with initial letter in brand color on tinted circle.
- Right side vertical A-Z index with "#" highlighted.
- FAB changes to "+" (create contact) on this tab.

### Contact details (17 - 20)
- Toolbar: back, "Details", edit icon, overflow (Create shortcut, Share contact).
- Large circular photo, name.
- Three equal action tiles: Call, Message, Video.
- "Contact info" card: phone number with label (Mobile), message and video icons.
- "Recent activity" card with "Show more".
- Bottom bar: Favorites/Unfavorite (filled blue star when favorite), Block, Delete (red).
- Delete dialog (20): title, body, Cancel (tonal) and Delete (primary) buttons.
- Improvement: name scrolls under toolbar (19); use a collapsing header with the name in the toolbar.

### Create / edit contact (23 - 28)
- Toolbar: close, "Create Contact"/"Edit Contact", pill "Done" button disabled until valid.
- Photo circle with "+" badge; tapping opens "Set profile photo" sheet (Take photo / Choose photo / Cancel) and crop screen (28).
- "Save to Phone" account selector.
- Grouped cards: Name (expandable: middle name, surname, prefix, suffix), Company, Phones (value + type dropdown + remove), Add number, Emails (value + type dropdown + remove), Add email, Add address, Add date of birth, Nickname, Notes.
- Type dropdown (26): Home / Work / Mobile / Other with check mark.

### Dial pad (29, 30)
- Back arrow, overflow menu when digits exist.
- Matching suggestions list ("Recent" header) above the number.
- Large number display, 4 x 3 round keys (light gray) with digit and letters, blue round call button, backspace icon when digits exist.
- Improvement: empty top half; show suggestions/recent numbers.

### In-call (31 - 33)
- Incoming: large avatar, name, number, "Incoming call" in blue, "Message" chip, green Answer and red Decline buttons.
- Active: timer at top, avatar, name and number, control row (Mute, Keypad, Speaker, More); active toggles become black filled circles. Keypad sheet overlays the info (bug: every key labelled "ABC"). More panel shows Hold and Add Call. Red End button.

### Settings (34 - 40)
- Grouped cards with section titles: Appearance (Language - English, Theme Mode - Light Mode), General (Blocked Numbers, Quick Response, Widget), Others (Share App, Rate Us, Privacy Policy).
- Language (35): list of languages with initial-letter avatar, native + English name, radio, "Done" pill.
- Theme (36): three phone-preview cards (System Default, Light Mode, Dark Mode) with radio.
- Quick response (37, 38): plain list of messages, "+" adds via dialog (Message field, Cancel / Add).
- Blocked numbers (39): "Block unknown" switch with description, tonal "Add a number" button, rows with red block avatar and remove "x", Unblock confirmation dialog.

## 3. UI component inventory

- Large-title top bar with icon actions
- Segmented control (All / Missed)
- Section header (date / letter / settings group)
- Avatar (photo, initial, unknown glyph, blocked variant)
- Call row (with expandable actions)
- Contact row
- Alphabet side index
- Floating pill bottom navigation and circular FAB
- Grouped card container
- Setting row (icon, title, value or switch)
- Action tile (icon + label, equal width)
- Bottom action bar
- Primary, tonal and text buttons, pill "Done" button
- Dialog (title, message, two buttons)
- Bottom sheet (options list, permissions list, quick responses)
- Dial pad key and call button
- Form field group (icon, inputs, type dropdown, remove button, add row)
- Theme preview card with radio
- Language row with radio
- Empty state (icon, title, message)

## 4. Design observations

- Clean white surfaces with light gray (`#F4F5F7`-like) grouped cards; very few shadows.
- Brand blue (`#3D7EF7`-like) for primary actions, selected states and links.
- Large radius everywhere (cards 16-20, buttons fully rounded / 16).
- Line icons (outlined, 1.5-2 dp stroke), filled only for selected states.
- Typography: bold large titles (~24-28 sp), medium row titles (~17 sp), regular secondary (~14-15 sp).

## 5. Light theme

Background `#FFFFFF`, cards `#F4F5F7`, primary text `#16181D`, secondary `#6B7080`, divider `#E6E8EC`, primary `#3D7EF7`.

## 6. Dark theme

Seen in 05: background true black, cards dark gray, same brand blue, white text. Our values: background `#000000`, surface `#121316`, cards `#1C1D21`, primary text `#F2F3F5`, secondary `#9A9EAA`, divider `#2A2C31`, primary `#4C8BFF`.

## 7. Typography

Roboto (bundled: Regular, Medium, Bold). Scale: Display 32/bold, Headline 26/bold, Title 20/medium, Title small 17/medium, Body 15/regular, Body small 14/regular, Label 14/medium, Caption 12/regular.

## 8. Colors

Primary, primary container (tinted avatar background), background, surface, card, text primary, text secondary, text tertiary, divider, success / incoming (`#1FA855`), warning (`#F2A516`), error / missed (`#E5383B`), outgoing (`#3D7EF7`), rejected/blocked (gray).

## 9. Spacing

4, 8, 12, 16, 20, 24, 32, 40 dp. Screen horizontal padding 16 dp (20 dp for onboarding).

## 10. Border radius

8 (chips), 12 (small cards, keys menu), 16 (cards, buttons), 20 (sheets, large cards), 24 (dialogs), full (avatars, pills, FAB).

## 11. Shadows

Only the floating bottom navigation, FAB and popup menus have soft shadows. Everything else is flat.

## 12. Icons

Material Symbols (outlined, rounded) as vector drawables. Filled variants for selected nav tab and favorite star.

## 13. Animations

Reference has almost no motion. Planned: splash icon scale/fade, shared-axis screen transitions, fade+scale dialogs, sliding bottom sheets, animated nav pill, dial key press scale with haptic, expand/collapse of recent rows, slide-up after-call card.

## 14. Empty states

Not shown in reference. Designed: no contacts, no recent calls, no missed calls ("You're all caught up."), no favorites, no blocked numbers, no search results, no quick responses.

## 15. Loading states

Splash only. Designed: lightweight progress indicator for first contact / call log load.

## 16. Dialogs

Delete contact, Unblock number, Add quick response, Add blocked number. Style: rounded 24 card, centered title, message, Cancel (tonal) + action (primary) buttons side by side.

## 17. Bottom sheets

Permissions explanation, Set profile photo, Save-to account picker, SIM chooser, Quick response picker on incoming call.

## 18. Permission UX

Welcome explains features, then system notification dialog. Default app screen precedes the permissions sheet. Our flow requests the dialer role first (which can grant phone, contacts and call log groups automatically) and then shows only the missing permissions, one system dialog at a time, with Settings redirect after permanent denial.

## 19. Ads

The reference shows banner, native and full-width ads on many screens (06, 07, 08 - 11, 13, 17 - 20, 35, 37). All are ignored. No ad SDK, no ad placeholders.

## 20. Reusable components

`ContactAvatarView`, `AlphabetIndexView`, `EmptyStateView`, `SettingRowView`, `AppDialogs`, `AppBottomSheet`, `SectionHeader` layout, call row layout shared by Recent, Search and Dialer suggestions, contact row layout shared by Contacts, Favorites and Search, `DialPadView` shared by Dialer and in-call keypad.

## 21. Areas to improve over the reference

1. Real splash instead of fake progress.
2. Correct "default Phone app" wording.
3. Theme selection with System option and live preview.
4. No overlay permission.
5. Group consecutive calls.
6. Dial pad suggestions instead of blank space.
7. Correct in-call keypad letters.
8. Collapsing contact header.
9. Empty, loading and error states everywhere.
10. Accessible call types (icon + text + color).
11. No ads.
