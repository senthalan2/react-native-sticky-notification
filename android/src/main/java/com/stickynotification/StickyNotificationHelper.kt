package com.stickynotification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat

/**
 * Stateless helper that converts a config Bundle into an Android Notification.
 *
 * Action buttons are rendered in a custom big-content view (notification_panel.xml)
 * using RemoteViews.addView() so the count is not bounded by the standard
 * NotificationCompat.addAction() three-button limit.  Buttons are laid out in
 * rows; each row is a notification_action_row inflated at runtime, and each
 * button slot is a notification_action_button added to that row.
 *
 * Supported config keys
 * ─────────────────────
 * Core
 *   channelId            String   Notification channel ID   (default: "sticky_notification_channel")
 *   channelName          String   Channel display name in system settings
 *   channelDescription   String   Channel description in system settings
 *   notificationId       Int      Notification ID            (default: 1337)
 *   title                String   Bold title line
 *   text                 String   Body text
 *   subText              String   Smaller sub-text below the body (hidden if empty)
 *
 * Icons & colour
 *   smallIcon            String   Drawable resource name for the status-bar icon
 *   largeIcon            String   Drawable resource name decoded as a large Bitmap
 *   color                String   Hex accent colour, e.g. "#FF5722"
 *
 * Behaviour
 *   priority             String   "min"|"low"|"default"|"high"|"max"
 *   ongoing              Boolean  Prevent swipe-dismiss   (default: true)
 *   autoCancel           Boolean  Dismiss on tap          (default: false)
 *
 * Button layout
 *   buttonsPerRow        Int      Action buttons per row  (default: 5)
 *   maxButtons           Int      Cap on total buttons shown; 0 or absent = no cap
 *
 * Actions  (ArrayList<Bundle> keyed "actions"; each Bundle)
 *   id                   String   Unique action identifier returned to JS
 *   title                String   Button label
 *   icon                 String   Drawable resource name for the button icon
 *   payload              String   Arbitrary string forwarded with the action event
 */
object StickyNotificationHelper {

  private const val DEFAULT_CHANNEL_ID = "sticky_notification_channel"
  private const val DEFAULT_CHANNEL_NAME = "Sticky Notification"
  private const val DEFAULT_BUTTONS_PER_ROW = 5

  // ─── Public API ────────────────────────────────────────────────────────

  fun buildNotification(context: Context, config: Bundle): Notification {
    val channelId = config.getString("channelId") ?: DEFAULT_CHANNEL_ID
    val channelName = config.getString("channelName") ?: DEFAULT_CHANNEL_NAME
    ensureChannelExists(context, channelId, channelName, config)

    val title = config.getString("title") ?: ""
    val text = config.getString("text") ?: ""
    val subText = config.getString("subText")
    val smallIconName = config.getString("smallIcon")
    val largeIconName = config.getString("largeIcon")
    val colorHex = config.getString("color")
    val priorityStr = config.getString("priority")
    val ongoing = if (config.containsKey("ongoing")) config.getBoolean("ongoing") else true
    val autoCancel = if (config.containsKey("autoCancel")) config.getBoolean("autoCancel") else false
    val repostOnDismiss = if (config.containsKey("repostOnDismiss")) config.getBoolean("repostOnDismiss") else true

    // Button layout params
    val buttonsPerRow = config.getInt("buttonsPerRow", DEFAULT_BUTTONS_PER_ROW)
      .coerceAtLeast(1)
    val maxButtons = config.getInt("maxButtons", 0)  // 0 = no cap

    @Suppress("UNCHECKED_CAST")
    val actionsBundle: ArrayList<Bundle>? = config.getParcelableArrayList("actions")

    val smallIconRes = resolveDrawableResource(context, smallIconName)
      ?: context.applicationInfo.icon

    val bigView = buildBigContentView(
      context, title, text, subText, actionsBundle, buttonsPerRow, maxButtons
    )

    val builder = NotificationCompat.Builder(context, channelId)
      .setContentTitle(title)
      .setContentText(text)
      .setSmallIcon(smallIconRes)
      .setOngoing(ongoing)
      .setAutoCancel(autoCancel)
      .setPriority(parsePriority(priorityStr))
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      // Custom big-content view shows all action buttons when expanded.
      // DecoratedCustomViewStyle keeps the system notification header
      // (app icon, app name, timestamp) above our custom content.
      .setCustomBigContentView(bigView)
      .setStyle(NotificationCompat.DecoratedCustomViewStyle())
      .setContentIntent(buildLaunchPendingIntent(context))
      // Android 14+ allows users to swipe away foreground service notifications
      // even when ongoing=true.  When repostOnDismiss is enabled the deleteIntent
      // immediately re-posts the notification by sending ACTION_REPOST to the service.
      .apply { if (repostOnDismiss) setDeleteIntent(buildRepostPendingIntent(context)) }

    subText?.let { builder.setSubText(it) }
    colorHex?.let { hex -> runCatching { builder.setColor(Color.parseColor(hex)) } }
    largeIconName?.let { name ->
      loadBitmapFromDrawable(context, name)?.let { bmp -> builder.setLargeIcon(bmp) }
    }

    return builder.build()
  }

  // ─── RemoteViews builder ───────────────────────────────────────────────

