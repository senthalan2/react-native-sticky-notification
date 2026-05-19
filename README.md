# React Native Sticky Notification 🔔

A production-ready, fully typed, and deeply customizable React Native library for displaying a **persistent foreground-service notification** on Android with **unlimited interactive action buttons** — delivered reliably to JavaScript in every app state.

[![npm version](https://img.shields.io/npm/v/react-native-sticky-notification.svg)](https://npmjs.com/package/react-native-sticky-notification)
[![TypeScript](https://img.shields.io/badge/TypeScript-Ready-blue.svg)](https://www.typescriptlang.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)

---

## ✨ Why this library?

- **Unlimited Action Buttons**: Uses a custom `RemoteViews` layout to bypass Android's standard 3-button cap. Render any number of buttons in configurable rows — 5, 10, 20+.

- **Rock-Solid Event Delivery**: Action taps reach your JS listener whether the app is in the foreground, background, or was just cold-started from a killed state. Events are persisted in `SharedPreferences` and replayed on resume — zero taps are lost.

- **New Architecture Ready**: Fully compatible with React Native 0.73+ TurboModules and the New Architecture. A deferred `Handler`-based drain with retry back-off ensures events are never emitted into an uninitialised bridge.

- **Deep Visual Customisation**: Control every colour and shape — title, body text, divider, action labels, icon tints, button backgrounds, and border radius — globally or per-button.

- **Smart Dismiss Handling**: On Android 14+, where users can swipe away foreground notifications, the library re-posts the notification instantly via a `deleteIntent` to keep it truly sticky. Toggle this on or off with `repostOnDismiss`.

- **Panel & App Control**: Choose whether tapping an action collapses the notification panel (`closeOnAction`) and/or brings your app to the foreground (`openAppOnAction`). A transparent trampoline Activity handles both reliably on all Android versions including API 29+ Background Activity Launch restrictions.

- **Zero Native Dependencies**: No extra libraries. Everything is built on top of standard Android SDK APIs already bundled with React Native.

---

## 📦 Installation

```bash
npm install react-native-sticky-notification
# or
yarn add react-native-sticky-notification
```

React Native's [auto-linking](https://github.com/react-native-community/cli/blob/main/docs/autolinking.md) handles the rest — no manual `link` step needed.

---

## 🔧 Android Setup

### 1. Permissions

Add the following to your app's `android/app/src/main/AndroidManifest.xml` inside the `<manifest>` block, **above** `<application>`:

```xml
<!-- Always required -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

<!-- Required on Android 14+ (API 34) for the dataSync service type -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />

<!-- Required on Android 13+ (API 33) to show any notification -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### 2. Runtime Notification Permission (Android 13+)

On Android 13+ the user must grant `POST_NOTIFICATIONS` at runtime before any notification can appear:

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

### 3. No Extra Manifest Entries

The library's own `AndroidManifest.xml` already declares the `<service>`, `<receiver>`, and trampoline `<activity>` entries through manifest merging. You do **not** need to copy them into your app manifest.

### 4. Notification Icons

Android requires the small status-bar icon to be a **white-on-transparent** vector or PNG drawable. Add it to `android/app/src/main/res/drawable/` and pass the resource name (without the extension):

```
android/app/src/main/res/drawable/ic_notification.png
```
```ts
StickyNotification.startService({ title: 'My App', smallIcon: 'ic_notification' });
```

---

## 💻 Quick Start

```tsx
import React, { useEffect, useRef } from 'react';
import { View, Button } from 'react-native';
import StickyNotification from 'react-native-sticky-notification';
import type { ActionPressEvent } from 'react-native-sticky-notification';

export default function App() {
  const sub = useRef<{ remove: () => void } | null>(null);

  useEffect(() => {
    sub.current = StickyNotification.addActionListener((event: ActionPressEvent) => {
      console.log('Action pressed:', event.actionId, event.payload);
      if (event.actionId === 'stop') StickyNotification.stopService();
    });
    return () => sub.current?.remove();
  }, []);

  const start = () =>
    StickyNotification.startService({
      title: 'Music Player',
      text: 'Now playing: Awesome Song',
      smallIcon: 'ic_notification',
      color: '#1DB954',
      actions: [
        { id: 'prev',  title: 'Prev',  icon: 'ic_skip_previous' },
        { id: 'pause', title: 'Pause', icon: 'ic_pause'          },
        { id: 'next',  title: 'Next',  icon: 'ic_skip_next'      },
      ],
    });

  return (
    <View>
      <Button title="Start" onPress={start} />
      <Button title="Stop"  onPress={() => StickyNotification.stopService()} />
    </View>
  );
}
```

---

## 🎨 Advanced Examples

### 1. Dark-Themed Player with Rounded Buttons

```ts
StickyNotification.startService({
  title: 'Now Playing',
  text: 'Awesome Song — Artist Name',
  smallIcon: 'ic_notification',
  color: '#1DB954',

  // Divider styling
  showDivider: true,
  dividerColor: '#333333',

  // Text colours
  titleColor: '#FFFFFF',
  textColor: '#AAAAAA',

  // Button container background
  actionsContainerBackground: '#111111',

  // Global button defaults
  actionBorderRadius: 100,         // pill shape
  actionBackground: '#2A2A2A',
  actionLabelColor: '#FFFFFF',
  actionIconTint: '#AAAAAA',
  actionSpacing: 6,                // 6 dp on each side → 12 dp gap between buttons
  rowSpacing: 4,                   // 4 dp above/below each row → 8 dp between rows

  actions: [
    { id: 'prev',  title: 'Prev',  icon: 'ic_skip_previous' },
    {
      id: 'play',
      title: 'Play',
      icon: 'ic_play_arrow',
      background: '#1DB954',       // green pill for play button
      labelColor: '#000000',
      iconTint: '#000000',
      borderRadius: 100,
    },
    { id: 'next',  title: 'Next',  icon: 'ic_skip_next' },
  ],
});
```

### 2. Notification Without Body Text

When `text` is omitted the body row is hidden automatically — no empty gap. Hide the divider too for a clean look:

```ts
StickyNotification.startService({
  title: 'Quick Actions',
  showDivider: false,
  buttonsPerRow: 5,
  actions: [
    { id: 'home',     title: 'Home'     },
    { id: 'search',   title: 'Search'   },
    { id: 'add',      title: 'Add'      },
    { id: 'fav',      title: 'Favourites' },
    { id: 'settings', title: 'Settings' },
  ],
});
```

### 3. Buttons in Both Collapsed and Expanded Views

```ts
StickyNotification.startService({
  title: 'Music Player',
  text: 'Now playing: Awesome Song',
  smallIcon: 'ic_notification',
  color: '#1DB954',

  // Collapsed view — 3 icon-only buttons (fits within the ~64 dp height cap)
  collapsedActions: [
    { id: 'prev',  title: 'Prev',  icon: 'ic_skip_previous' },
    { id: 'pause', title: 'Pause', icon: 'ic_pause'         },
    { id: 'next',  title: 'Next',  icon: 'ic_skip_next'     },
  ],
  showLabelsInCollapsed: false,  // icon-only in collapsed (default)

  // Expanded view — same or richer set of actions
  actions: [
    { id: 'prev',    title: 'Prev',     icon: 'ic_skip_previous' },
    { id: 'pause',   title: 'Pause',    icon: 'ic_pause'         },
    { id: 'next',    title: 'Next',     icon: 'ic_skip_next'     },
    { id: 'shuffle', title: 'Shuffle',  icon: 'ic_shuffle'       },
    { id: 'repeat',  title: 'Repeat',   icon: 'ic_repeat'        },
  ],
});
```

> **Tip**: Pass the same `actions` array to both `collapsedActions` and `actions` if you want identical buttons in both states. Pass a subset (e.g. 3 out of 5) to keep the collapsed view compact.

### 4. Open App & Close Panel on Button Tap

```ts
StickyNotification.startService({
  title: 'Download Manager',
  text: '3 files downloading…',
  closeOnAction: true,     // collapse notification drawer on tap
  openAppOnAction: true,   // bring app to foreground on tap
  actions: [{ id: 'view', title: 'View Progress', icon: 'ic_download' }],
});
```

> **Killed state**: When the app process is not running, tapping any action always opens the app regardless of `openAppOnAction`.

### 4. Sticky Notification on Android 14+

Android 14 allows users to swipe foreground service notifications away. Enable `repostOnDismiss` (default: `true`) to make the notification immediately reappear:

```ts
StickyNotification.startService({
  title: 'Background Sync',
  repostOnDismiss: true,   // default — notification reappears immediately after swipe
  // repostOnDismiss: false  // allow user to hide it temporarily
});
```

### 5. More Than 5 Buttons Across Multiple Rows

```ts
StickyNotification.startService({
  title: 'Toolbar',
  buttonsPerRow: 5,   // 5 per row → 2 rows for 10 actions
  actions: Array.from({ length: 10 }, (_, i) => ({
    id: `action${i + 1}`,
    title: `A${i + 1}`,
  })),
});
```

### 6. Passing Data Through Action Payloads

```ts
StickyNotification.startService({
  title: 'Download Manager',
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

### 7. Live Update Without Flicker

```ts
// Update text and buttons in-place — no service restart, no visual flash
await StickyNotification.updateNotification({
  text: 'Now playing: Another Great Song',
  actions: [
    { id: 'prev',  title: 'Prev', icon: 'ic_skip_previous' },
    { id: 'play',  title: 'Play', icon: 'ic_play_arrow'    },
    { id: 'next',  title: 'Next', icon: 'ic_skip_next'     },
  ],
});
```

### 8. Navigate to a Screen from an Action

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

## 📚 Methods

| Method | Returns | Description |
|---|---|---|
| `startService(options)` | `Promise<void>` | Start the foreground service and show the notification. Channel is created automatically. |
| `stopService()` | `Promise<void>` | Stop the service and remove the notification. Safe to call when not running. |
| `updateNotification(options)` | `Promise<void>` | Update notification content in-place. Supply only the keys you want to change. |
| `isServiceRunning()` | `Promise<boolean>` | `true` when the service is active. Always `false` on iOS. |
| `addActionListener(callback)` | `EmitterSubscription` | Subscribe to action-button tap events. Call `.remove()` on unmount. |
| `removeAllListeners()` | `void` | Remove every active action listener at once. |

---

## ⚙️ Props API Reference

### 📋 Channel & Identity

| Prop | Type | Default | Description |
|---|---|---|---|
| `channelId` | `string` | `"sticky_notification_channel"` | Android notification channel ID. Use a stable value; changing it creates a new channel. |
| `channelName` | `string` | `"Sticky Notification"` | Human-readable channel name shown in Android system settings. |
| `channelDescription` | `string` | — | Optional channel description shown in system settings. |
| `notificationId` | `number` | `1337` | Android notification ID. Use a fixed value to update in-place; change it to show an independent notification. |

### 📝 Content

| Prop | Type | Default | Description |
|---|---|---|---|
| `title` | `string` | — | Bold title line. Hidden automatically when absent or empty — no blank gap. |
| `text` | `string` | — | Body text. Hidden automatically when absent or empty — no blank gap. |
| `subText` | `string` | — | Smaller sub-text below the body. Hidden when absent or empty. |

### 🖼️ Icons & Accent Colour

| Prop | Type | Default | Description |
|---|---|---|---|
| `smallIcon` | `string` | App launcher icon | Drawable resource name for the status-bar icon. Must be a white-on-transparent PNG/vector. |
| `largeIcon` | `string` | — | Drawable resource name decoded as a large bitmap on the right side. |
| `color` | `string` | — | Hex accent colour for the notification, e.g. `"#FF5722"`. |

### 🛠️ Behaviour

| Prop | Type | Default | Description |
|---|---|---|---|
| `priority` | `'min' \| 'low' \| 'default' \| 'high' \| 'max'` | `"default"` | Notification importance / priority. |
| `ongoing` | `boolean` | `true` | Prevent the user from swiping the notification away (pre-Android 14). |
| `autoCancel` | `boolean` | `false` | Dismiss notification when the user taps its body. |
| `repostOnDismiss` | `boolean` | `true` | On Android 14+, where foreground service notifications can be swiped, immediately re-post the notification after dismissal to keep it truly sticky. Set to `false` to allow temporary hiding. |
| `openAppOnAction` | `boolean` | `false` | Bring the app to the foreground whenever any action button is tapped. When the app process is killed, the app always opens regardless of this prop. |
| `closeOnAction` | `boolean` | `false` | Collapse the notification panel when any action button is tapped. Implemented via a transparent trampoline Activity — the only reliable cross-version mechanism on Android 10+. |

### 🔲 Button Layout

| Prop | Type | Default | Description |
|---|---|---|---|
| `actions` | `NotificationAction[]` | — | Interactive buttons displayed in the **expanded** notification panel. No hard limit. |
| `buttonsPerRow` | `number` | `5` | Action buttons per row. Reduce for wider buttons with longer labels. Minimum: 1. |
| `maxButtons` | `number` | `0` (no cap) | Maximum total buttons to display. Buttons beyond this count are silently hidden. |

### 📲 Collapsed View Buttons

| Prop | Type | Default | Description |
|---|---|---|---|
| `collapsedActions` | `NotificationAction[]` | — | Buttons to show in the **collapsed** (non-expanded) notification. When omitted, the collapsed view shows Android's standard title + text template. See limitations below. |
| `showLabelsInCollapsed` | `boolean` | `false` | Show text labels below icons in the collapsed buttons. Disabled by default to fit within Android's ~64 dp collapsed height cap. Enable only with ≤ 2 buttons or icon-less buttons. |

> **Collapsed view limitations**
> - Android enforces a ~64 dp height cap on the collapsed notification — content beyond that is clipped.
> - **Recommended: ≤ 3 buttons, icon-only** (`showLabelsInCollapsed: false`).
> - The same `openAppOnAction`, `closeOnAction`, `actionSpacing`, and per-button colour overrides apply to collapsed buttons.
> - When `collapsedActions` is set and `title` is also set, the title appears above the button row inside the collapsed view.

### 🗂️ Container Styling

| Prop | Type | Default | Description |
|---|---|---|---|
| `containerBackground` | `string` | None | Hex background colour for the entire notification panel, e.g. `"#1A1A1A"` for a dark card. |
| `containerBorderRadius` | `number` | `0` | Corner radius in dp for the notification panel. Requires `containerBackground` to be visible. On Android 12+ (API 31) the content is clipped to rounded corners; on older versions the colour is applied but corners remain square. |

### 🎨 Divider Styling

| Prop | Type | Default | Description |
|---|---|---|---|
| `showDivider` | `boolean` | `true` | Show or hide the horizontal line between the text area and action buttons. Set to `false` when `text` is empty to avoid a floating orphan line. |
| `dividerColor` | `string` | `"#33000000"` | Hex colour for the divider. Has no effect when `showDivider` is `false`. |

### 🖌️ Text Colours

| Prop | Type | Default | Description |
|---|---|---|---|
| `titleColor` | `string` | System default | Hex colour for the title text, e.g. `"#FFFFFF"`. |
| `textColor` | `string` | System default | Hex colour for the body text. |
| `subTextColor` | `string` | System default | Hex colour for the sub-text. |

### 🎭 Action Button Styling (Global)

These apply to **all** action buttons. Individual buttons can override colours and border radius — see [Per-Button Styling](#per-button-styling) below.

| Prop | Type | Default | Description |
|---|---|---|---|
| `actionLabelColor` | `string` | System default | Hex colour applied to every button's text label. |
| `actionIconTint` | `string` | None | Hex tint applied to every button's icon via a `SRC_ATOP` colour filter. No effect on icon-less buttons. |
| `actionBackground` | `string` | None | Hex background colour for each individual button. |
| `actionBorderRadius` | `number` | `0` | Corner radius in dp for button backgrounds. Set to a large value (e.g. `100`) for a pill/capsule shape. Requires `actionBackground` to be visible. |
| `actionSpacing` | `number` | `0` | Horizontal padding added to the **left and right** of every button, in dp. Creates a visible gap between adjacent buttons. The background (including border radius) is drawn inside the padded area, so pill/rounded buttons appear as separate chips. Example: `actionSpacing: 6` → 6 dp on each side → 12 dp gap between two adjacent buttons. |
| `rowSpacing` | `number` | `0` | Vertical padding added **above and below** each row of buttons, in dp. Creates a visible gap between rows when there are multiple rows. Example: `rowSpacing: 4` → 4 dp above and below each row → 8 dp gap between rows. |
| `actionIconSpacing` | `number` | `2` | Vertical gap in dp between the icon and the label text inside each button. Only applies to buttons that have an icon; buttons without an icon are unaffected. |
| `actionsContainerBackground` | `string` | None | Hex background colour for the entire button strip container. |

---

## 🧩 Data Models

### `NotificationAction`

```typescript
interface NotificationAction {
  /** Unique identifier returned in ActionPressEvent when this button is tapped. */
  id: string;
  /** Label shown on the button. */
  title: string;
  /** Drawable resource name in the host app's res/drawable folder (no extension). */
  icon?: string;
  /** Arbitrary string forwarded as-is with the action press event. */
  payload?: string;

  // Per-button styling (overrides global action* props for this button only)
  /** Hex colour for this button's label text. */
  labelColor?: string;
  /** Hex tint applied to this button's icon (SRC_ATOP). */
  iconTint?: string;
  /** Hex background colour for this button's container. */
  background?: string;
  /** Corner radius in dp for this button's background. Overrides global actionBorderRadius. */
  borderRadius?: number;
}
```

### `StickyNotificationOptions`

```typescript
interface StickyNotificationOptions {
  // Channel
  channelId?: string;
  channelName?: string;
  channelDescription?: string;

  // Identity
  notificationId?: number;

  // Content
  title?: string;
  text?: string;
  subText?: string;

  // Icons & accent colour
  smallIcon?: string;
  largeIcon?: string;
  color?: string;

  // Behaviour
  priority?: 'min' | 'low' | 'default' | 'high' | 'max';
  ongoing?: boolean;
  autoCancel?: boolean;
  repostOnDismiss?: boolean;
  openAppOnAction?: boolean;
  closeOnAction?: boolean;

  // Button layout (expanded view)
  actions?: NotificationAction[];
  buttonsPerRow?: number;
  maxButtons?: number;

  // Collapsed view buttons
  collapsedActions?: NotificationAction[];
  showLabelsInCollapsed?: boolean;

  // Divider
  showDivider?: boolean;
  dividerColor?: string;

  // Text colours
  titleColor?: string;
  textColor?: string;
  subTextColor?: string;

  // Container styling
  containerBackground?: string;
  containerBorderRadius?: number;

  // Action button styling (global)
  actionLabelColor?: string;
  actionIconTint?: string;
  actionBackground?: string;
  actionBorderRadius?: number;
  actionSpacing?: number;
  rowSpacing?: number;
  actionIconSpacing?: number;
  actionsContainerBackground?: string;
}
```

### `ActionPressEvent`

```typescript
interface ActionPressEvent {
  /** The `id` of the tapped NotificationAction. */
  actionId: string;
  /** The `payload` string from the NotificationAction, if provided. */
  payload?: string;
}
```

---

## 🔲 Per-Button Styling

Every styling prop that can be set globally also has a per-button override inside `NotificationAction`. Per-button values take priority; missing values fall back to the global prop; if the global prop is also absent, the system default is used.

```
Per-button prop  →  Global action* prop  →  System default
```

```ts
StickyNotification.startService({
  // Global defaults
  actionBorderRadius: 8,
  actionBackground: '#2A2A2A',
  actionLabelColor: '#AAAAAA',

  actions: [
    { id: 'prev', title: '⏮' },                   // uses all globals
    {
      id: 'play',
      title: '▶ Play',
      background: '#1DB954',                        // overrides global background
      labelColor: '#000000',                        // overrides global label colour
      borderRadius: 100,                            // overrides global border radius (pill)
    },
    { id: 'next', title: '⏭' },                   // uses all globals
  ],
});
```

---

## 📡 Event Delivery Across App States

| App state | Delivery mechanism |
|---|---|
| **Foreground** | Receiver → module static reference → `DeviceEventEmitter` |
| **Background** (process alive) | Same path; event is queued until JS layer resumes |
| **Killed / cold start** | Receiver writes event to `SharedPreferences`; app is launched; module reads and emits in `onHostResume()` |

**Retry logic** — on New Architecture, `onHostResume` fires before the JS bundle finishes loading. The module defers the first emit by 150 ms and retries up to 8 times with a 250 ms back-off until the bridge accepts the call.

**Force-stop** — when the user force-stops the app from Android Settings, both the service and the notification are removed immediately. No delivery is expected after a force-stop.

---

## 📐 Button Layout Diagram

```
buttonsPerRow: 5, 7 actions → 2 rows

┌─────────────────────────────────────────┐
│ Title                                   │  ← collapsed view
│ Body text                               │
└─────────────────────────────────────────┘
           ↓ user expands ↓
┌─────────────────────────────────────────┐
│ App icon · App name · timestamp         │  ← system header
├─────────────────────────────────────────┤
│ Title                                   │
│ Body text                               │
├ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤  ← showDivider
│ [Btn1] [Btn2] [Btn3] [Btn4] [Btn5]     │  ← row 1   ↕ rowSpacing
│       [Btn6] [Btn7]                     │  ← row 2
└─────────────────────────────────────────┘
       ↑←actionSpacing→↑
         gap between buttons
```

### Spacing props at a glance

| Prop | Controls | Visual effect |
|---|---|---|
| `actionSpacing` | Left + right padding on each button | Gap between adjacent buttons in the same row |
| `rowSpacing` | Top + bottom padding on each row | Gap between rows |
| `actionIconSpacing` | Top padding on the label inside each button | Gap between icon and label text (icon-bearing buttons only) |

```ts
// Pill buttons with gaps — chip-style layout
StickyNotification.startService({
  actionBorderRadius: 100,
  actionBackground: '#2A2A2A',
  actionSpacing: 6,    // 12 dp gap between adjacent buttons
  rowSpacing: 4,       // 8 dp gap between rows
  actions: [...],
});
```

### Choosing `buttonsPerRow`

| Use case | Recommended value |
|---|---|
| Icon-only or very short labels (≤ 4 chars) | `5` |
| Short labels (≤ 8 chars) | `4` |
| Medium labels (≤ 12 chars) | `3` |
| Long labels | `2` |

---

## 🍎 iOS Limitations

iOS has no equivalent of Android's foreground service:

| Use case | iOS alternative |
|---|---|
| Media playback controls | `AVAudioSession` + `MPRemoteCommandCenter` (Now Playing) |
| Ongoing call UI | `CallKit` (`CXCallController`) |
| Rich notifications with actions | `UNUserNotificationCenter` with `UNNotificationAction` |
| Background task indication | `BGTaskScheduler` |

The library's JS API returns safe no-ops on iOS:

```ts
await StickyNotification.startService({ title: 'My Service' }); // no-op
const running = await StickyNotification.isServiceRunning();     // always false
```

---

## ⚠️ Known Limitations

**Expanded view required for buttons** — Action buttons live in the notification's big-content view. Users must expand the notification (long-press or swipe down) to see them.

**Channel settings are user-controlled after creation** — Importance, sound, and vibration are locked to the user's preference once a channel is created. Use a new `channelId` to change importance programmatically.

**Android 14+ swipe behaviour** — The system allows users to dismiss foreground service notifications. `repostOnDismiss: true` (default) re-posts immediately via `deleteIntent`. Set it to `false` to allow temporary dismissal.

**Force-stop clears everything** — A user force-stop from Android Settings removes the service, notification, and any pending SharedPreferences events immediately.

**`START_STICKY` restart gap** — If Android kills the process under memory pressure, the service restarts via `START_STICKY`. The notification briefly disappears and reappears during the restart window. This is standard Android foreground service behaviour.

---

## 🏪 Google Play Compliance

This section covers every aspect of the library that intersects with Google Play policies, Android's foreground-service rules, and notification-abuse guidelines. Read it before submitting your app.

---

### ✅ What is fully compliant

| Feature | Status | Reason |
|---|---|---|
| Foreground service with visible notification | ✅ | Tied to an active, user-started operation |
| Unlimited action buttons via `RemoteViews` | ✅ | Standard Android API, no policy restrictions |
| `POST_NOTIFICATIONS` runtime permission | ✅ | Declared correctly; must be requested with rationale |
| `closeOnAction` trampoline activity | ✅ | Launching an Activity from a notification is the recommended pattern |
| `openAppOnAction` | ✅ | Bringing an app to foreground from a notification the user tapped is explicitly permitted |
| Killed-state delivery via `SharedPreferences` | ✅ | Standard pattern used by all major notification libraries |
| `START_STICKY` service restart | ✅ | Standard Android foreground service contract |

---

### ⚠️ Areas that need your attention

#### 1. Foreground Service Type — choose the type that matches your app

The library declares `foregroundServiceType="dataSync"` in its manifest. **Google Play requires the declared type to accurately describe what your service actually does.** Using the wrong type can cause rejection or removal.

| App category | Use case | Correct `foregroundServiceType` |
|---|---|---|
| 💳 **Fintech / Banking** | Quick pay, transfer, balance check, transactions | **`dataSync`** ✅ library default — no change needed |
| 🎵 Music / Podcast | Audio playback controls | `mediaPlayback` |
| 🗺️ Navigation / Delivery | Turn-by-turn, live location | `location` |
| 📞 VoIP / Calling | Active phone or video call | `phoneCall` |
| 📁 File Manager / Cloud | Upload / download progress | `dataSync` ✅ |
| 🏋️ Fitness / Health | Workout tracking, step counter | `health` |
| 📷 Camera / Recording | Camera or microphone in use | `camera` / `microphone` |
| 🔵 IoT / Wearables | Bluetooth or USB device | `connectedDevice` |

> **Fintech apps:** Every action in a financial quick-panel (payment initiation, balance refresh, transaction history) is a network call that reads from or writes to a remote financial server — that is precisely the definition of `dataSync`. **The library default is correct for Fintech. No manifest override is needed.**

If your app falls into a category that requires a different type, override it in your app's `AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

  <application>
    <!-- Example: override to mediaPlayback for a music player -->
    <service
      android:name="com.stickynotification.StickyNotificationService"
      android:foregroundServiceType="mediaPlayback"
      tools:replace="android:foregroundServiceType" />
  </application>

</manifest>
```

And declare the matching permission:

```xml
<!-- mediaPlayback -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />

<!-- location -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />

<!-- health -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_HEALTH" />
```

> `FOREGROUND_SERVICE_DATA_SYNC` is already declared by the library. If you switch to a different type, add `tools:remove="android:name"` for the `dataSync` permission or it will remain as an unused declared permission.

---

#### 2. `repostOnDismiss` — use responsibly

Android 14 deliberately gave users the ability to swipe away foreground service notifications. Setting `repostOnDismiss: true` (the library default) immediately re-posts the notification after the user swipes it, effectively overriding that control.

Google Play's **Device and Network Abuse policy** prohibits apps from "circumventing system processes or controls." Re-posting is acceptable when the notification provides real-time interactive functionality the user needs; it is not acceptable for purely informational or promotional content.

| Scenario | Recommendation |
|---|---|
| 💳 Fintech quick-actions panel — user needs it to initiate transactions | `repostOnDismiss: true` ✅ |
| 🎵 Music player — user needs transport controls visible | `repostOnDismiss: true` ✅ |
| 🗺️ Navigation — turn-by-turn must stay visible | `repostOnDismiss: true` ✅ |
| 📁 Download progress — informational only | `repostOnDismiss: false` ✅ |
| 🏋️ Step counter — informational only | `repostOnDismiss: false` ✅ |
| Any advertising or promotional content | `repostOnDismiss: false` — required |

---

#### 3. `POST_NOTIFICATIONS` permission rationale

Google Play requires that runtime permission requests are preceded by an explanation of why the permission is needed. Show a rationale dialog **before** calling `PermissionsAndroid.request` if the user has previously denied the permission:

```tsx
import { PermissionsAndroid, Platform } from 'react-native';

async function ensureNotificationPermission(): Promise<boolean> {
  if (Platform.OS !== 'android' || Platform.Version < 33) return true;

  const already = await PermissionsAndroid.check(
    PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS
  );
  if (already) return true;

  const needsRationale = await PermissionsAndroid.shouldShowRequestPermissionRationale(
    PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS
  );
  if (needsRationale) {
    // Show your own dialog explaining why the notification is needed
    // e.g. "We show a quick-actions panel so you can pay or check your
    //       balance without opening the app."
    await showNotificationRationaleDialog();
  }

  const result = await PermissionsAndroid.request(
    PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS
  );
  return result === PermissionsAndroid.RESULTS.GRANTED;
}
```

---

#### 4. Fintech-specific Play Store considerations

Financial apps are subject to Google Play's **Financial Services policy** in addition to the standard policies. Relevant points:

- **Transparent purpose**: The Play Store listing must clearly state that the app displays a persistent notification panel for quick financial actions. Reviewers may check that the notification is non-deceptive and accurately reflects the app's functionality.
- **No misleading UI**: The notification must not simulate system alerts, banking alerts from other institutions, or security warnings.
- **User-initiated only**: The foreground service must only start when the user explicitly enables the quick-actions panel — not silently on app launch.
- **Data security**: Action payloads transmitted through the notification (`payload` prop) are passed as plain strings in `SharedPreferences` when the app is in a killed state. **Do not put sensitive financial data (account numbers, tokens, amounts) in the `payload` field.** Use opaque identifiers instead.

---

#### 5. Acceptable vs unacceptable use cases

| ✅ Acceptable | ❌ Not acceptable |
|---|---|
| Fintech quick-actions (pay, transfer, balance) | Advertising or promotional banners |
| Media player transport controls | Spam or unsolicited re-engagement |
| Active navigation / live location | Keeping the app alive purely to collect analytics |
| File upload / download with progress | Circumventing battery optimization without consent |
| VoIP call in progress | Content that misleads or impersonates system UI |
| Real-time data sync the user initiated | Background activity the user did not start |

---

### 📋 Pre-submission checklist

- [ ] `foregroundServiceType` matches your actual use case (Fintech → `dataSync` ✅ already set)
- [ ] The matching `FOREGROUND_SERVICE_*` permission is declared (Fintech → `FOREGROUND_SERVICE_DATA_SYNC` ✅ already declared by the library)
- [ ] `POST_NOTIFICATIONS` is requested at runtime with a clear user-facing rationale
- [ ] The foreground service starts only in response to a direct user action — never silently on launch
- [ ] `repostOnDismiss` is justified if `true` (quick-actions panels are justified; informational notifications are not)
- [ ] No sensitive financial data is placed in the `payload` field of any `NotificationAction`
- [ ] The Play Store listing description mentions the persistent notification feature
- [ ] If Google Play requests a video during review, record the full flow: user enables the panel → notification appears → action button tapped → app responds

---

## 🤝 Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development workflow and pull-request guidelines.

---

## License

MIT © [Senthalan](https://github.com/senthalan2)

---

## Support

If you find this project helpful, please consider supporting it:

⭐ **Give it a star on GitHub** — Your stars help keep this project alive and improving!

[![GitHub stars](https://img.shields.io/github/stars/senthalan2/react-native-sticky-notification?style=social)](https://github.com/senthalan2/react-native-sticky-notification/stargazers)

☕ **Buy me a coffee** — Your support keeps me motivated to maintain and enhance this package!

<a href="https://www.buymeacoffee.com/senthalan2" target="_blank">
  <img src="https://cdn.buymeacoffee.com/buttons/v2/default-red.png" alt="Buy Me A Coffee" style="height: 60px !important;width: 217px !important;" >
</a>

Thank you for your support! 🙏
