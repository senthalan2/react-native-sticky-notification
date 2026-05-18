package com.stickynotification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver that handles notification action-button taps.
 *
 * Delivery strategy
 * ─────────────────
 * 1. App process is alive → delegate directly to the module instance.
 *    The module queues the event until the JS layer resumes if needed.
 *
 * 2. App process is dead → write the event to SharedPreferences and then
 *    launch the app.  On startup the module reads and clears that entry from
 *    SharedPreferences in onHostResume() and emits it to the JS layer.
 *
 * The activity-start from a notification-triggered BroadcastReceiver is an
 * explicitly permitted case in Android 10+ BAL restrictions (the user
 * interaction that fired the notification action is the origin).
 */
class StickyNotificationReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != ACTION_NOTIFICATION_ACTION) return

    val actionId = intent.getStringExtra(EXTRA_ACTION_ID) ?: return
    val payload = intent.getStringExtra(EXTRA_PAYLOAD)

    val delivered = StickyNotificationModule.notifyActionPressed(actionId, payload)

    if (!delivered) {
      // Process is not running — persist so the module picks it up on resume
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_PENDING_ACTION, actionId)
        .apply {
          if (payload != null) putString(KEY_PENDING_PAYLOAD, payload)
          else remove(KEY_PENDING_PAYLOAD)
        }
        .apply()

      // Launch the app to let the user see the result
      context.packageManager
        .getLaunchIntentForPackage(context.packageName)
        ?.apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_SINGLE_TOP or
            Intent.FLAG_ACTIVITY_CLEAR_TOP
          putExtra(EXTRA_ACTION_ID, actionId)
          payload?.let { putExtra(EXTRA_PAYLOAD, it) }
        }
        ?.let { context.startActivity(it) }
    }
  }

  companion object {
    const val ACTION_NOTIFICATION_ACTION = "com.stickynotification.ACTION_PRESSED"
    const val EXTRA_ACTION_ID = "actionId"
    const val EXTRA_PAYLOAD = "payload"

    // SharedPreferences used for cross-process (killed-state) event persistence.
    // Read by StickyNotificationModule.onHostResume().
    const val PREFS_NAME = "StickyNotificationPrefs"
    const val KEY_PENDING_ACTION = "pendingActionId"
    const val KEY_PENDING_PAYLOAD = "pendingPayload"
  }
}
