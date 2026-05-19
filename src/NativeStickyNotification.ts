import { TurboModuleRegistry, type TurboModule } from 'react-native';

/**
 * TurboModule spec for react-native-sticky-notification.
 *
 * All methods that perform I/O are async (Promise-based) so they never block
 * the JS thread.  The addListener / removeListeners pair is required by
 * NativeEventEmitter to correctly manage subscription reference counts.
 */
export interface Spec extends TurboModule {
  /** Start the foreground service and display the sticky notification. */
  startService(config: Object): Promise<void>;

  /** Stop the foreground service and remove the notification. */
  stopService(): Promise<void>;

  /** Update the visible notification content without restarting the service. */
  updateNotification(config: Object): Promise<void>;

  /** Returns true when the foreground service is currently running. */
  isServiceRunning(): Promise<boolean>;

  /**
   * Returns true when the device runs Android 14+ (API 34), where the system
   * allows users to swipe away foreground service notifications even when
   * ongoing is true.
   */
  canSwipeDismiss(): Promise<boolean>;

  // Required by NativeEventEmitter
  addListener(eventName: string): void;
  removeListeners(count: number): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>('StickyNotification');
