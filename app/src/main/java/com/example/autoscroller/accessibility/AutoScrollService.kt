package com.example.autoscroller.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter

class AutoScrollService : AccessibilityService() {

    companion object {
        private const val TAG = "AutoScrollService"

        const val ACTION_START_SCROLL = "com.example.autoscroller.ACTION_START_SCROLL"
        const val ACTION_STOP_SCROLL  = "com.example.autoscroller.ACTION_STOP_SCROLL"
    }

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    private val ticker = object : Runnable {
        override fun run() {
            if (!isRunning) return
            performScrollGesture()
            handler.postDelayed(this, 800) // ~0.8s
        }
    }

    private val toggleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_START_SCROLL -> startScrolling()
                ACTION_STOP_SCROLL  -> stopScrolling()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service connected")
        // listen for overlay toggle intents
        val f = IntentFilter().apply {
            addAction(ACTION_START_SCROLL)
            addAction(ACTION_STOP_SCROLL)
        }
        registerReceiver(toggleReceiver, f)
    }

    override fun onInterrupt() {
        // no-op
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // not driven by events; we scroll on a timer when isRunning == true
    }

    private fun startScrolling() {
        if (isRunning) return
        isRunning = true
        handler.removeCallbacks(ticker)
        handler.post(ticker)
        Log.d(TAG, "Scrolling started")
    }

    private fun stopScrolling() {
        if (!isRunning) return
        isRunning = false
        handler.removeCallbacks(ticker)
        Log.d(TAG, "Scrolling stopped")
    }

    private fun performScrollGesture() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return

        // simple upward swipe near the right edge, from 70% -> 30% height
        val dm = resources.displayMetrics
        val w = dm.widthPixels.toFloat()
        val h = dm.heightPixels.toFloat()
        val startX = w * 0.85f
        val startY = h * 0.70f
        val endX   = w * 0.85f
        val endY   = h * 0.30f

        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }

        val stroke = GestureDescription.StrokeDescription(path, /*startTime*/0, /*duration*/300)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        dispatchGesture(gesture, null, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { unregisterReceiver(toggleReceiver) }
        handler.removeCallbacks(ticker)
        isRunning = false
        Log.d(TAG, "Service destroyed")
    }
}