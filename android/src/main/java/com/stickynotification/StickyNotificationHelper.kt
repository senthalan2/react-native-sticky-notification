package com.stickynotification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat

/**
 * Stateless helper that converts a config Bundle into an Android Notification.
 *
 * Supported config keys
 * ─────────────────────
 * Core
 *   channelId                String   Channel ID            (default: "sticky_notification_channel")
 *   channelName              String   Channel display name
 *   channelDescription       String   Channel description
 *   notificationId           Int      Notification ID       (default: 1337)
 *   title                    String   Bold title line
 *   text                     String   Body text             (hidden when absent / empty)
 *   subText                  String   Sub-text              (hidden when absent / empty)
 *
 * Icons & colour
 *   smallIcon                String   Drawable name for status-bar icon
 *   largeIcon                String   Drawable name decoded as a large Bitmap
 *   color                    String   Hex accent colour, e.g. "#FF5722"
 *
 * Behaviour
 *   priority                 String   "min"|"low"|"default"|"high"|"max"
 *   ongoing                  Boolean  Prevent swipe-dismiss (default: true)
 *   autoCancel               Boolean  Dismiss on tap        (default: false)
 *   repostOnDismiss          Boolean  Re-show after swipe on Android 14+ (default: true)
 *   openAppOnAction          Boolean  Open app on button tap (default: false)
 *   closeOnAction            Boolean  Collapse panel on button tap (default: false)
 *
 * Button layout
 *   buttonsPerRow            Int      Buttons per row       (default: 5)
 *   maxButtons               Int      Cap on total buttons; 0 = no cap
 *
 * Styling
 *   showDivider              Boolean  Show/hide the line between text and buttons (default: true)
 *   dividerColor             String   Hex colour for the divider
 *   titleColor               String   Hex colour for the title text
 *   textColor                String   Hex colour for the body text
 *   subTextColor             String   Hex colour for the sub-text
 *   actionLabelColor         String   Hex colour for all action button labels
 *   actionIconTint           String   Hex tint applied to all action button icons
 *   actionBackground         String   Hex background colour for each action button
 *   actionsContainerBackground  String   Hex background for the whole button container
 *
 * Actions  (ArrayList<Bundle> keyed "actions"; each Bundle)
 *   id                       String   Unique action identifier returned to JS
 *   title                    String   Button label
 *   icon                     String   Drawable resource name
 *   payload                  String   Arbitrary string forwarded with the event
 */
object StickyNotificationHelper {

  private const val DEFAULT_CHANNEL_ID = "sticky_notification_channel"
  private const val DEFAULT_CHANNEL_NAME = "Sticky Notification"
  private const val DEFAULT_BUTTONS_PER_ROW = 5

  // ─── Styling holder ────────────────────────────────────────────────────

  private data class Style(
    val showDivider: Boolean,
    val dividerColor: Int?,
    val titleColor: Int?,
    val textColor: Int?,
    val subTextColor: Int?,
    val actionLabelColor: Int?,
    val actionIconTint: Int?,
    val actionBackground: Int?,
    val actionBorderRadius: Float,        // dp — applied to all buttons
    val actionsContainerBackground: Int?,
  )

  // ─── Public API ────────────────────────────────────────────────────────

