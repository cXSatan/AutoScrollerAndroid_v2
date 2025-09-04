package com.example.autoscroller.ui.setup

import android.app.Activity
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.autoscroller.R
import com.example.autoscroller.util.Permissions
import com.example.autoscroller.AutoScrollAccessibilityService
import com.example.autoscroller.accessibility.AutoScrollService
import com.example.autoscroller.OverlayIndicatorService

class SetupActivity : Activity() {

    private val tvAcc by lazy { findViewById<TextView>(R.id.tvAccessibilityStatus) }
    private val tvOverlay by lazy { findViewById<TextView>(R.id.tvOverlayStatus) }
    private val tvNotif by lazy { findViewById<TextView>(R.id.tvNotificationsStatus) }

    private lateinit var accObserver: ContentObserver
    private val statusHandler by lazy { Handler(Looper.getMainLooper()) }
    private val statusPoll = object : Runnable {
        override fun run() {
            updateOverlayStatus()
            updateNotificationsStatus()
            statusHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        // normalize any garbled header text
        normalizeHeaderText()

        // Observe accessibility changes for instant updates
        accObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                updateAccessibilityStatus()
            }
        }

        wireButtons()
        refreshAllStatuses()
    }

    override fun onResume() {
        super.onResume()
        contentResolver.registerContentObserver(
            Permissions.enabledAccessibilityServicesUri,
            false,
            accObserver
        )
        refreshAllStatuses()
        statusHandler.removeCallbacks(statusPoll)
        statusHandler.post(statusPoll)
    }

    override fun onPause() {
        super.onPause()
        runCatching { contentResolver.unregisterContentObserver(accObserver) }
        statusHandler.removeCallbacks(statusPoll)
    }

    // --- Buttons wiring (match by label; case-insensitive, contains) ---
    private fun wireButtons() {
        // Accessibility settings
        findButtonByText("OPEN ACCESSIBILITY SETTINGS")?.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        // Overlay permission
        findButtonByText("ALLOW DISPLAY OVER OTHER APPS")?.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:")
                )
                startActivity(intent)
            }
        }

        // Notifications settings
        findButtonByText("ALLOW NOTIFICATIONS (STATUS)")?.setOnClickListener {
            val intent = Intent().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                } else {
                    action = "android.settings.APP_NOTIFICATION_SETTINGS"
                    putExtra("app_package", packageName)
                    putExtra("app_uid", applicationInfo.uid)
                }
            }
            startActivity(intent)
        }

        // Start overlay indicator
        findButtonByText("START OVERLAY INDICATOR")?.setOnClickListener {
            Log.d("SetupActivity", "Start overlay button clicked")
            Toast.makeText(this, "Starting overlay…", Toast.LENGTH_SHORT).show()
            OverlayIndicatorService.start(this)
        }
    }

    private fun findButtonByText(label: String): Button? {
        val target = label.trim().lowercase()
        val root = findViewById<View>(android.R.id.content) ?: return null
        return findButtonIn(root, target)
    }

    private fun findButtonIn(view: View, target: String): Button? {
        if (view is Button) {
            val t = view.text?.toString()?.trim()?.lowercase() ?: ""
            if (t == target || t.contains(target)) return view
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findButtonIn(view.getChildAt(i), target)
                if (found != null) return found
            }
        }
        return null
    }

    // --- Fix header text rendered from XML (handles garbled encodings) ---
    private fun normalizeHeaderText() {
        val root = findViewById<View>(android.R.id.content) as? ViewGroup ?: return
        val targets = mutableListOf<TextView>()
        collectTextViews(root, targets)

        for (tv in targets) {
            val raw = tv.text?.toString() ?: continue
            val normalized = raw
                .replace("Ã¢â‚¬Â¢", "•")
                .replace("â€¢", "•")
                .replace("&#8226;", "•")
            if (normalized.contains("AutoScroller") && normalized.contains("Setup")) {
                tv.text = getString(R.string.setup_title)
                break
            }
        }
    }

    private fun collectTextViews(v: View, out: MutableList<TextView>) {
        if (v is TextView) out.add(v)
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) collectTextViews(v.getChildAt(i), out)
        }
    }

    // --- Status badges ---
    private fun refreshAllStatuses() {
        updateAccessibilityStatus()
        updateOverlayStatus()
        updateNotificationsStatus()
    }

    private fun updateAccessibilityStatus() {
        val enabled = Permissions.isAnyAccessibilityServiceEnabled(
            this,
            AutoScrollAccessibilityService::class.java,
            AutoScrollService::class.java
        )
        setStatusBadge(tvAcc, enabled)
    }

    private fun updateOverlayStatus() {
        val enabled = Permissions.hasOverlayPermission(this)
        setStatusBadge(tvOverlay, enabled)
    }

    private fun updateNotificationsStatus() {
        val enabled = Permissions.areNotificationsEnabled(this)
        setStatusBadge(tvNotif, enabled)
    }

    private fun setStatusBadge(view: TextView?, enabled: Boolean) {
        view ?: return
        view.text = if (enabled) "ENABLED" else "DISABLED"
        view.alpha = if (enabled) 1.0f else 0.55f
    }
}