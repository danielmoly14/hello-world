package com.danielmoly.launcher

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AppFolder(
    val id: String,
    var name: String,
    val apps: MutableList<String>,
)

class Prefs(context: Context) {

    private val prefs = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    /** Elementos de la pantalla de inicio, en orden: claves de apps o "folder:<id>". */
    var homeApps: List<String>
        get() = prefs.getString(KEY_HOME, "").orEmpty().split('\n').filter { it.isNotBlank() }
        set(value) = prefs.edit().putString(KEY_HOME, value.joinToString("\n")).apply()

    var folders: List<AppFolder>
        get() {
            val json = JSONArray(prefs.getString(KEY_FOLDERS, "[]"))
            return (0 until json.length()).map { i ->
                val obj = json.getJSONObject(i)
                val apps = obj.getJSONArray("apps")
                AppFolder(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    apps = (0 until apps.length()).map { apps.getString(it) }.toMutableList(),
                )
            }
        }
        set(value) {
            val json = JSONArray()
            value.forEach { folder ->
                json.put(
                    JSONObject()
                        .put("id", folder.id)
                        .put("name", folder.name)
                        .put("apps", JSONArray(folder.apps))
                )
            }
            prefs.edit().putString(KEY_FOLDERS, json.toString()).apply()
        }

    var columns: Int
        get() = prefs.getInt(KEY_COLUMNS, 4)
        set(value) = prefs.edit().putInt(KEY_COLUMNS, value).apply()

    companion object {
        private const val KEY_HOME = "home_apps"
        private const val KEY_FOLDERS = "folders"
        private const val KEY_COLUMNS = "columns"
    }
}
