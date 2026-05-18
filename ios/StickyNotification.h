#import <StickyNotificationSpec/StickyNotificationSpec.h>

/**
 * iOS stub — foreground-service sticky notifications are an Android-only
 * concept.  iOS does not support persistent background services that can
 * display interactive notification buttons in the same way.
 *
 * All methods on this stub reject their promises with NOT_SUPPORTED or
 * resolve with a safe no-op value so that cross-platform JS code that
 * guards on Platform.OS works without extra try/catch handling.
 *
 * See the README for the closest iOS alternatives (UNUserNotificationCenter,
 * CallKit, etc.).
 */
@interface StickyNotification : NSObject <NativeStickyNotificationSpec>

@end
