import { DeviceEventEmitter, Platform } from 'react-native';
import type { EmitterSubscription } from 'react-native';
import NativeStickyNotification from './NativeStickyNotification';

// ─── Public types ──────────────────────────────────────────────────────────

/**
 * A single interactive button displayed inside the notification.
 */
export interface NotificationAction {
  /** Unique identifier returned to JS when this action is tapped. */
  id: string;
  /** Button label shown in the notification. */
  title: string;
  /**
   * Name of a drawable resource in the host app's res/drawable folder.
   * Example: "ic_play" maps to res/drawable/ic_play.png.
   */
  icon?: string;
  /**
   * Arbitrary string forwarded as-is with the action press event.
   * Use it to carry route names, item IDs, or any JSON-serialised data.
   */
  payload?: string;

  // ── Per-button styling (overrides the global action* props) ──────────────
  /** Hex colour for this button's label text, e.g. "#FF5722". */
  labelColor?: string;
  /** Hex tint applied to this button's icon. Has no effect if no icon is set. */
  iconTint?: string;
  /** Hex background colour for this button's container. */
  background?: string;
  /**
   * Corner radius for this button's background, in dp.
   * Overrides the global `actionBorderRadius` for this button only.
   * Set to a very large value (e.g. 100) for a pill / capsule shape.
   */
  borderRadius?: number;
}

/**
 * Full configuration object passed to startService / updateNotification.
 */
export interface StickyNotificationOptions {
  // ── Channel ──────────────────────────────────────────────────────────────
  /**
   * Android notification channel ID.
   * Use a stable value across restarts; changing it creates a new channel.
   * Default: "sticky_notification_channel"
   */
  channelId?: string;
  /** Human-readable channel name shown in Android system settings. */
  channelName?: string;
  /** Optional channel description shown in Android system settings. */
  channelDescription?: string;

  // ── Identity ─────────────────────────────────────────────────────────────
  /**
   * Android notification ID.  Use a fixed value if you want to update
   * the notification in-place; change it to show a new independent
   * notification.  Default: 1337.
   */
  notificationId?: number;

  // ── Content ──────────────────────────────────────────────────────────────
  /**
   * Bold title line.
   * Hidden automatically when absent or empty — no blank gap above the body text.
   */
  title?: string;
  /**
   * Body text shown below the title.
   * Hidden automatically when absent or empty — no blank-line artefact.
   */
  text?: string;
  /**
   * Sub-text shown in a smaller font below the body.
   * Hidden automatically when absent or empty.
   */
  subText?: string;

  // ── Icons & colour ───────────────────────────────────────────────────────
  /**
   * Drawable resource name for the small status-bar icon.
   * Must be a white-on-transparent PNG for correct rendering.
   * Defaults to the application launcher icon.
   */
  smallIcon?: string;
  /**
   * Drawable resource name decoded as a large bitmap displayed on the right
   * side of the notification (app-icon area).
   */
  largeIcon?: string;
  /**
   * Accent colour for the notification.  Must be a valid CSS/Android hex
   * string, e.g. "#FF5722".
   */
  color?: string;

  // ── Behaviour ────────────────────────────────────────────────────────────
  /** Notification importance / priority. Default: "default". */
  priority?: 'min' | 'low' | 'default' | 'high' | 'max';
  /**
   * Prevent the user from swiping the notification away.
   * Default: true (sticky behaviour).
   */
  ongoing?: boolean;
  /**
   * Dismiss the notification when the user taps its body.
   * Default: false.
   */
  autoCancel?: boolean;
  /**
   * Open the app when an action button is tapped while the app is in the
   * foreground or background.
   *
   * When the app process is killed, the app is always brought to the
   * foreground regardless of this prop (the process must start to deliver
   * the event to JS).
   *
   * Default: false.
   */
  openAppOnAction?: boolean;

  /**
   * Collapse the notification panel when an action button is tapped.
   *
   * Implemented via a transparent trampoline Activity.  Starting an Activity
   * from the notification is the only reliable cross-version mechanism to
   * close the notification drawer; it also bypasses Android 10+ Background
   * Activity Launch restrictions that can silently block panel closure when
   * using a BroadcastReceiver.
   *
   * Default: false.
   */
  closeOnAction?: boolean;

  /**
   * Re-post the notification immediately after the user swipes it away.
   *
   * Android 14+ allows users to dismiss foreground service notifications even
   * when `ongoing` is true.  Setting this to `true` attaches a `deleteIntent`
   * that calls back into the running service to re-show the notification,
   * making it effectively non-dismissible on all Android versions.
   *
   * Set to `false` if you want Android 14+ users to be able to hide the
   * notification temporarily without stopping the service.
   *
   * Default: true.
   */
  repostOnDismiss?: boolean;

