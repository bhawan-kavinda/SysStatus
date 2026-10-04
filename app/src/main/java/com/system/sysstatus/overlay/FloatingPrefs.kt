package com.system.sysstatus.overlay

import android.content.Context
import android.content.SharedPreferences

// On/off, position and look of the floating window
object FloatingPrefs {

    const val KEY_SIZE = "size"
    const val KEY_ICON = "icon"
    const val KEY_ALPHA = "alpha"
    const val KEY_SNAP = "snap"

    const val DEFAULT_SIZE = 56
    const val MIN_SIZE = 36
    const val MAX_SIZE = 96
    const val SIZE_STEP = 4

    // 0 = app icon, 1..4 = custom icons
    const val ICON_COUNT = 5

    const val MIN_ALPHA = 0.2f

    private fun sp(c: Context) =
        c.applicationContext.getSharedPreferences("floating", Context.MODE_PRIVATE)

    fun isEnabled(c: Context) = sp(c).getBoolean("enabled", false)
    fun setEnabled(c: Context, on: Boolean) = sp(c).edit().putBoolean("enabled", on).apply()

    // True once the first-launch sheet has been shown and closed
    fun onboardingSeen(c: Context) = sp(c).getBoolean("onboarding_seen", false)
    fun setOnboardingSeen(c: Context, seen: Boolean) =
        sp(c).edit().putBoolean("onboarding_seen", seen).apply()

    fun savedRight(c: Context) = sp(c).getBoolean("right", true)
    fun savedY(c: Context) = sp(c).getFloat("y", 0.3f)

    // 0f = left edge, 1f = right edge (used when snap to edge is off)
    fun savedX(c: Context) = sp(c).getFloat("x", if (savedRight(c)) 1f else 0f)

    fun savePosition(
        c: Context,
        right: Boolean,
        yFraction: Float,
        xFraction: Float = if (right) 1f else 0f
    ) = sp(c).edit()
        .putBoolean("right", right)
        .putFloat("y", yFraction)
        .putFloat("x", xFraction)
        .apply()

    // ---------- look ----------

    fun size(c: Context) = sp(c).getInt(KEY_SIZE, DEFAULT_SIZE).coerceIn(MIN_SIZE, MAX_SIZE)
    fun setSize(c: Context, dp: Int) =
        sp(c).edit().putInt(KEY_SIZE, dp.coerceIn(MIN_SIZE, MAX_SIZE)).apply()

    fun icon(c: Context) = sp(c).getInt(KEY_ICON, 0).coerceIn(0, ICON_COUNT - 1)
    fun setIcon(c: Context, index: Int) =
        sp(c).edit().putInt(KEY_ICON, index.coerceIn(0, ICON_COUNT - 1)).apply()

    fun alpha(c: Context) = sp(c).getFloat(KEY_ALPHA, 1f).coerceIn(MIN_ALPHA, 1f)
    fun setAlpha(c: Context, value: Float) =
        sp(c).edit().putFloat(KEY_ALPHA, value.coerceIn(MIN_ALPHA, 1f)).apply()

    fun snap(c: Context) = sp(c).getBoolean(KEY_SNAP, true)
    fun setSnap(c: Context, on: Boolean) = sp(c).edit().putBoolean(KEY_SNAP, on).apply()

    // The caller must keep a strong reference to the listener
    fun register(c: Context, l: SharedPreferences.OnSharedPreferenceChangeListener) =
        sp(c).registerOnSharedPreferenceChangeListener(l)

    fun unregister(c: Context, l: SharedPreferences.OnSharedPreferenceChangeListener) =
        sp(c).unregisterOnSharedPreferenceChangeListener(l)
}