  fun buildNotification(context: Context, config: Bundle): Notification {
    val channelId = config.getString("channelId") ?: DEFAULT_CHANNEL_ID
    val channelName = config.getString("channelName") ?: DEFAULT_CHANNEL_NAME
    ensureChannelExists(context, channelId, channelName, config)

    val title = config.getString("title") ?: ""
    val text = config.getString("text")
    val subText = config.getString("subText")
    val smallIconName = config.getString("smallIcon")
    val largeIconName = config.getString("largeIcon")
    val colorHex = config.getString("color")
    val priorityStr = config.getString("priority")
    val ongoing = if (config.containsKey("ongoing")) config.getBoolean("ongoing") else true
    val autoCancel = if (config.containsKey("autoCancel")) config.getBoolean("autoCancel") else false
    val repostOnDismiss = if (config.containsKey("repostOnDismiss")) config.getBoolean("repostOnDismiss") else true
    val openAppOnAction = if (config.containsKey("openAppOnAction")) config.getBoolean("openAppOnAction") else false
    val closeOnAction = if (config.containsKey("closeOnAction")) config.getBoolean("closeOnAction") else false

    val buttonsPerRow = config.getInt("buttonsPerRow", DEFAULT_BUTTONS_PER_ROW).coerceAtLeast(1)
    val maxButtons = config.getInt("maxButtons", 0)

    val style = Style(
      showDivider = if (config.containsKey("showDivider")) config.getBoolean("showDivider") else true,
      dividerColor = parseColor(config.getString("dividerColor")),
      titleColor = parseColor(config.getString("titleColor")),
      textColor = parseColor(config.getString("textColor")),
      subTextColor = parseColor(config.getString("subTextColor")),
      actionLabelColor = parseColor(config.getString("actionLabelColor")),
      actionIconTint = parseColor(config.getString("actionIconTint")),
      actionBackground = parseColor(config.getString("actionBackground")),
      actionBorderRadius = config.getFloat("actionBorderRadius", 0f).coerceAtLeast(0f),
      actionsContainerBackground = parseColor(config.getString("actionsContainerBackground")),
    )

    @Suppress("UNCHECKED_CAST")
    val actionsBundle: ArrayList<Bundle>? = config.getParcelableArrayList("actions")

    val smallIconRes = resolveDrawableResource(context, smallIconName)
      ?: context.applicationInfo.icon

    val bigView = buildBigContentView(
      context, title, text, subText, actionsBundle,
      buttonsPerRow, maxButtons, openAppOnAction, closeOnAction, style
    )

    val builder = NotificationCompat.Builder(context, channelId)
      .setContentTitle(title)
      .setSmallIcon(smallIconRes)
      .setOngoing(ongoing)
      .setAutoCancel(autoCancel)
      .setPriority(parsePriority(priorityStr))
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setCustomBigContentView(bigView)
      .setStyle(NotificationCompat.DecoratedCustomViewStyle())
      .setContentIntent(buildLaunchPendingIntent(context))
      .apply { if (repostOnDismiss) setDeleteIntent(buildRepostPendingIntent(context)) }

    if (!text.isNullOrEmpty()) builder.setContentText(text)
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
    text: String?,
    subText: String?,
    actions: ArrayList<Bundle>?,
    buttonsPerRow: Int,
    maxButtons: Int,
    openAppOnAction: Boolean,
    closeOnAction: Boolean,
    style: Style,
  ): RemoteViews {
    val pkg = context.packageName
    val views = RemoteViews(pkg, R.layout.notification_panel)

    // ── Title ─────────────────────────────────────────────────────────────
    views.setTextViewText(R.id.notification_title, title)
    style.titleColor?.let { views.setTextColor(R.id.notification_title, it) }

    // ── Body text (hidden when null or empty) ─────────────────────────────
    if (!text.isNullOrEmpty()) {
      views.setViewVisibility(R.id.notification_text, View.VISIBLE)
      views.setTextViewText(R.id.notification_text, text)
      style.textColor?.let { views.setTextColor(R.id.notification_text, it) }
    } else {
      views.setViewVisibility(R.id.notification_text, View.GONE)
    }

    // ── Sub-text (hidden when null or empty) ──────────────────────────────
    if (!subText.isNullOrEmpty()) {
      views.setViewVisibility(R.id.notification_subtext, View.VISIBLE)
      views.setTextViewText(R.id.notification_subtext, subText)
      style.subTextColor?.let { views.setTextColor(R.id.notification_subtext, it) }
    } else {
      views.setViewVisibility(R.id.notification_subtext, View.GONE)
    }

    // ── Divider ───────────────────────────────────────────────────────────
    if (style.showDivider) {
      views.setViewVisibility(R.id.notification_divider, View.VISIBLE)
      style.dividerColor?.let {
        views.setInt(R.id.notification_divider, "setBackgroundColor", it)
      }
    } else {
      views.setViewVisibility(R.id.notification_divider, View.GONE)
    }

    // ── Action buttons ────────────────────────────────────────────────────
    if (actions.isNullOrEmpty()) return views

    style.actionsContainerBackground?.let {
      views.setInt(R.id.buttons_container, "setBackgroundColor", it)
    }

    val visibleActions = if (maxButtons > 0) actions.take(maxButtons) else actions

    visibleActions.chunked(buttonsPerRow).forEach { rowActions ->
      val rowView = RemoteViews(pkg, R.layout.notification_action_row)

      rowActions.forEach rowLoop@{ actionBundle ->
        val actionId = actionBundle.getString("id") ?: return@rowLoop
        val actionTitle = actionBundle.getString("title") ?: actionId
        val iconName = actionBundle.getString("icon")
        val payload = actionBundle.getString("payload")

        // Per-button values fall back to global style when absent.
        val btnLabelColor  = parseColor(actionBundle.getString("labelColor"))  ?: style.actionLabelColor
        val btnIconTint    = parseColor(actionBundle.getString("iconTint"))     ?: style.actionIconTint
        val btnBackground  = parseColor(actionBundle.getString("background"))   ?: style.actionBackground
        val btnRadius      = actionBundle.getFloat("borderRadius", -1f)
          .let { if (it < 0f) style.actionBorderRadius else it }

        val buttonView = RemoteViews(pkg, R.layout.notification_action_button)

        // ── Background (flat colour or rounded-rect bitmap) ───────────────
        if (btnBackground != null || btnRadius > 0f) {
          val density = context.resources.displayMetrics.density
          // Use the fixed row height (52dp) for the bitmap height; use a wide
          // fixed width (200dp) that fitXY will stretch to the real button size.
          // Corner accuracy is best when the bitmap aspect ratio is close to the
          // rendered button's, which holds for typical buttonsPerRow values.
          val bitmapH = (52 * density).toInt().coerceAtLeast(1)
          val bitmapW = (200 * density).toInt().coerceAtLeast(1)
          val radiusPx = btnRadius * density
          val bgBitmap = buildRoundedBitmap(
            color = btnBackground ?: Color.TRANSPARENT,
            cornerRadius = radiusPx,
            width = bitmapW,
            height = bitmapH,
          )
          buttonView.setViewVisibility(R.id.action_bg, View.VISIBLE)
          buttonView.setImageViewBitmap(R.id.action_bg, bgBitmap)
        } else {
          buttonView.setViewVisibility(R.id.action_bg, View.GONE)
        }

        // ── Label ─────────────────────────────────────────────────────────
        buttonView.setTextViewText(R.id.action_text, actionTitle)
        btnLabelColor?.let { buttonView.setTextColor(R.id.action_text, it) }

        // ── Icon ──────────────────────────────────────────────────────────
        val iconRes = resolveDrawableResource(context, iconName)
        if (iconRes != null) {
          buttonView.setViewVisibility(R.id.action_icon, View.VISIBLE)
          buttonView.setImageViewResource(R.id.action_icon, iconRes)
          btnIconTint?.let { buttonView.setInt(R.id.action_icon, "setColorFilter", it) }
        } else {
          buttonView.setViewVisibility(R.id.action_icon, View.GONE)
        }

        val pendingIntent = if (closeOnAction || openAppOnAction) {
          buildTrampolinePendingIntent(context, actionId, payload, openAppOnAction)
        } else {
          buildActionPendingIntent(context, actionId, payload)
        }
        buttonView.setOnClickPendingIntent(R.id.action_button, pendingIntent)

        rowView.addView(R.id.action_row, buttonView)
      }

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

  private fun buildTrampolinePendingIntent(
    context: Context,
    actionId: String,
    payload: String?,
    openApp: Boolean,
  ): PendingIntent {
    val intent = Intent(context, StickyNotificationTrampolineActivity::class.java).apply {
      putExtra(StickyNotificationTrampolineActivity.EXTRA_ACTION_ID, actionId)
      payload?.let { putExtra(StickyNotificationTrampolineActivity.EXTRA_PAYLOAD, it) }
      putExtra(StickyNotificationTrampolineActivity.EXTRA_OPEN_APP, openApp)
      addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    return PendingIntent.getActivity(context, actionId.hashCode(), intent, pendingIntentFlags())
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
    return PendingIntent.getBroadcast(context, actionId.hashCode(), intent, pendingIntentFlags())
  }

  // ─── Helpers ─────────────────────────────────────────────────────────

  /**
   * Renders a rounded rectangle into a Bitmap.
   * Used as a RemoteViews background because Drawable objects cannot be passed
   * directly to a RemoteViews view — only Bitmaps can via setImageViewBitmap().
   *
   * The bitmap is drawn at [width]×[height] pixels with [cornerRadius] px
   * corner radius.  A value of half the height produces a pill shape.
   */
  private fun buildRoundedBitmap(color: Int, cornerRadius: Float, width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    val clampedRadius = cornerRadius.coerceAtMost(minOf(width, height) / 2f)
    canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), clampedRadius, clampedRadius, paint)
    return bitmap
  }

  private fun parseColor(hex: String?): Int? {
    if (hex.isNullOrBlank()) return null
    return runCatching { Color.parseColor(hex) }.getOrNull()
  }

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
