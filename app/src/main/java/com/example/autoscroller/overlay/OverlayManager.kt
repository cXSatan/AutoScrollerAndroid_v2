package com.example.autoscroller.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.roundToInt

class OverlayManager(
    private val context: Context,
    private val onToggle: () -> Unit
) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: TextView? = null
    private var state: State = State.STOPPED
    private val handler = Handler(Looper.getMainLooper())
    private var revertRunnable: Runnable? = null

    private enum class State { STOPPED, RUNNING, SWIPING, ERROR }

    fun show() {
        if (view != null) return

        val tv = TextView(context).apply {
            text = "STOP"
            textSize = 12f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(dp(10), dp(8), dp(10), dp(8))
            contentDescription = "AutoScroller status"
            background = pill(colorFor(state))
            elevation = dp(8).toFloat()
        }

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(16)
            y = dp(120)
        }

        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0
        tv.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { downX = e.rawX; downY = e.rawY; startX = lp.x; startY = lp.y; true }
                MotionEvent.ACTION_MOVE -> { lp.x = startX + (e.rawX - downX).toInt(); lp.y = startY + (e.rawY - downY).toInt(); wm.updateViewLayout(tv, lp); true }
                MotionEvent.ACTION_UP -> {
                    val moved = abs(e.rawX - downX) + abs(e.rawY - downY) > dp(6)
                    if (!moved) onToggle()
                    true
                }
                else -> false
            }
        }

        view = tv
        wm.addView(tv, lp)
        render()
    }

    fun hide() {
        view?.let { wm.removeView(it) }
        view = null
    }

    /** Public API the service calls */
    fun updateRunning(isRunning: Boolean) {
        state = if (isRunning) State.RUNNING else State.STOPPED
        render()
    }

    fun flashSwipe(durationMs: Long = 500L) {
        // Temporarily show SWIPING (blue), then revert to RUNNING/STOPPED
        setTransient(State.SWIPING, durationMs)
    }

    fun flashError(durationMs: Long = 700L) {
        // Temporarily show ERROR (orange), then revert
        setTransient(State.ERROR, durationMs)
    }

    private fun setTransient(temp: State, durationMs: Long) {
        // Cancel any pending revert and apply new transient state
        revertRunnable?.let { handler.removeCallbacks(it) }
        val previous = state
        state = temp
        render()
        revertRunnable = Runnable {
            state = if (previous == State.SWIPING || previous == State.ERROR) State.STOPPED else previous
            // If we were in RUNNING before, stay RUNNING; if STOPPED before, go back to STOPPED
            render()
        }.also { handler.postDelayed(it, durationMs) }
    }

    private fun render() {
        view?.apply {
            text = when (state) {
                State.STOPPED -> "STOP"
                State.RUNNING -> "RUN"
                State.SWIPING -> "SWIPE"
                State.ERROR -> "ERR"
            }
            background = pill(colorFor(state))
            contentDescription = when (state) {
                State.STOPPED -> "AutoScroller stopped"
                State.RUNNING -> "AutoScroller running"
                State.SWIPING -> "AutoScroller swiping"
                State.ERROR -> "AutoScroller error"
            }
        }
    }

    private fun pill(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(18).toFloat()
        setColor(color)
    }

    private fun colorFor(s: State): Int = when (s) {
        State.STOPPED -> 0xFFE74C3C.toInt()  // red
        State.RUNNING -> 0xFF2ECC71.toInt()  // green
        State.SWIPING -> 0xFF3498DB.toInt()  // blue
        State.ERROR   -> 0xFFF39C12.toInt()  // orange
    }

    private fun dp(v: Int): Int = (v * context.resources.displayMetrics.density).roundToInt()
    private fun dp(v: Float): Int = (v * context.resources.displayMetrics.density).roundToInt()
}
