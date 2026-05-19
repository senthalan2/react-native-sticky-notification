package com.stickynotification

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder

/**
 * Foreground service that hosts the sticky notification.
 *
 * Lifecycle:
 *   START  → builds the notification and calls startForeground()
 *   UPDATE → rebuilds the notification in-place (no flicker)
 *   STOP   → removes the notification and stops the service
 *
 * The service is declared with foregroundServiceType="dataSync" to satisfy
 * the Android 14+ (API 34) requirement for an explicit service type.
 * Corresponding permission: FOREGROUND_SERVICE_DATA_SYNC.
 */
class StickyNotificationService : Service() {

  /** Notification ID used for the current foreground session. */
  private var notificationId = DEFAULT_NOTIFICATION_ID

  /**
   * Last config bundle used to build the notification.
   * Retained so ACTION_SWIPE_DISMISS can decide whether to re-show it and
   * can read the `repostOnDismiss` flag without a full restart.
   */
  private var lastConfig: Bundle? = null

  /**
   * Guards against double-emitting the service-stop event.
   * Set to true once a stop reason has been forwarded to the module; reset
   * to false when a new session starts via ACTION_START.
   */
  private var stopEventEmitted = false

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_START -> {
        val config = intent.getBundleExtra(EXTRA_CONFIG) ?: Bundle()
        notificationId = config.getInt("notificationId", DEFAULT_NOTIFICATION_ID)
        lastConfig = config
        stopEventEmitted = false   // fresh session — clear the guard
        startForegroundWithNotification(config)
        isRunning = true
      }

      ACTION_UPDATE -> {
        if (isRunning) {
          val config = intent.getBundleExtra(EXTRA_CONFIG) ?: Bundle()
          val updateId = config.getInt("notificationId", notificationId)
          lastConfig = config
          val notification = StickyNotificationHelper.buildNotification(this, config)
          StickyNotificationHelper.getNotificationManager(this).notify(updateId, notification)
        }
      }

      ACTION_SWIPE_DISMISS -> {
        // Fired by the notification's deleteIntent whenever the user swipes it away.
        // This replaces the old ACTION_REPOST path and is always attached as the
        // deleteIntent so swipe events are captured on all Android versions.
        StickyNotificationModule.notifyServiceStopped("SWIPE_DISMISS")

        val cfg = lastConfig ?: Bundle()
        val repost = if (cfg.containsKey("repostOnDismiss")) cfg.getBoolean("repostOnDismiss") else true
        if (repost && isRunning) {
          // Re-attach the notification to the service so it reappears.
          startForegroundWithNotification(cfg)
        } else {
          // No repost — mark event emitted so onDestroy doesn't double-fire.
          stopEventEmitted = true
        }
      }

      ACTION_STOP -> {
        StickyNotificationModule.notifyServiceStopped("MANUAL_STOP")
        stopEventEmitted = true
        lastConfig = null
        isRunning = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
          @Suppress("DEPRECATION")
          stopForeground(true)
        }
        stopSelf()
        return START_NOT_STICKY
      }
    }
    return START_STICKY
  }

  private fun startForegroundWithNotification(config: Bundle) {
    val notification = StickyNotificationHelper.buildNotification(this, config)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(
        notificationId,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
      )
    } else {
      startForeground(notificationId, notification)
    }
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onDestroy() {
    // Emit SYSTEM_KILLED only when no explicit stop reason was already reported.
    // This covers unexpected termination by the Android system (OOM killer, etc.)
    // as well as cases where the process is killed without going through ACTION_STOP.
    if (!stopEventEmitted) {
      StickyNotificationModule.notifyServiceStopped("SYSTEM_KILLED")
    }
    stopEventEmitted = false
    isRunning = false
    super.onDestroy()
  }

  companion object {
    const val ACTION_START         = "com.stickynotification.START"
    const val ACTION_UPDATE        = "com.stickynotification.UPDATE"
    const val ACTION_STOP          = "com.stickynotification.STOP"
    const val ACTION_SWIPE_DISMISS = "com.stickynotification.SWIPE_DISMISS"

    const val EXTRA_CONFIG = "config"
    const val DEFAULT_NOTIFICATION_ID = 1337

    /**
     * In-process flag reflecting whether the service is currently running.
     * Written by the service; read by the module (isServiceRunning) and by
     * the receiver to decide whether to attempt direct delivery.
     */
    @Volatile
    var isRunning: Boolean = false
  }
}