  // ── Actions ───────────────────────────────────────────────────────────────
  /**
   * Interactive buttons displayed inside the notification panel.
   * There is no hard limit — buttons are arranged in rows using a custom
   * RemoteViews layout, bypassing Android's standard 3-action cap.
   */
  actions?: NotificationAction[];
  /**
   * Action buttons to display in the **collapsed** (non-expanded) notification.
   *
   * When provided, the collapsed view is replaced with a compact row of these
   * buttons. When omitted, the collapsed view shows Android's standard
   * title + text template (default behaviour).
   *
   * **Limitations**:
   * - Android caps the collapsed notification height at ~64 dp. Exceeding 3
   *   buttons or enabling labels may cause content to be clipped.
   * - Recommended: ≤ 3 icon-only buttons.
   * - Labels are hidden by default; set `showLabelsInCollapsed: true` to show them.
   */
  collapsedActions?: NotificationAction[];
  /**
   * Show text labels below icons in the collapsed notification buttons.
   *
   * Labels are hidden by default in collapsed mode to fit within the ~64 dp
   * height limit. Enable this only when using ≤ 2 buttons or when buttons
   * have no icons.
   *
   * Has no effect when `collapsedActions` is not provided.
   * Default: false.
   */
  showLabelsInCollapsed?: boolean;

  // ── Divider ───────────────────────────────────────────────────────────────
  /**
   * Show the horizontal line between the text area and the action buttons.
   * Set to `false` to hide it — useful when `text` is empty and the line
   * would otherwise sit awkwardly right below the title.
   * Default: true.
   */
  showDivider?: boolean;
  /**
   * Colour of the divider line.  Any CSS/Android hex string, e.g. "#33000000".
   * Has no effect when `showDivider` is false.
   */
  dividerColor?: string;

  // ── Text colours ─────────────────────────────────────────────────────────
  /** Hex colour for the title text, e.g. "#FFFFFF". */
  titleColor?: string;
  /** Hex colour for the body text. */
  textColor?: string;
  /** Hex colour for the sub-text. */
  subTextColor?: string;

  // ── Container styling ────────────────────────────────────────────────────
  /**
   * Hex background colour for the entire notification panel.
   * Example: "#1A1A1A" for a dark panel.
   */
  containerBackground?: string;
  /**
   * Corner radius for the notification panel background, in dp.
   * Requires `containerBackground` to be set for the rounding to be visible.
   * On Android 12+ (API 31) the corners are clipped; on older versions the
   * background colour is applied but corners remain square.
   * Default: 0.
   */
  containerBorderRadius?: number;

  // ── Action button styling ─────────────────────────────────────────────────
  /**
   * Hex colour applied to every action button's text label.
   * Example: "#FFFFFF" for white labels.
   */
  actionLabelColor?: string;
  /**
   * Hex tint colour applied to every action button's icon image via a
   * SRC_ATOP colour filter.  Has no effect on buttons without an icon.
   */
  actionIconTint?: string;
  /**
   * Hex background colour for each individual action button.
   * Applied to the button's root container, so it covers the entire tap area.
   */
  actionBackground?: string;
  /**
   * Corner radius for action button backgrounds, in dp.
   * Requires `actionBackground` to be set on the button (global or per-button)
   * unless you only want rounded transparent clipping.
   *
   * Set to a very large value (e.g. 100) for a pill / capsule shape.
   * Default: 0 (square corners).
   */
  actionBorderRadius?: number;
  /**
   * Horizontal padding applied to each action button, in dp.
   *
   * This padding is added on the left and right of every button's root
   * container, which creates a visible gap between adjacent buttons.
   * The background (including border radius) is drawn inside the padded
   * area, so rounded buttons will appear as separate chips with space
   * between them.
   *
   * Example: `actionSpacing: 4` → 4 dp gap on each side of every button
   * → 8 dp visible gap between two adjacent buttons.
   *
   * Default: 0.
   */
  actionSpacing?: number;
  /**
   * Vertical padding applied to each button row container, in dp.
   *
   * Adds space above and below every row of action buttons, creating a
   * visible gap between rows when `buttonsPerRow` is less than the total
   * number of actions.
   *
   * Default: 0.
   */
  rowSpacing?: number;
  /**
   * Vertical gap between the icon and the label text inside each action button, in dp.
   *
   * Only applies to buttons that have an icon; buttons with no icon are unaffected.
   *
   * Default: 2.
   */
  actionIconSpacing?: number;
  /**
   * Hex background colour for the entire row-container that holds all the
   * action buttons.  Use this to colour the strip as a whole while keeping
   * individual buttons transparent.
   */
  actionsContainerBackground?: string;

  /**
   * Maximum number of action buttons to display.
   * When set, any actions beyond this count are silently ignored.
   * Omit (or set to 0) to show all provided actions.
   */
  maxButtons?: number;

  /**
   * How many action buttons to place on each row of the notification panel.
   * Default: 5.  Increase for icon-only compact buttons, decrease for
   * wider buttons with long labels.
   * Minimum: 1.
   */
  buttonsPerRow?: number;

