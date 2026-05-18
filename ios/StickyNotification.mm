#import "StickyNotification.h"

@implementation StickyNotification

// ─── TurboModule boilerplate ──────────────────────────────────────────────

+ (NSString *)moduleName
{
  return @"StickyNotification";
}

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
  return std::make_shared<facebook::react::NativeStickyNotificationSpecJSI>(params);
}

// ─── Spec implementation (no-ops / rejections) ────────────────────────────

- (void)startService:(JS::NativeStickyNotification::StickyNotificationConfig &)config
             resolve:(RCTPromiseResolveBlock)resolve
              reject:(RCTPromiseRejectBlock)reject
{
  reject(
    @"NOT_SUPPORTED",
    @"react-native-sticky-notification: foreground-service sticky notifications "
    @"are not supported on iOS.  See the README for platform alternatives.",
    nil
  );
}

- (void)stopService:(RCTPromiseResolveBlock)resolve
             reject:(RCTPromiseRejectBlock)reject
{
  resolve(nil);
}

- (void)updateNotification:(JS::NativeStickyNotification::StickyNotificationConfig &)config
                   resolve:(RCTPromiseResolveBlock)resolve
                    reject:(RCTPromiseRejectBlock)reject
{
  reject(
    @"NOT_SUPPORTED",
    @"react-native-sticky-notification: foreground-service sticky notifications "
    @"are not supported on iOS.",
    nil
  );
}

- (void)isServiceRunning:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject
{
  resolve(@NO);
}

- (void)addListener:(NSString *)eventName {}

- (void)removeListeners:(double)count {}

@end
