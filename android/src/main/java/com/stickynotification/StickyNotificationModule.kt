package com.stickynotification

import android.content.Context
import android.content.Intent
import android.os.Build
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.core.DeviceEventManagerModule

@ReactModule(name = StickyNotificationModule.NAME)
class StickyNotificationModule(reactContext: ReactApplicationContext) :
  NativeStickyNotificationSpec(reactContext), LifecycleEventListener {

  // Queue for events received before the JS layer resumes
  private val pendingQueue = mutableListOf<Pair<String, String?>>()
  private var isResumed = false

  init {
    currentInstance = this
    reactContext.addLifecycleEventListener(this)
  }

  override fun getName() = NAME

  // ─── Service control ──────────────────────────────────────────────────────

  override fun startService(config: ReadableMap, promise: Promise) {
    try {
      val bundle = Arguments.toBundle(config)
      val intent = Intent(reactApplicationContext, StickyNotificationService::class.java).apply {
        action = StickyNotificationService.ACTION_START
        putExtra(StickyNotificationService.EXTRA_CONFIG, bundle)
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        reactApplicationContext.startForegroundService(intent)
      } else {
        reactApplicationContext.startService(intent)
      }
      promise.resolve(null)
    } catch (e: Exception) {
      promise.reject("START_SERVICE_ERROR", e.message, e)
    }
  }

  override fun stopService(promise: Promise) {
    try {
      reactApplicationContext.stopService(
        Intent(reactApplicationContext, StickyNotificationService::class.java)
      )
      promise.resolve(null)
    } catch (e: Exception) {
      promise.reject("STOP_SERVICE_ERROR", e.message, e)
    }
  }

  override fun updateNotification(config: ReadableMap, promise: Promise) {
    try {
      val bundle = Arguments.toBundle(config)
      val intent = Intent(reactApplicationContext, StickyNotificationService::class.java).apply {
        action = StickyNotificationService.ACTION_UPDATE
        putExtra(StickyNotificationService.EXTRA_CONFIG, bundle)
      }
      reactApplicationContext.startService(intent)
      promise.resolve(null)
    } catch (e: Exception) {
      promise.reject("UPDATE_ERROR", e.message, e)
    }
  }

  override fun isServiceRunning(promise: Promise) {
    promise.resolve(StickyNotificationService.isRunning)
  }

  // ─── NativeEventEmitter stubs ─────────────────────────────────────────────

  override fun addListener(eventName: String) {}

  override fun removeListeners(count: Double) {}

  // ─── Event delivery ───────────────────────────────────────────────────────

  /**
   * Called by StickyNotificationReceiver when an action button is tapped.
   * Returns true if the event was or will be delivered; false if the module
   * is not initialised (app process not running).
   */
  private fun handleActionPressed(actionId: String, payload: String?) {
    if (isResumed) {
      emitActionEvent(actionId, payload)
    } else {
      synchronized(pendingQueue) { pendingQueue.add(Pair(actionId, payload)) }
    }
  }

  private fun emitActionEvent(actionId: String, payload: String?) {
    val params = Arguments.createMap().apply {
      putString("actionId", actionId)
      payload?.let { putString("payload", it) }
    }
    reactApplicationContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit(EVENT_ACTION_PRESS, params)
  }

  // ─── Lifecycle ────────────────────────────────────────────────────────────

  override fun onHostResume() {
    isResumed = true

    // Deliver events saved when the app was killed (written by the Receiver)
    val prefs = reactApplicationContext
      .getSharedPreferences(StickyNotificationReceiver.PREFS_NAME, Context.MODE_PRIVATE)
    val pendingAction = prefs.getString(StickyNotificationReceiver.KEY_PENDING_ACTION, null)
    if (pendingAction != null) {
      val pendingPayload = prefs.getString(StickyNotificationReceiver.KEY_PENDING_PAYLOAD, null)
      prefs.edit()
        .remove(StickyNotificationReceiver.KEY_PENDING_ACTION)
        .remove(StickyNotificationReceiver.KEY_PENDING_PAYLOAD)
        .apply()
      emitActionEvent(pendingAction, pendingPayload)
    }

    // Flush events that arrived while JS was not yet ready
    val queued: List<Pair<String, String?>>
    synchronized(pendingQueue) {
      queued = pendingQueue.toList()
      pendingQueue.clear()
    }
    queued.forEach { (id, pl) -> emitActionEvent(id, pl) }
  }

  override fun onHostPause() {
    isResumed = false
  }

  override fun onHostDestroy() {
    isResumed = false
  }

  override fun invalidate() {
    if (currentInstance === this) currentInstance = null
    reactApplicationContext.removeLifecycleEventListener(this)
    super.invalidate()
  }

  companion object {
    const val NAME = "StickyNotification"
    const val EVENT_ACTION_PRESS = "StickyNotification_onActionPress"

    @Volatile
    private var currentInstance: StickyNotificationModule? = null

    /**
     * Called from StickyNotificationReceiver (same process) to route an
     * action-press event to the JS layer.
     *
     * @return true when the module instance was available; false when the app
     *   process was not running (caller should persist the event instead).
     */
    fun notifyActionPressed(actionId: String, payload: String?): Boolean {
      val instance = currentInstance ?: return false
      instance.handleActionPressed(actionId, payload)
      return true
    }
  }
}
