package com.danielmoly.launcher

import android.content.Context

class Prefs(context: Context) {

    private val prefs = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    /** Apps fijadas en la pantalla de inicio, en orden. */
    var homeApps: List<String>
        get() = prefs.getString(KEY_HOME, "").orEmpty().split('\n').filter { it.isNotBlank() }
        set(value) = prefs.edit().putString(KEY_HOME, value.joinToString("\n")).apply()

    var columns: Int
        get() = prefs.getInt(KEY_COLUMNS, 4)
        set(value) = prefs.edit().putInt(KEY_COLUMNS, value).apply()

    companion object {
        private const val KEY_HOME = "home_apps"
        private const val KEY_COLUMNS = "columns"
    }
}
