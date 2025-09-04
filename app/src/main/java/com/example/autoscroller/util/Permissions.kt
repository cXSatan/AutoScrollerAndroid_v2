package com.example.autoscroller.util

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

object Permissions {

    /** Overlay (floating bubble) permission */
    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }

    /** Notifications enabled for this app */
    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Return true if ANY of the provided AccessibilityService classes is enabled */
    fun isAnyAccessibilityServiceEnabled(
        context: Context,
        vararg serviceClasses: Class<out AccessibilityService>
    ): Boolean {
        val cr = context.contentResolver
        val accessibilityOn = Settings.Secure.getInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1
        if (!accessibilityOn) return false

        val enabledServices = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false

        // Compare against both flat and short forms to handle OEM differences
        return serviceClasses.any { svc ->
            val comp = ComponentName(context, svc)
            val flat = comp.flattenToString()
            val shortFlat = comp.flattenToShortString()
            enabledServices.split(':').any { entry ->
                entry.equals(flat, true) || entry.equals(shortFlat, true)
            }
        }
    }

    /** Observe this for instant updates when enabled services change */
    val enabledAccessibilityServicesUri
        get() = Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
}