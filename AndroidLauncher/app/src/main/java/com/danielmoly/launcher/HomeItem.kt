package com.danielmoly.launcher

/** Lo que se muestra en la cuadrícula de inicio. */
sealed class HomeItem {
    abstract val label: String

    data class App(val app: AppInfo) : HomeItem() {
        override val label get() = app.label
    }

    data class Folder(val folder: AppFolder, val apps: List<AppInfo>) : HomeItem() {
        override val label get() = folder.name
    }
}