  // ── Footer text ───────────────────────────────────────────────────────────
  /**
   * Optional text displayed below the action buttons, centered horizontally.
   * Hidden when absent or empty.
   */
  footerText?: string;
  /**
   * Default (lowest priority) colour for the footer text, e.g. "#888888".
   * Overridden by `footerWordColors` and `footerLetterColors` where they apply.
   */
  footerTextColor?: string;
  /**
   * Per-word colour overrides for the footer text.
   * Every occurrence of `word` in the footer string is coloured with `color`.
   * Overrides `footerTextColor`; overridden by `footerLetterColors`.
   *
   * @example
   * footerWordColors: [{ word: 'Kansas', color: '#FF5722' }]
   */
  footerWordColors?: Array<{ word: string; color: string }>;
  /**
   * Per-character colour overrides for the footer text (highest priority).
   * `index` is the zero-based character position in `footerText`.
   * Overrides both `footerTextColor` and `footerWordColors`.
   *
   * @example
   * footerLetterColors: [{ index: 0, color: '#FF0000' }, { index: 1, color: '#00FF00' }]
   */
  footerLetterColors?: Array<{ index: number; color: string }>;
}

/**
 * Event payload delivered to the JS action listener.
 */
export interface ActionPressEvent {
  /** The `id` of the NotificationAction that was tapped. */
  actionId: string;
  /** The `payload` string from the NotificationAction, if any. */
  payload?: string;
}

// ─── Internal event plumbing ───────────────────────────────────────────────

const ACTION_PRESS_EVENT = 'StickyNotification_onActionPress';

// DeviceEventEmitter is used instead of NativeEventEmitter(module) for two reasons:
//  1. NativeEventEmitter forces the TurboModule to load at import time, which can
//     race with the New Architecture bridge initialisation and cause a
//     ReactNoCrashSoftException on cold launch.
//  2. DeviceEventEmitter is lazily subscribed — it has no module dependency and
//     works identically with the RCTDeviceEventEmitter emissions from the Kotlin side.

// ─── Public API ────────────────────────────────────────────────────────────

/**
 * `StickyNotification` — main entry point for the library.
 *
 * All methods are safe to call on both platforms; on iOS they resolve
 * immediately with no-op / false values so that shared code does not need
 * platform guards.
 */
export const StickyNotification = {
  /**
   * Start the Android foreground service and display the notification.
   *
   * The promise resolves once the service start command has been issued.
   * The notification channel is created (or updated) automatically.
   *
   * Requires the `POST_NOTIFICATIONS` runtime permission on Android 13+.
   */
  startService(options: StickyNotificationOptions): Promise<void> {
    if (Platform.OS !== 'android') return Promise.resolve();
    return NativeStickyNotification.startService(options as Object);
  },

  /**
   * Stop the foreground service and remove the notification.
   * Safe to call even when the service is not running.
   */
  stopService(): Promise<void> {
    if (Platform.OS !== 'android') return Promise.resolve();
    return NativeStickyNotification.stopService();
  },

  /**
   * Update the notification content without restarting the service.
   * Only the keys you provide are applied; unspecified keys retain their
   * previous values from the native layer.
   *
   * Note: the `channelId` / `notificationId` must match the values used in
   * `startService` for the update to affect the correct notification.
   */
  updateNotification(options: Partial<StickyNotificationOptions>): Promise<void> {
    if (Platform.OS !== 'android') return Promise.resolve();
    return NativeStickyNotification.updateNotification(options as Object);
  },

  /**
   * Returns `true` when the foreground service is currently active.
   * Always returns `false` on iOS.
   */
  isServiceRunning(): Promise<boolean> {
    if (Platform.OS !== 'android') return Promise.resolve(false);
    return NativeStickyNotification.isServiceRunning();
  },

  /**
   * Subscribe to action-button press events from the notification.
   *
   * The callback is called whenever the user taps an action button.
   * It receives an `ActionPressEvent` with the action's `id` and optional
   * `payload`.
   *
   * Returns an `EmitterSubscription` — call `.remove()` on it to unsubscribe.
   *
   * @example
   * ```ts
   * const sub = StickyNotification.addActionListener(({ actionId, payload }) => {
   *   if (actionId === 'stop') stopPlayback();
   * });
   * // later:
   * sub.remove();
   * ```
   */
  addActionListener(
    callback: (event: ActionPressEvent) => void
  ): EmitterSubscription {
    if (Platform.OS !== 'android') {
      // Return a no-op subscription on unsupported platforms
      return { remove: () => {} } as EmitterSubscription;
    }
    return DeviceEventEmitter.addListener(ACTION_PRESS_EVENT, (event) =>
      callback(event as ActionPressEvent)
    );
  },

  /** Remove all active action listeners at once. */
  removeAllListeners(): void {
    if (Platform.OS === 'android') {
      DeviceEventEmitter.removeAllListeners(ACTION_PRESS_EVENT);
    }
  },
};

export default StickyNotification;
