package com.stickynotification

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * Invisible trampoline activity that handles notification action-button taps.
 *
 * Why an Activity instead of a BroadcastReceiver?
 * ────────────────────────────────────────────────
 * 1. Notification panel — Starting an Activity from a notification PendingIntent
 *    causes the system to collapse the notification drawer automatically.
 *    A broadcast alone never closes the panel.
 *
 * 2. Background Activity Launch (BAL) restrictions (Android 10+) — Custom
 *    RemoteViews buttons using PendingIntent.getBroadcast() may not receive a
 *    BAL token, so the receiver's subsequent startActivity() call is silently
 *    blocked in killed-state scenarios.  Using PendingIntent.getActivity()
 *    directly bypasses BAL restrictions because the start is attributed to the
 *    user's notification tap.
 *
 * The activity uses Theme.NoDisplay so it produces no visible window, and
 * always calls finish() before returning, so it never appears in Recents.
 */
class StickyNotificationTrampolineActivity : Activity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val actionId = intent.getStringExtra(EXTRA_ACTION_ID)
    val payload = intent.getStringExtra(EXTRA_PAYLOAD)
    val openApp = intent.getBooleanExtra(EXTRA_OPEN_APP, false)

    if (actionId != null) {
      val delivered = StickyNotificationModule.notifyActionPressed(actionId, payload)

      if (!delivered) {
        // Module not ready — process was just created from killed state.
        // Persist the event; StickyNotificationModule.drainEvents() will
        // emit it to JS once onHostResume fires after RN initialises.
        getSharedPreferences(StickyNotificationReceiver.PREFS_NAME, Context.MODE_PRIVATE)
          .edit()
          .putString(StickyNotificationReceiver.KEY_PENDING_ACTION, actionId)
          .apply {
            if (payload != null) putString(StickyNotificationReceiver.KEY_PENDING_PAYLOAD, payload)
            else remove(StickyNotificationReceiver.KEY_PENDING_PAYLOAD)
          }
          .apply()

        // App was not running — always bring it to foreground so the user
        // can see the result of their tap.
        openMainActivity()
      } else if (openApp) {
        // App was running and delivered; bring it forward only if requested.
        openMainActivity()
      }
    }

    finish()
  }

  private fun openMainActivity() {
    packageManager.getLaunchIntentForPackage(packageName)
      ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP }
      ?.let { startActivity(it) }
  }

  companion object {
    const val EXTRA_ACTION_ID = "actionId"
    const val EXTRA_PAYLOAD = "payload"
    const val EXTRA_OPEN_APP = "openApp"
  }
}
