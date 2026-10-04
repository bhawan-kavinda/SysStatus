package com.system.sysstatus.overlay

import android.content.Context

// Small on/off + position store for the floating window
object FloatingPrefs {

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
    fun savePosition(c: Context, right: Boolean, yFraction: Float) =
        sp(c).edit().putBoolean("right", right).putFloat("y", yFraction).apply()
}