  private fun buildBigContentView(
    context: Context,
    title: String,
    text: String,
    subText: String?,
    actions: ArrayList<Bundle>?,
    buttonsPerRow: Int,
    maxButtons: Int,
  ): RemoteViews {
    val pkg = context.packageName
    val views = RemoteViews(pkg, R.layout.notification_panel)

    // ── Header ────────────────────────────────────────────────────────────
    views.setTextViewText(R.id.notification_title, title)
    views.setTextViewText(R.id.notification_text, text)
    if (!subText.isNullOrEmpty()) {
      views.setViewVisibility(R.id.notification_subtext, View.VISIBLE)
      views.setTextViewText(R.id.notification_subtext, subText)
    } else {
      views.setViewVisibility(R.id.notification_subtext, View.GONE)
    }

    // ── Action buttons ────────────────────────────────────────────────────
    if (actions.isNullOrEmpty()) return views

    // Apply the optional cap
    val visibleActions = if (maxButtons > 0) actions.take(maxButtons) else actions

    // Split actions into rows of buttonsPerRow, then add each row via addView()
    visibleActions.chunked(buttonsPerRow).forEach { rowActions ->
      val rowView = RemoteViews(pkg, R.layout.notification_action_row)

      rowActions.forEach rowLoop@{ actionBundle ->
        val actionId = actionBundle.getString("id") ?: return@rowLoop
        val actionTitle = actionBundle.getString("title") ?: actionId
        val iconName = actionBundle.getString("icon")
        val payload = actionBundle.getString("payload")

        val buttonView = RemoteViews(pkg, R.layout.notification_action_button)
        buttonView.setTextViewText(R.id.action_text, actionTitle)

        val iconRes = resolveDrawableResource(context, iconName)
        if (iconRes != null) {
          buttonView.setViewVisibility(R.id.action_icon, View.VISIBLE)
          buttonView.setImageViewResource(R.id.action_icon, iconRes)
        } else {
          buttonView.setViewVisibility(R.id.action_icon, View.GONE)
        }

        val pendingIntent = buildActionPendingIntent(context, actionId, payload)
        buttonView.setOnClickPendingIntent(R.id.action_button, pendingIntent)

        // Append this button to the current row
        rowView.addView(R.id.action_row, buttonView)
      }

      // Append the completed row to the main container
      views.addView(R.id.buttons_container, rowView)
    }

    return views
  }

  // ─── Channel ──────────────────────────────────────────────────────────

  fun ensureChannelExists(
    context: Context,
    channelId: String,
    channelName: String,
    config: Bundle,
  ) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val importance = parseImportance(config.getString("priority"))
    val channel = NotificationChannel(channelId, channelName, importance).apply {
      config.getString("channelDescription")?.let { description = it }
      setSound(null, null)
      enableVibration(false)
    }
    getNotificationManager(context).createNotificationChannel(channel)
  }

  fun getNotificationManager(context: Context): NotificationManager =
    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

  // ─── PendingIntents ───────────────────────────────────────────────────

  private fun buildLaunchPendingIntent(context: Context): PendingIntent {
    val launchIntent = context.packageManager
      .getLaunchIntentForPackage(context.packageName)
      ?.apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP }
      ?: Intent()
    return PendingIntent.getActivity(context, 0, launchIntent, pendingIntentFlags())
  }

  private fun buildRepostPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, StickyNotificationService::class.java).apply {
      action = StickyNotificationService.ACTION_REPOST
    }
    return PendingIntent.getService(context, 0, intent, pendingIntentFlags())
  }

  private fun buildActionPendingIntent(
    context: Context,
    actionId: String,
    payload: String?,
  ): PendingIntent {
    val intent = Intent(context, StickyNotificationReceiver::class.java).apply {
      action = StickyNotificationReceiver.ACTION_NOTIFICATION_ACTION
      putExtra(StickyNotificationReceiver.EXTRA_ACTION_ID, actionId)
      payload?.let { putExtra(StickyNotificationReceiver.EXTRA_PAYLOAD, it) }
    }
    return PendingIntent.getBroadcast(
      context,
      actionId.hashCode(),
      intent,
      pendingIntentFlags()
    )
  }

  // ─── Helpers ─────────────────────────────────────────────────────────

  private fun resolveDrawableResource(context: Context, name: String?): Int? {
    if (name.isNullOrBlank()) return null
    val res = context.resources.getIdentifier(name, "drawable", context.packageName)
    return if (res != 0) res else null
  }

  private fun loadBitmapFromDrawable(context: Context, name: String): Bitmap? {
    val res = resolveDrawableResource(context, name) ?: return null
    return runCatching { BitmapFactory.decodeResource(context.resources, res) }.getOrNull()
  }

  private fun parsePriority(priority: String?): Int = when (priority) {
    "min" -> NotificationCompat.PRIORITY_MIN
    "low" -> NotificationCompat.PRIORITY_LOW
    "high" -> NotificationCompat.PRIORITY_HIGH
    "max" -> NotificationCompat.PRIORITY_MAX
    else -> NotificationCompat.PRIORITY_DEFAULT
  }

  private fun parseImportance(priority: String?): Int {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return 0
    return when (priority) {
      "min" -> NotificationManager.IMPORTANCE_MIN
      "low" -> NotificationManager.IMPORTANCE_LOW
      "high" -> NotificationManager.IMPORTANCE_HIGH
      "max" -> NotificationManager.IMPORTANCE_MAX
      else -> NotificationManager.IMPORTANCE_DEFAULT
    }
  }

  private fun pendingIntentFlags(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }
}
