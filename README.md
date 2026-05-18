# react-native-sticky-notification

A production-ready React Native library for displaying a **persistent foreground-service notification** on Android with any number of interactive action buttons. Action taps are delivered reliably to the JavaScript layer regardless of whether the app is in the foreground, background, or has been restarted after a process death.

> **Platform support:** Android only. An iOS stub is included so that shared code compiles without platform guards; all iOS methods resolve immediately or reject with `NOT_SUPPORTED`. See [iOS Limitations](#ios-limitations) for the nearest iOS alternatives.

---

## Table of Contents

1. [Features](#features)
2. [Requirements](#requirements)
3. [Installation](#installation)
4. [Android Setup](#android-setup)
5. [Usage](#usage)
6. [API Reference](#api-reference)
7. [TypeScript Types](#typescript-types)
8. [Button Layout](#button-layout)
9. [Event Delivery Across App States](#event-delivery-across-app-states)
10. [Customisation](#customisation)
11. [iOS Limitations](#ios-limitations)
12. [Known Limitations](#known-limitations)
13. [License](#license)

---

## Features

- Foreground service that survives the user pressing the Home button
- **Unlimited action buttons** — uses a custom `RemoteViews` layout, bypassing Android's standard 3-button cap
- Buttons laid out in configurable rows (`buttonsPerRow`, default 5)
- Optional hard cap on displayed buttons (`maxButtons`)
- Each action delivers a typed event to JS with an optional payload string
- Silent, reliable event delivery in foreground, background, and after process restart
- Notification channel auto-creation on Android 8+
- Full visual customisation: title, body, sub-text, icons, accent colour, priority
- TurboModule / New Architecture compatible (also works on the Old Architecture)
- Zero additional native dependencies beyond React Native itself

---

## Requirements

| Item | Minimum |
|------|---------|
| React Native | 0.73+ |
| Android SDK | 24 (Android 7.0) |
| compileSdk / targetSdk | 34+ recommended |
| Kotlin | 1.9+ |

---

## Installation

```sh
# npm
npm install react-native-sticky-notification

# yarn
yarn add react-native-sticky-notification
```

React Native's [auto-linking](https://github.com/react-native-community/cli/blob/main/docs/autolinking.md) handles the rest. No manual `link` step is needed.

---

## Android Setup

### 1. Permissions

Add the following to your app's `android/app/src/main/AndroidManifest.xml` inside the `<manifest>` element, **above** the `<application>` block:

```xml
<!-- Always required -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

<!-- Required on Android 14+ (API 34) for the dataSync service type -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />

<!-- Required on Android 13+ (API 33) to show any notification -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### 2. Runtime notification permission (Android 13+)

On Android 13+ the user must grant `POST_NOTIFICATIONS` at runtime before a notification can appear. Use [`PermissionsAndroid`](https://reactnative.dev/docs/permissionsandroid) or a library such as `react-native-permissions`:

```tsx
import { PermissionsAndroid, Platform } from 'react-native';

async function requestNotificationPermission(): Promise<boolean> {
  if (Platform.OS !== 'android' || Platform.Version < 33) return true;

  const result = await PermissionsAndroid.request(
    PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS
  );
  return result === PermissionsAndroid.RESULTS.GRANTED;
}
```

Call this before `StickyNotification.startService(...)`.

### 3. No additional manifest entries

The library's own `AndroidManifest.xml` already declares the `<service>` and `<receiver>` entries through manifest merging. You do **not** need to copy them into your app manifest.

### 4. Notification icons

Android requires the small notification icon to be a **white-on-transparent** vector or PNG drawable. Add your icon to `android/app/src/main/res/drawable/` and pass its resource name (without the extension) via `smallIcon`:

```
android/app/src/main/res/drawable/ic_notification.png
```

```ts
StickyNotification.startService({
  title: 'My App',
  smallIcon: 'ic_notification',
  // ...
});
```

---

## Usage

### Basic example

```tsx
import React, { useEffect, useRef } from 'react';
import { View, Button } from 'react-native';
import StickyNotification from 'react-native-sticky-notification';
import type { ActionPressEvent } from 'react-native-sticky-notification';

export default function App() {
  const subscription = useRef<{ remove: () => void } | null>(null);

  useEffect(() => {
    subscription.current = StickyNotification.addActionListener(
      (event: ActionPressEvent) => {
        console.log('Action pressed:', event.actionId, event.payload);

        if (event.actionId === 'stop') {
          StickyNotification.stopService();
        }
      }
    );

    return () => {
      subscription.current?.remove();
    };
  }, []);

  const start = async () => {
    await StickyNotification.startService({
      title: 'Music Player',
      text: 'Now playing: Awesome Song',
      smallIcon: 'ic_notification',
      color: '#1DB954',
      ongoing: true,
      actions: [
        { id: 'prev',  title: 'Prev',  icon: 'ic_skip_previous' },
        { id: 'pause', title: 'Pause', icon: 'ic_pause' },
        { id: 'next',  title: 'Next',  icon: 'ic_skip_next' },
      ],
    });
  };

  return (
    <View>
      <Button title="Start notification" onPress={start} />
      <Button title="Stop notification"  onPress={() => StickyNotification.stopService()} />
    </View>
  );
}
```

### 5-button row (matches old package behaviour)

```ts
await StickyNotification.startService({
  title: 'Floating Toolbar',
  text: 'Tap an action below',
  smallIcon: 'ic_notification',
  buttonsPerRow: 5,
  actions: [
    { id: 'home',      title: 'Home',      icon: 'ic_home' },
    { id: 'search',    title: 'Search',    icon: 'ic_search' },
    { id: 'add',       title: 'Add',       icon: 'ic_add' },
    { id: 'favorites', title: 'Favorites', icon: 'ic_favorite' },
    { id: 'settings',  title: 'Settings',  icon: 'ic_settings' },
  ],
});
```

### More than 5 buttons across multiple rows

```ts
await StickyNotification.startService({
  title: 'Quick Actions',
  text: '10 shortcuts available',
  smallIcon: 'ic_notification',
  buttonsPerRow: 5,   // 5 per row → 2 rows for 10 actions
  actions: [
    { id: 'action1',  title: 'Action 1'  },
    { id: 'action2',  title: 'Action 2'  },
    { id: 'action3',  title: 'Action 3'  },
    { id: 'action4',  title: 'Action 4'  },
    { id: 'action5',  title: 'Action 5'  },
    { id: 'action6',  title: 'Action 6'  },
    { id: 'action7',  title: 'Action 7'  },
    { id: 'action8',  title: 'Action 8'  },
    { id: 'action9',  title: 'Action 9'  },
    { id: 'action10', title: 'Action 10' },
  ],
});
```

### Limiting displayed buttons

```ts
// Provide 10 actions but only display the first 3
await StickyNotification.startService({
  title: 'Player',
  text: 'Now playing',
  maxButtons: 3,
  buttonsPerRow: 3,
  actions: [
    { id: 'prev',  title: 'Prev'  },
    { id: 'pause', title: 'Pause' },
    { id: 'next',  title: 'Next'  },
    // … additional actions defined but hidden by maxButtons
  ],
});
```

### Updating the notification in-place

```ts
// Change content without restarting the service (no visual flicker)
await StickyNotification.updateNotification({
  text: 'Now playing: Another Great Song',
  actions: [
    { id: 'prev', title: 'Prev', icon: 'ic_skip_previous' },
    { id: 'play', title: 'Play', icon: 'ic_play_arrow' },
    { id: 'next', title: 'Next', icon: 'ic_skip_next' },
  ],
});
```

### Passing data through action payloads

```ts
await StickyNotification.startService({
  title: 'Download Manager',
  text: '3 items downloading…',
  actions: [
    {
      id: 'cancel',
      title: 'Cancel',
      icon: 'ic_close',
      payload: JSON.stringify({ taskId: 42 }),
    },
  ],
});

StickyNotification.addActionListener(({ actionId, payload }) => {
  if (actionId === 'cancel' && payload) {
    const { taskId } = JSON.parse(payload);
    cancelDownload(taskId);
  }
});
```

### Navigating to a screen from an action

```tsx
import { NavigationContainerRef } from '@react-navigation/native';

const navigationRef = React.createRef<NavigationContainerRef<any>>();

StickyNotification.addActionListener(({ actionId }) => {
  if (actionId === 'open_details') {
    navigationRef.current?.navigate('Details');
  }
});
```

---

## API Reference

### `StickyNotification.startService(options)`

Starts the Android foreground service and shows the persistent notification. The notification channel is created automatically on first call.

| Parameter | Type | Description |
|-----------|------|-------------|
| `options` | `StickyNotificationOptions` | See [TypeScript Types](#typescript-types) |

Returns `Promise<void>`.

---

### `StickyNotification.stopService()`

Stops the foreground service and removes the notification. Safe to call even when the service is not running.

Returns `Promise<void>`.

---

### `StickyNotification.updateNotification(options)`

Updates the notification content without restarting the service. Supply only the keys you want to change. The `notificationId` must match the one used in `startService`.

Returns `Promise<void>`.

---

### `StickyNotification.isServiceRunning()`

Returns `Promise<boolean>` — `true` when the service is currently active. Always resolves to `false` on iOS.

---

### `StickyNotification.addActionListener(callback)`

Subscribes to action-button press events.

```ts
const sub = StickyNotification.addActionListener((event: ActionPressEvent) => {
  console.log(event.actionId, event.payload);
});

// Remove when done:
sub.remove();
```

Returns an `EmitterSubscription`. Always call `.remove()` on component unmount.

---

### `StickyNotification.removeAllListeners()`

Removes every active action listener at once.

---

## TypeScript Types

```ts
interface NotificationAction {
  /** Unique identifier returned in ActionPressEvent when this button is tapped. */
  id: string;
  /** Label shown on the button. */
  title: string;
  /** Name of a drawable resource in the host app (without file extension). */
  icon?: string;
  /** Arbitrary string forwarded with the action press event. */
  payload?: string;
}

interface StickyNotificationOptions {
  // Channel
  channelId?: string;          // Default: "sticky_notification_channel"
  channelName?: string;
  channelDescription?: string;

  // Identity
  notificationId?: number;     // Default: 1337

  // Content
  title: string;               // Required
  text?: string;
  subText?: string;

  // Icons & colour
  smallIcon?: string;          // Drawable resource name (white-on-transparent PNG)
  largeIcon?: string;          // Drawable resource name decoded as Bitmap
  color?: string;              // Hex accent colour, e.g. "#FF5722"

  // Behaviour
  priority?: 'min' | 'low' | 'default' | 'high' | 'max';
  ongoing?: boolean;           // Default: true (cannot be swiped away)
  autoCancel?: boolean;        // Default: false

  // Actions
  actions?: NotificationAction[];

  /**
   * Number of action buttons per row.
   * Default: 5.  Reduce for wide buttons with long labels.  Minimum: 1.
   */
  buttonsPerRow?: number;

  /**
   * Maximum total buttons to show.
   * Buttons beyond this count are silently hidden.
   * Omit or set to 0 for no limit.
   */
  maxButtons?: number;
}

interface ActionPressEvent {
  /** The `id` of the tapped NotificationAction. */
  actionId: string;
  /** The `payload` from the NotificationAction, if provided. */
  payload?: string;
}
```

---

## Button Layout

### How buttons are arranged

The library uses a custom `RemoteViews` panel injected as the notification's **expanded (big) content view**. This bypasses Android's standard 3-action cap entirely.

- Actions are split into rows of `buttonsPerRow` (default 5).
- Each row is a `LinearLayout` with equal-weight children, so buttons share the available width evenly.
- If a button has an `icon`, the icon is shown above the `title` text.
- If no `icon` is given, only the text is shown.

```
buttonsPerRow: 5, 7 actions → 2 rows
┌───────────────────────────────────────┐
│ Title                                 │  ← standard collapsed view
│ Body text                             │
└───────────────────────────────────────┘
        ↓ user expands ↓
┌───────────────────────────────────────┐
│ App icon  ·  App name  ·  timestamp   │  ← system decoration
├───────────────────────────────────────┤
│ Title                                 │
│ Body text                             │
├ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤
│ [Btn1] [Btn2] [Btn3] [Btn4] [Btn5]   │  ← row 1
│       [Btn6] [Btn7]                   │  ← row 2
└───────────────────────────────────────┘
```

### Choosing `buttonsPerRow`

| Use case | Recommended value |
|----------|-----------------|
| Icon-only or very short labels (≤ 4 chars) | 5 |
| Short labels (≤ 8 chars) | 4 |
| Medium labels (≤ 12 chars) | 3 |
| Long labels | 2 |

### Using `maxButtons`

`maxButtons` is useful when you have a variable-length actions array but want a guaranteed upper bound on the notification UI:

```ts
// Always show at most 6 buttons, 3 per row
{
  buttonsPerRow: 3,
  maxButtons: 6,
  actions: dynamicActions,   // could have any length
}
```

---

## Event Delivery Across App States

| App state | Delivery mechanism |
|-----------|-------------------|
| **Foreground** | `BroadcastReceiver` → module static reference → `NativeEventEmitter` |
| **Background** (process alive) | Same as foreground; event is queued until the JS layer resumes |
| **Killed / restarted** | `BroadcastReceiver` writes event to `SharedPreferences`; the app is launched; the module reads and emits the event in `onHostResume()` |

**Important:** when the app process is killed, the foreground service is also killed and the notification is removed by Android. The killed-state delivery path covers:

- System-initiated process termination (low memory) followed by `START_STICKY` restart.
- The user tapping a button just as the process is dying — event persists in SharedPreferences and is delivered when the app reopens.
- It does **not** cover a user force-stop from Settings (notification disappears; no delivery expected).

---

## Customisation

### Notification icons

All icon fields accept the **resource name** (string) of a drawable placed in `android/app/src/main/res/drawable/`.

```
res/drawable/
  ic_notification.png    ← smallIcon: 'ic_notification'
  ic_pause.png           ← action icon (per-button): 'ic_pause'
  app_logo.png           ← largeIcon: 'app_logo'
```

Vector drawables (XML) are supported for `smallIcon` and per-button icons.

### Accent colour

```ts
color: '#FF5722'   // deep orange
color: '#1DB954'   // Spotify green
```

The `color` field sets the notification's accent colour (small icon tint, expand-line colour on some devices).

### Notification priority / importance

| Value | Behaviour |
|-------|-----------|
| `"min"` | No sound, hidden from status bar on some devices |
| `"low"` | No sound, appears in status bar |
| `"default"` | System default sound / vibration |
| `"high"` | Makes a sound |
| `"max"` | Full heads-up notification |

The channel importance is set once at **first channel creation**. To change importance, use a new `channelId`.

### Custom notification appearance

The expanded panel (`RemoteViews`) provides:
- `title` (bold) + `text` + optional `subText` in the header
- Rows of icon + label buttons below a divider

Android does not allow arbitrary CSS styling inside `RemoteViews`. The panel uses system default text colours, which automatically adapt to the notification background on Android 12+.

---

## iOS Limitations

iOS does not provide an equivalent to Android's foreground service:

1. **No persistent background execution** — iOS aggressively suspends background apps.
2. **No non-dismissible notifications** — iOS has no `ongoing` notification mode.
3. **No direct service-to-JS routing** — iOS notification actions go through the App Delegate.

**Closest iOS alternatives:**

| Use case | iOS approach |
|----------|-------------|
| Media playback controls | `AVAudioSession` + `MPRemoteCommandCenter` (Now Playing) |
| Ongoing call UI | `CallKit` (`CXCallController`) |
| Rich notifications with actions | `UNUserNotificationCenter` with `UNNotificationAction` |
| Background task indication | `BGTaskScheduler` |

The library's JS API returns safe no-ops on iOS:

```ts
await StickyNotification.startService({ title: 'My Service' }); // no-op on iOS
const running = await StickyNotification.isServiceRunning();     // false on iOS
```

---

## Known Limitations

### Expanded view required to see buttons

The action buttons are rendered in the notification's **expanded (big) content view** via `RemoteViews`. The user must expand the notification (long-press or swipe down) to see the buttons. The collapsed view shows only `title` and `text`.

This is intentional: fitting many buttons in the single-row collapsed height is impractical. If you need action buttons visible in the collapsed state, consider limiting to 1–2 buttons and using a `setCustomContentView` approach (currently out of scope).

### `RemoteViews.addView()` is API 16+

Dynamic button injection uses `RemoteViews.addView()`. This library's `minSdkVersion` is 24 (Android 7.0), so this is always available.

### Android 14+ foreground service restrictions

Android 14 (API 34) requires foreground services to declare an explicit service type. This library uses `dataSync`. Review Google's [foreground service documentation](https://developer.android.com/guide/components/foreground-services) if a different type better fits your use case.

### Notification channel settings are immutable after creation

Once a channel is created with a `channelId`, its importance, sound, and vibration settings are user-controlled. To change importance programmatically, use a new `channelId`.

### Force-stop clears the notification

When the user force-stops the app from Android Settings, both the service and the notification are immediately removed. The service will not restart automatically after a force-stop.

### `START_STICKY` restart window

If Android kills the process due to memory pressure, the service is restarted via `START_STICKY`. During the brief window between the kill and the restart the notification disappears and then reappears. This is standard Android foreground service behaviour.

---

## Contributing

- [Development workflow](CONTRIBUTING.md#development-workflow)
- [Sending a pull request](CONTRIBUTING.md#sending-a-pull-request)

## License

MIT © [Senthalan](https://github.com/senthalan2)

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
