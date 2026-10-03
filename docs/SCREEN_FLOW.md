# Screen Flow

## First launch

```
App launch
↓
Splash (SplashScreen API)
↓
Choose Language                       ref 35
↓
Welcome / Agree & Continue            ref 02
↓
Notification permission (API 33+)     ref 03
↓
Theme (System / Light / Dark)         ref 04, 05
↓
Set default Phone app                 ref 06
↓
Permissions sheet (missing only)      ref 07
↓
Recent                                ref 08
```

Skipping the default-app step ("Not now") still continues to the permissions sheet. Denied permissions never block the user: the main screens show an inline banner with a fix action.

## Later launches

```
App launch → Splash → Main (Recent tab)
```

## Main navigation

```
Main
├── Recent (tab)        FAB = Dial pad
├── Contacts (tab)      FAB = Create contact
└── Favorites (tab)     FAB = Dial pad
```

Top bar actions on every tab: Search, Settings.

## Recent

```
Recent
├── All / Missed filter
├── Row tap → expand actions
│   ├── Saved contact: Video call | Messages | Call history
│   └── Unknown: Add contact | Messages | Call history
├── Row call icon → place call → In-call
├── Avatar tap → Contact details (saved) or Call history (unknown)
├── Call history → Call history screen
│   ├── Call (toolbar)
│   ├── Video call / Add contact
│   ├── Messages (SMS app)
│   └── Delete (removes entries for that number)
├── Search → Search
└── Settings → Settings
```

## Contacts

```
Contacts
├── A-Z index → scroll to section
├── Row → Contact details
│   ├── Call / Message / Video
│   ├── Phone row → call; message, video icons
│   ├── Recent activity → Show more → Call history
│   ├── Edit → Edit contact
│   ├── Overflow → Create shortcut (system pin dialog) | Share contact (system share)
│   ├── Favorite / Unfavorite
│   ├── Block / Unblock
│   └── Delete → confirmation dialog → back to list
└── FAB + → Create contact
    ├── Photo → Take photo | Choose photo → Crop
    ├── Save to → account picker
    └── Done → save → Contact details
```

## Favorites

```
Favorites
├── Tile tap → call
├── Tile long press → Remove favorite | Contact details
└── Empty state → Go to contacts
```

## Dial pad

```
Dial pad
├── Keys → digits + matching suggestions
├── Suggestion → fill number / call
├── Backspace (tap) → delete one, (long press) → clear
├── Overflow → Add to contacts | Send message
└── Call → SIM chooser if needed → In-call
```

## Calling

```
Incoming call (full-screen when locked, heads-up otherwise)
├── Answer → Active call
├── Decline → After call (if enabled)
└── Message → Quick response sheet → reject with message

Outgoing call → Dialing → Active call

Active call
├── Mute | Keypad | Speaker | More (Hold, Add call)
└── End → After call (if enabled) → Call again | Message | Save | Favorite | Block | History
```

Missed call → Smart notification → Call back | Message | open Recent (Missed).

## Settings

```
Settings
├── Appearance
│   ├── Language → Choose language
│   └── Theme mode → Theme
├── Calling
│   ├── Default phone app → role request
│   ├── After call screen (switch)
│   ├── Smart notifications (switch)
│   ├── Blocked numbers → list | Block unknown | Add | Unblock dialog
│   └── Quick response → list | Add | Edit | Delete
├── General
│   └── Widget → pin favorites widget
├── Privacy
│   ├── Permissions → status of each, request or open Settings
│   └── Privacy policy
└── Others
    ├── Share app (system share)
    ├── Rate us (Play Store)
    └── About (version)
```

## External entry points

- `tel:` / `ACTION_DIAL` intents → Dial pad with number.
- Home-screen widget favorite → place call.
- Pinned contact shortcut → Contact details.
- Missed-call notification → Recent (Missed) or actions.
- Call notification → In-call screen.
