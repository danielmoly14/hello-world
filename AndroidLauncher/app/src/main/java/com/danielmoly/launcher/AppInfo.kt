package com.danielmoly.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable

data class AppInfo(
    val label: String,
    val component: ComponentName,
) {
    /** Clave estable para guardar la app en preferencias. */
    val key: String get() = component.flattenToString()

    fun launchIntent(): Intent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setComponent(component)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
}

/** Carga la lista de apps instaladas y cachea sus iconos. */
class AppRepository(private val context: Context) {

    private val iconCache = HashMap<String, Drawable>()

    fun loadApps(): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .map {
                AppInfo(
                    label = it.loadLabel(pm).toString(),
                    component = ComponentName(it.activityInfo.packageName, it.activityInfo.name),
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun icon(app: AppInfo): Drawable = iconCache.getOrPut(app.key) {
        try {
            context.packageManager.getActivityIcon(app.component)
        } catch (e: Exception) {
            context.packageManager.defaultActivityIcon
        }
    }

    fun clearIcons() = iconCache.clear()
}
