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

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_START -> {
        val config = intent.getBundleExtra(EXTRA_CONFIG) ?: Bundle()
        notificationId = config.getInt("notificationId", DEFAULT_NOTIFICATION_ID)
        startForegroundWithNotification(config)
        isRunning = true
      }

      ACTION_UPDATE -> {
        if (isRunning) {
          val config = intent.getBundleExtra(EXTRA_CONFIG) ?: Bundle()
          // Use the notificationId from the update payload if provided
          val updateId = config.getInt("notificationId", notificationId)
          val notification = StickyNotificationHelper.buildNotification(this, config)
          StickyNotificationHelper.getNotificationManager(this).notify(updateId, notification)
        }
      }

      ACTION_STOP -> {
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
    isRunning = false
    super.onDestroy()
  }

  companion object {
    const val ACTION_START = "com.stickynotification.START"
    const val ACTION_UPDATE = "com.stickynotification.UPDATE"
    const val ACTION_STOP = "com.stickynotification.STOP"

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
