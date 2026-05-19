package com.stickynotification

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.core.DeviceEventManagerModule

/**
 * TurboModule bridge for react-native-sticky-notification.
 *
 * New Architecture (React Native 0.73+) compatibility notes
 * ──────────────────────────────────────────────────────────
 * 1. onHostResume fires when the Android Activity resumes, which happens
 *    BEFORE the JS bundle has finished loading in the New Architecture.
 *    All event delivery is therefore deferred via Handler.postDelayed and
 *    retried with back-off until the bridge accepts the emission.
 *
 * 2. Every call to the JS DeviceEventEmitter is wrapped in a try/catch.
 *    If the bridge is not yet accepting calls, the event is placed back at
 *    the front of the pending queue and a retry is scheduled.
 *
 * 3. The module holds a weak static reference to itself so the
 *    BroadcastReceiver can route action events into the module without
 *    coupling the two classes beyond a single static method.
 */
@ReactModule(name = StickyNotificationModule.NAME)
class StickyNotificationModule(reactContext: ReactApplicationContext) :
  NativeStickyNotificationSpec(reactContext), LifecycleEventListener {

  // In-memory queue for action-press events that arrived before JS was ready
  private val pendingQueue = mutableListOf<Pair<String, String?>>()
  // In-memory queue for service-stop events
  private val pendingStopReasons = mutableListOf<String>()
  private var isResumed = false
  private var drainRetryCount = 0

  // All Handler operations run on the main thread to keep synchronisation simple.
  private val mainHandler = Handler(Looper.getMainLooper())

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

  override fun canSwipeDismiss(promise: Promise) {
    promise.resolve(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
  }

  // ─── NativeEventEmitter stubs ─────────────────────────────────────────────

  override fun addListener(eventName: String) {}

  override fun removeListeners(count: Double) {}

  // ─── Inbound event routing (called from BroadcastReceiver) ───────────────

  /**
   * Entry point called by StickyNotificationService when the service stops.
   * Reason values: "MANUAL_STOP" | "SWIPE_DISMISS" | "SYSTEM_KILLED".
   */
  private fun handleServiceStopped(reason: String) {
    mainHandler.post {
      if (isResumed) {
        val delivered = tryEmitServiceStop(reason)
        if (!delivered) {
          synchronized(pendingStopReasons) { pendingStopReasons.add(reason) }
          scheduleRetry()
        }
      } else {
        synchronized(pendingStopReasons) { pendingStopReasons.add(reason) }
      }
    }
  }

  /**
   * Entry point called by StickyNotificationReceiver on the main thread.
   * If the JS layer is already live we attempt immediate delivery; otherwise
   * the event is queued and will be flushed in the next drainEvents pass.
   */
  private fun handleActionPressed(actionId: String, payload: String?) {
    mainHandler.post {
      if (isResumed) {
        val delivered = tryEmit(actionId, payload)
        if (!delivered) {
          // Bridge not ready yet — queue and schedule a retry
          synchronized(pendingQueue) { pendingQueue.add(Pair(actionId, payload)) }
          scheduleRetry()
        }
      } else {
        synchronized(pendingQueue) { pendingQueue.add(Pair(actionId, payload)) }
      }
    }
  }

  // ─── Event emission ───────────────────────────────────────────────────────

  /**
   * Attempts a single event emission to the JS layer.
   * Returns true on success, false when the bridge is not yet accepting calls.
   * All exceptions are caught so a not-ready bridge never crashes the process.
   */
  private fun tryEmit(actionId: String, payload: String?): Boolean = try {
    val params = Arguments.createMap().apply {
      putString("actionId", actionId)
      payload?.let { putString("payload", it) }
    }
    reactApplicationContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit(EVENT_ACTION_PRESS, params)
    true
  } catch (_: Exception) {
    false
  }

  private fun tryEmitServiceStop(reason: String): Boolean = try {
    val params = Arguments.createMap().apply { putString("reason", reason) }
    reactApplicationContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit(EVENT_SERVICE_STOP, params)
    true
  } catch (_: Exception) {
    false
  }

  // ─── Pending-event drain with retry ──────────────────────────────────────

  /**
   * Loads any SharedPreferences event written during a killed-state launch,
   * then attempts to deliver every queued event.  Failed deliveries are
   * placed back at the head of the queue and a retry is scheduled.
   *
   * This is called from the main thread only.
   */
  private fun drainEvents() {
    if (!isResumed) return

    // Merge the SharedPreferences entry (written by Receiver when process was dead)
    loadPersistedEvent()

    var anyFailed = false

    // ── Action-press events ───────────────────────────────────────────────
    val actionSnapshot: List<Pair<String, String?>>
    synchronized(pendingQueue) {
      actionSnapshot = pendingQueue.toList()
      pendingQueue.clear()
    }
    val failedActions = actionSnapshot.filterNot { (id, pl) -> tryEmit(id, pl) }
    if (failedActions.isNotEmpty()) {
      synchronized(pendingQueue) { pendingQueue.addAll(0, failedActions) }
      anyFailed = true
    }

    // ── Service-stop events ───────────────────────────────────────────────
    val stopSnapshot: List<String>
    synchronized(pendingStopReasons) {
      stopSnapshot = pendingStopReasons.toList()
      pendingStopReasons.clear()
    }
    val failedStop = stopSnapshot.filterNot { tryEmitServiceStop(it) }
    if (failedStop.isNotEmpty()) {
      synchronized(pendingStopReasons) { pendingStopReasons.addAll(0, failedStop) }
      anyFailed = true
    }

    if (anyFailed) {
      scheduleRetry()
    } else {
      drainRetryCount = 0
    }
  }

  private fun scheduleRetry() {
    if (!isResumed || drainRetryCount >= MAX_DRAIN_RETRIES) {
      drainRetryCount = 0
      return
    }
    drainRetryCount++
    mainHandler.postDelayed(::drainEvents, RETRY_DELAY_MS)
  }

  private fun loadPersistedEvent() {
    val prefs = reactApplicationContext
      .getSharedPreferences(StickyNotificationReceiver.PREFS_NAME, Context.MODE_PRIVATE)
    val actionId = prefs.getString(StickyNotificationReceiver.KEY_PENDING_ACTION, null)
      ?: return
    val payload = prefs.getString(StickyNotificationReceiver.KEY_PENDING_PAYLOAD, null)
    prefs.edit()
      .remove(StickyNotificationReceiver.KEY_PENDING_ACTION)
      .remove(StickyNotificationReceiver.KEY_PENDING_PAYLOAD)
      .apply()
    synchronized(pendingQueue) { pendingQueue.add(Pair(actionId, payload)) }
  }

  // ─── Lifecycle ────────────────────────────────────────────────────────────

  override fun onHostResume() {
    isResumed = true
    drainRetryCount = 0
    // New Architecture: onHostResume fires on Activity.onResume(), which can
    // precede full JS bundle load.  A short initial delay avoids emitting
    // events into a not-yet-ready bridge on cold launch.
    mainHandler.postDelayed(::drainEvents, INITIAL_DRAIN_DELAY_MS)
  }

  override fun onHostPause() {
    isResumed = false
    mainHandler.removeCallbacks(::drainEvents)
    drainRetryCount = 0
  }

  override fun onHostDestroy() {
    isResumed = false
    mainHandler.removeCallbacks(::drainEvents)
    drainRetryCount = 0
  }

  override fun invalidate() {
    if (currentInstance === this) currentInstance = null
    isResumed = false
    mainHandler.removeCallbacks(::drainEvents)
    reactApplicationContext.removeLifecycleEventListener(this)
    super.invalidate()
  }

  companion object {
    const val NAME = "StickyNotification"
    const val EVENT_ACTION_PRESS = "StickyNotification_onActionPress"
    const val EVENT_SERVICE_STOP  = "StickyNotification_onServiceStop"

    /**
     * Delay before first drain on resume.  Gives the New Architecture JS
     * runtime enough time to finish bundle evaluation after Activity.onResume.
     */
    private const val INITIAL_DRAIN_DELAY_MS = 150L

    /**
     * Delay between retry attempts when the bridge rejects an emission.
     */
    private const val RETRY_DELAY_MS = 250L

    /**
     * Maximum number of automatic retry attempts per resume cycle.
     * Prevents an infinite loop if the bridge never becomes ready.
     */
    private const val MAX_DRAIN_RETRIES = 8

    @Volatile
    private var currentInstance: StickyNotificationModule? = null

    /**
     * Called from StickyNotificationReceiver (same process) to route an
     * action-press event into the JS layer.
     *
     * @return true when the module instance was available; false when the app
     *   process was not running (caller should persist to SharedPreferences).
     */
    fun notifyActionPressed(actionId: String, payload: String?): Boolean {
      val instance = currentInstance ?: return false
      instance.handleActionPressed(actionId, payload)
      return true
    }

    /**
     * Called from StickyNotificationService when the service stops for any reason.
     * Reason values: "MANUAL_STOP" | "SWIPE_DISMISS" | "SYSTEM_KILLED".
     *
     * @return true when the module instance was available to accept the event.
     */
    fun notifyServiceStopped(reason: String): Boolean {
      val instance = currentInstance ?: return false
      instance.handleServiceStopped(reason)
      return true
    }
  }
}
