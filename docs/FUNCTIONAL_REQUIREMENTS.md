# Functional Requirements

Platform: native Android, Java, minSdk 25, targetSdk 36. All data stays on device.

## Contacts
- Read device contacts through `ContactsContract` off the main thread; live refresh through a `ContentObserver`.
- Alphabetical sections ("#" for names starting with digits, symbols or emoji), A-Z side index.
- Avatar: contact photo, otherwise initial letter, otherwise person glyph.
- Show display name and primary number.

## Recent calls
- Read `CallLog.Calls` (incoming, outgoing, missed, rejected, blocked, voicemail mapped to incoming).
- Group consecutive calls from the same number on the same day, show count.
- Date sections: Today, Yesterday, `d MMM yyyy`.
- Time: relative within the last hours, clock time otherwise.
- Row expand actions as documented in `SCREEN_FLOW.md`.

## Missed calls
- All / Missed filter. Missed rows use red text, a missed arrow icon and the text "Missed call".

## Dial pad
- 0-9, *, # with letters; long-press 0 for "+"; tap haptics and press scale.
- Backspace tap deletes one digit, long press clears.
- Matching suggestions from contacts (number and T9 name match) and recent calls.
- Call button places call; pasted / incoming `tel:` numbers prefilled.

## Calling
- Outgoing through `TelecomManager.placeCall` with SIM chooser when multiple accounts exist.
- When default dialer: `InCallService` drives an in-call screen with incoming, dialing, active, holding and ended states; mute, speaker / Bluetooth / earpiece route, DTMF keypad, hold, add call, end.
- When not default dialer: calls go through the system in-call UI.
- Call notifications: `CallStyle` incoming (full-screen intent) and ongoing.

## Call history
- Per-number history grouped by date with type, time and duration.
- Delete all entries for a number (`WRITE_CALL_LOG`).

## Favorites
- Uses `ContactsContract.Contacts.STARRED`. Grid of favorites, tap to call, long press for remove / details.

## Search
- Debounced (250 ms) query over contacts (name, number) and recent calls. Results in Contacts and Recent sections.

## Contact details
- Header with photo and name, Call / Message / Video, all phone numbers and emails, organization, address, birthday, nickname, notes, recent activity (latest calls), Favorite, Block, Delete, Edit, Create shortcut, Share contact.

## Add / edit / delete contact
- Fields: prefix, first, middle, last, suffix, company, multiple phones with type, multiple emails with type, address, birthday, nickname, notes, photo.
- Save-to account picker (device / Google accounts that support contacts).
- Validation: at least a name or a number; phone and email format check.
- Save through `ContentProviderOperation` batch; delete through the contact URI with confirmation.

## Call blocking
- Block / unblock numbers through `BlockedNumberContract` (only available when default dialer; otherwise the user is asked to set default first).
- Blocked numbers list with add (dialog) and unblock (confirmation).
- "Block unknown" uses `CallScreeningService` to reject callers not in contacts; on API 29+ the call-screening role is requested.
- A number is shown as blocked only after the system write succeeds.

## After call screen
- Shown after a call ends when the app is the default dialer and the setting is on.
- Avatar, name, number, call type, duration; actions: Call again, Message, Save contact (unknown), Favorite (saved), Block, Call history.

## Smart notifications
- Default dialer receives `ACTION_SHOW_MISSED_CALLS_NOTIFICATION`; post one notification per missed number with Call back and Message actions, grouped.
- Toggle in Settings. Cleared when Recent is opened.

## Quick response
- Editable list of reply messages (default four). Used on the incoming screen: reject call with selected message (`Call.reject(true, text)`).

## Permissions
- Contacts (`READ_CONTACTS`, `WRITE_CONTACTS`), Call log (`READ_CALL_LOG`, `WRITE_CALL_LOG`), Phone (`CALL_PHONE`, `READ_PHONE_STATE`), Notifications (`POST_NOTIFICATIONS`, API 33+), `USE_FULL_SCREEN_INTENT`.
- Explanation before request, one at a time; handle granted, denied, permanently denied (Settings redirect), unsupported and revoked (re-checked on resume).
- No overlay, SMS sending or other unused permissions.

## Default dialer
- `RoleManager` `ROLE_DIALER` on API 29+, `ACTION_CHANGE_DEFAULT_DIALER` on 25-28. Status visible in Settings.

## Theme
- Light, Dark, System; persisted; applied with `AppCompatDelegate.setDefaultNightMode`.

## Language
- English, Hindi, French, Spanish, German. Per-app locale with `AppCompatDelegate.setApplicationLocales`.

## Widget
- Favorites widget (list of favorite contacts, tap to call). Settings row requests pinning when supported.

## Settings
- Appearance, Calling, General, Privacy, Others groups as in `SCREEN_FLOW.md`. Share app and Rate us via intents.

## Privacy
- No network permission, no analytics, no ads. Privacy Policy screen in-app.

## Error handling
- Never crash on missing permissions; show banners / empty states with actions.
- Toast or snackbar for failures (invalid number, save failed, call failed, block not available).
- Catch `SecurityException` on all provider and telecom calls.

## Empty states
- No contacts, no recent calls, no missed calls, no favorites, no blocked numbers, no search results, no quick responses.
