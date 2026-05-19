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
  /** Bold title line (required). */
  title: string;
  /** Body text shown below the title. */
  text?: string;
  /** Sub-text shown in a smaller font below the body. */
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
