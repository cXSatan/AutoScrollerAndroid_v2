package com.example.autoscroller

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import com.example.autoscroller.util.NotificationHelper
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class OverlayIndicatorService : Service() {

    companion object {
        private const val TAG = "OverlayIndicatorService"
        const val CHANNEL_ID = NotificationHelper.CHANNEL_ID
        const val NOTIF_ID = 1337

        // Keep actions in sync with AutoScrollService
        private const val ACTION_START_SCROLL = "com.example.autoscroller.ACTION_START_SCROLL"
        private const val ACTION_STOP_SCROLL  = "com.example.autoscroller.ACTION_STOP_SCROLL"

        fun start(context: Context) {
            val i = Intent(context, OverlayIndicatorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }
    }

    private lateinit var wm: WindowManager
    private var bubbleView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null

    // layered parts we need to animate
    private var outerRingDrawable: GradientDrawable? = null
    private var centerBtn: TextView? = null

    // play/pause state
    private var isRunning = false
        set(value) {
            field = value
            centerBtn?.text = if (value) "\u23F8" else "\u25B6" // ⏸ / ▶
        }

    // idle fade handling
    private val idleHandler = Handler(Looper.getMainLooper())
    private var fadeAnimator: ValueAnimator? = null
    private var ringAnimator: ValueAnimator? = null
    private val idleRunnable = Runnable { fadeToIdle() }
    private val idleDelayMs = 3000L
    private val idleAlpha = 0.25f

    // colors
    private val ringActive = 0xFFFFFFFF.toInt()   // white
    private val ringIdle   = 0xFF000000.toInt()   // black
    private val innerGradStart = 0xFF00BCD4.toInt() // cyan
    private val innerGradEnd   = 0xFF3F51B5.toInt() // indigo
    private val centerBgActive = 0xFFFF9800.toInt() // orange
    private val glyphColor     = 0xFFFFFFFF.toInt() // white

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        NotificationHelper.ensureChannel(this)
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        hideOverlay()
        showOverlay()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        builder
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Overlay indicator running")
            .setSmallIcon(android.R.drawable.presence_online)
            .setOngoing(true)
        return builder.build()
    }

    private fun showOverlay() {
        if (bubbleView != null) return

        // sizes
        val outerSize = dp(92)
        val innerSize = dp(78)
        val centerSize = dp(50)   // slightly smaller as requested
        val ringWidth = dp(4)
        val marginPx = dp(16)

        val container = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(outerSize, outerSize)
        }

        // 1) OUTER RING — white stroke (animates to black on idle)
        val outerRing = View(this)
        val ringDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0x00000000) // transparent fill
            setStroke(ringWidth, ringActive)
        }
        outerRing.background = ringDrawable
        outerRing.layoutParams = FrameLayout.LayoutParams(outerSize, outerSize)
        container.addView(outerRing)
        outerRingDrawable = ringDrawable

        // 2) INNER CIRCLE — cyan → indigo gradient
        val inner = View(this)
        val innerDrawable = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(innerGradStart, innerGradEnd)
        ).apply { shape = GradientDrawable.OVAL }
        inner.background = innerDrawable
        inner.layoutParams = FrameLayout.LayoutParams(innerSize, innerSize, Gravity.CENTER)
        container.addView(inner)

        // 3) CENTER BUTTON — orange with white ▶/⏸; sends start/stop broadcasts
        val center = TextView(this).apply {
            layoutParams = FrameLayout.LayoutParams(centerSize, centerSize, Gravity.CENTER)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(centerBgActive)
            }
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(glyphColor)
            text = "\u25B6" // ▶
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = false
            setOnClickListener {
                wakeFromIdle()
                isRunning = !isRunning
                val action = if (isRunning) ACTION_START_SCROLL else ACTION_STOP_SCROLL
                sendBroadcast(Intent(action))
            }
        }
        centerBtn = center
        container.addView(center)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            val w = resources.displayMetrics.widthPixels
            val h = resources.displayMetrics.heightPixels
            x = w - outerSize - marginPx
            y = (h - outerSize) / 2
        }

        // Drag handling
        var startX = 0
        var startY = 0
        var touchStartX = 0f
        var touchStartY = 0f
        var moved = false

        val touchListener = View.OnTouchListener { _, event ->
            val p = bubbleParams ?: return@OnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    wakeFromIdle()
                    startX = p.x
                    startY = p.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchStartX).toInt()
                    val dy = (event.rawY - touchStartY).toInt()
                    if (!moved && (abs(dx) > dp(2) || abs(dy) > dp(2))) moved = true

                    val w = resources.displayMetrics.widthPixels
                    val h = resources.displayMetrics.heightPixels

                    p.x = min(max(0, startX + dx), w - outerSize)
                    p.y = min(max(0, startY + dy), h - outerSize)

                    runCatching { wm.updateViewLayout(container, p) }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    scheduleIdle()
                    moved
                }
                else -> false
            }
        }
        container.setOnTouchListener(touchListener)
        center.setOnTouchListener(touchListener)

        runCatching { wm.addView(container, params) }
            .onSuccess {
                bubbleView = container
                bubbleParams = params
                isRunning = false // default state
                scheduleIdle()
            }
            .onFailure {
                bubbleView = null
                bubbleParams = null
            }
    }

    private fun hideOverlay() {
        val v = bubbleView ?: return
        cancelAnimations()
        runCatching { wm.removeView(v) }
        bubbleView = null
        bubbleParams = null
    }

    // --- Idle / Wake animations ---
    private fun scheduleIdle() {
        idleHandler.removeCallbacks(idleRunnable)
        idleHandler.postDelayed(idleRunnable, idleDelayMs)
    }

    private fun wakeFromIdle() {
        idleHandler.removeCallbacks(idleRunnable)
        val view = bubbleView ?: return

        // fade alpha back to 1
        fadeAnimator?.cancel()
        val current = view.alpha
        if (current < 1f) {
            fadeAnimator = ValueAnimator.ofFloat(current, 1f).apply {
                duration = 180
                addUpdateListener { view.alpha = it.animatedValue as Float }
            }
            fadeAnimator?.start()
        } else view.alpha = 1f

        // ring color → white
        outerRingDrawable?.let { ring ->
            ringAnimator?.cancel()
            ringAnimator = ValueAnimator.ofObject(ArgbEvaluator(), ringIdle, ringActive).apply {
                duration = 180
                addUpdateListener { ring.setStroke(dp(4), it.animatedValue as Int) }
            }
            ringAnimator?.start()
        }
    }

    private fun fadeToIdle() {
        val view = bubbleView ?: return

        // alpha → idle
        fadeAnimator?.cancel()
        fadeAnimator = ValueAnimator.ofFloat(view.alpha, idleAlpha).apply {
            duration = 180
            addUpdateListener { view.alpha = it.animatedValue as Float }
        }
        fadeAnimator?.start()

        // ring color white → black
        outerRingDrawable?.let { ring ->
            ringAnimator?.cancel()
            ringAnimator = ValueAnimator.ofObject(ArgbEvaluator(), ringActive, ringIdle).apply {
                duration = 180
                addUpdateListener { ring.setStroke(dp(4), it.animatedValue as Int) }
            }
            ringAnimator?.start()
        }
    }

    private fun cancelAnimations() {
        idleHandler.removeCallbacks(idleRunnable)
        fadeAnimator?.cancel()
        ringAnimator?.cancel()
        fadeAnimator = null
        ringAnimator = null
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}