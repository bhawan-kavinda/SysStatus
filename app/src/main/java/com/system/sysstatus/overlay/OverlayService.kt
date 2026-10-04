package com.system.sysstatus.overlay

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import android.view.animation.DecelerateInterpolator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.system.sysstatus.MainActivity
import com.system.sysstatus.R
import com.system.sysstatus.data.BatteryCollector
import com.system.sysstatus.data.CpuCollector
import com.system.sysstatus.data.MemoryCollector
import com.system.sysstatus.ui.SysThemeBare
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

// Draws the floating icon and the mini panel over other apps
class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var pm: PowerManager
    private val owner = ServiceOwner()
    private val main = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val state = mutableStateOf(FloatState())
    private var faded by mutableStateOf(false)
    private var iconIdx by mutableIntStateOf(0)
    private var userAlpha by mutableFloatStateOf(1f)

    // Applies the Settings screen changes to the running icon (kept as a field: prefs hold it weakly)
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        main.post { onPrefChanged(key) }
    }

    private var bubble: View? = null
    @Volatile private var panel: ComposeView? = null
    private lateinit var bubbleLp: WindowManager.LayoutParams
    private var snapAnim: ValueAnimator? = null
    private var started = false
    private val fade = Runnable { faded = true }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        pm = getSystemService(POWER_SERVICE) as PowerManager
        owner.create()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            FloatingPrefs.setEnabled(this, false)
            stopSelf()
            return START_NOT_STICKY
        }
        // Must be called right after startForegroundService()
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification(),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )
        if (!Settings.canDrawOverlays(this)) {
            FloatingPrefs.setEnabled(this, false)
            stopSelf()
            return START_NOT_STICKY
        }
        if (!started) {
            started = true
            running = true
            owner.start()
            addBubble()
            startPolling()
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        closePanel()
        if (started) snapToEdge(animate = false)
    }

    override fun onDestroy() {
        running = false
        scope.cancel()
        main.removeCallbacksAndMessages(null)
        snapAnim?.cancel()
        closePanel()
        FloatingPrefs.unregister(this, prefListener)
        bubble?.let { runCatching { wm.removeView(it) } }
        bubble = null
        owner.destroy()
        super.onDestroy()
    }

    // ---------- bubble ----------

    private fun addBubble() {
        val metrics = resources.displayMetrics
        val size = dp(FloatingPrefs.size(this))
        iconIdx = FloatingPrefs.icon(this)
        userAlpha = FloatingPrefs.alpha(this)
        val right = FloatingPrefs.savedRight(this)
        bubbleLp = overlayParams(size, size).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (FloatingPrefs.snap(this@OverlayService)) {
                if (right) metrics.widthPixels - size else 0
            } else {
                (FloatingPrefs.savedX(this@OverlayService) * (metrics.widthPixels - size)).toInt()
                    .coerceIn(0, metrics.widthPixels - size)
            }
            y = (FloatingPrefs.savedY(this@OverlayService) * metrics.heightPixels).toInt()
                .coerceIn(0, metrics.heightPixels - size)
        }
        val view = DragView(
            onTap = { wake(); togglePanel() },
            onDragStart = { closePanel(); wake() },
            onDrag = ::moveBubble,
            onDragEnd = { snapToEdge(animate = true) }
        )
        // ComposeView is final, so it sits inside a FrameLayout that handles the touches
        val compose = ComposeView(this)
        compose.setContent { SysThemeBare { FloatingBubble(state.value, faded, iconIdx, userAlpha) } }
        view.bindOwner()
        view.addView(compose, FrameLayout.LayoutParams(MATCH, MATCH))
        wm.addView(view, bubbleLp)
        bubble = view
        FloatingPrefs.register(this, prefListener)
        scheduleFade()
    }

    private fun onPrefChanged(key: String?) {
        if (!started || bubble == null) return
        when (key) {
            FloatingPrefs.KEY_ICON -> iconIdx = FloatingPrefs.icon(this)
            FloatingPrefs.KEY_ALPHA -> userAlpha = FloatingPrefs.alpha(this)
            FloatingPrefs.KEY_SNAP -> if (FloatingPrefs.snap(this)) snapToEdge(animate = true)
            FloatingPrefs.KEY_SIZE -> {
                closePanel()
                val size = dp(FloatingPrefs.size(this))
                bubbleLp.width = size
                bubbleLp.height = size
                snapToEdge(animate = false)
            }
        }
    }

    private fun moveBubble(dx: Int, dy: Int) {
        val m = resources.displayMetrics
        val size = bubbleLp.width
        bubbleLp.x = (bubbleLp.x + dx).coerceIn(0, m.widthPixels - size)
        bubbleLp.y = (bubbleLp.y + dy).coerceIn(0, m.heightPixels - size)
        bubble?.let { wm.updateViewLayout(it, bubbleLp) }
    }

    private fun snapToEdge(animate: Boolean) {
        val m = resources.displayMetrics
        val size = bubbleLp.width
        val maxX = (m.widthPixels - size).coerceAtLeast(1)
        val snap = FloatingPrefs.snap(this)
        val right = bubbleLp.x + size / 2 > m.widthPixels / 2
        // With snap off the icon stays where it was dropped
        val targetX = if (snap) (if (right) m.widthPixels - size else 0) else bubbleLp.x.coerceIn(0, maxX)
        bubbleLp.y = bubbleLp.y.coerceIn(0, m.heightPixels - size)
        FloatingPrefs.savePosition(this, right, bubbleLp.y.toFloat() / m.heightPixels, targetX.toFloat() / maxX)

        snapAnim?.cancel()
        if (!animate || !snap) {
            bubbleLp.x = targetX
            bubble?.let { wm.updateViewLayout(it, bubbleLp) }
        } else {
            snapAnim = ValueAnimator.ofInt(bubbleLp.x, targetX).apply {
                duration = 220
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    bubbleLp.x = it.animatedValue as Int
                    bubble?.let { v -> if (v.isAttachedToWindow) wm.updateViewLayout(v, bubbleLp) }
                }
                start()
            }
        }
        scheduleFade()
    }

    private fun wake() {
        faded = false
        scheduleFade()
    }

    private fun scheduleFade() {
        main.removeCallbacks(fade)
        main.postDelayed(fade, 2_600)
    }

    // ---------- panel ----------

    private fun togglePanel() { if (panel != null) closePanel() else openPanel() }

    private fun openPanel() {
        val m = resources.displayMetrics
        val size = bubbleLp.width
        val gap = dp(10)
        val lp = overlayParams(min(dp(320), m.widthPixels - dp(24)), WindowManager.LayoutParams.WRAP_CONTENT)
        // Open below the icon when it is in the top half, above it otherwise
        if (bubbleLp.y + size / 2 < m.heightPixels / 2) {
            lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            lp.y = bubbleLp.y + size + gap
        } else {
            lp.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            lp.y = m.heightPixels - bubbleLp.y + gap
        }
        val view = ComposeView(this)
        view.bindOwner()
        view.setContent {
            SysThemeBare {
                FloatingPanel(
                    state = state.value,
                    onMinimize = ::closePanel,
                    onOpenApp = { closePanel(); openApp() },
                    onTurnOff = { FloatingPrefs.setEnabled(this, false); stopSelf() }
                )
            }
        }
        wm.addView(view, lp)
        panel = view
    }

    private fun closePanel() {
        panel?.let { runCatching { wm.removeView(it) } }
        panel = null
    }

    private fun openApp() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
    }

    // ---------- data ----------

    private fun startPolling() {
        val battery = BatteryCollector(this)
        val memory = MemoryCollector(this)
        val cpu = CpuCollector(this)
        val power = ArrayDeque<Float>()
        val ram = ArrayDeque<Float>()
        val load = ArrayDeque<Float>()

        scope.launch(Dispatchers.IO) {
            while (isActive) {
                // Nothing is read while the screen is off
                if (pm.isInteractive) {
                    val b = battery.read()
                    val m = memory.read()
                    val c = cpu.read()
                    if (b.available && b.currentAvailable) power.push(b.powerW)
                    if (m.totalMb > 0) ram.push(m.usedPercent)
                    if (c.loadAvailable) load.push(c.totalLoad)
                    state.value = FloatState(true, b, m, c, power.toList(), ram.toList(), load.toList())
                }
                delay(if (panel != null) 2_000L else 4_000L)
            }
        }
    }

    private fun ArrayDeque<Float>.push(v: Float) {
        addLast(v)
        if (size > HISTORY) removeFirst()
    }

    // ---------- plumbing ----------

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()

    private fun overlayParams(w: Int, h: Int) = WindowManager.LayoutParams(
        w, h,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    )

    private fun View.bindOwner() {
        setViewTreeLifecycleOwner(owner)
        setViewTreeViewModelStoreOwner(owner)
        setViewTreeSavedStateRegistryOwner(owner)
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Floating window", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, OverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Floating window is on")
            .setContentText("Tap to open System Status")
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(
                Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_notification), "Turn off", stop).build()
            )
            .build()
    }

    // Raw-coordinate drag so the icon follows the finger without jitter while its window moves
    private inner class DragView(
        private val onTap: () -> Unit,
        private val onDragStart: () -> Unit,
        private val onDrag: (Int, Int) -> Unit,
        private val onDragEnd: () -> Unit
    ) : FrameLayout(this@OverlayService) {
        private val slop = ViewConfiguration.get(context).scaledTouchSlop
        private var downX = 0f
        private var downY = 0f
        private var lastX = 0f
        private var lastY = 0f
        private var dragging = false

        override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.rawX; downY = ev.rawY
                    lastX = downX; lastY = downY
                    dragging = false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!dragging && (abs(ev.rawX - downX) > slop || abs(ev.rawY - downY) > slop)) {
                        dragging = true
                        onDragStart()
                    }
                    if (dragging) {
                        onDrag((ev.rawX - lastX).roundToInt(), (ev.rawY - lastY).roundToInt())
                        lastX = ev.rawX; lastY = ev.rawY
                    }
                }
                MotionEvent.ACTION_UP -> if (dragging) onDragEnd() else onTap()
                MotionEvent.ACTION_CANCEL -> if (dragging) onDragEnd()
            }
            return true
        }
    }

    // Lifecycle, saved state and view-model owner that Compose needs outside an Activity
    private class ServiceOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry(this)
        private val controller = SavedStateRegistryController.create(this)
        override val viewModelStore = ViewModelStore()
        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry

        fun create() {
            controller.performRestore(null)
            registry.currentState = Lifecycle.State.CREATED
        }
        fun start() { registry.currentState = Lifecycle.State.RESUMED }
        fun destroy() {
            registry.currentState = Lifecycle.State.DESTROYED
            viewModelStore.clear()
        }
    }

    companion object {
        const val ACTION_STOP = "com.podda.sysstatus.action.STOP_FLOATING"
        private const val CHANNEL_ID = "floating_window"
        private const val NOTIFICATION_ID = 42
        private const val HISTORY = 40
        private const val MATCH = FrameLayout.LayoutParams.MATCH_PARENT

        @Volatile var running = false
            private set

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, OverlayService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }
}
