package com.danielmoly.launcher

/**
 * Organización de la pantalla de inicio: apps sueltas y carpetas.
 * Cada app está como mucho en un sitio: suelta en inicio o dentro de una carpeta.
 */
class HomeStore(private val prefs: Prefs) {

    fun entries(): List<String> = prefs.homeApps

    fun folders(): List<AppFolder> = prefs.folders

    fun folderOf(key: String): AppFolder? = folders().firstOrNull { key in it.apps }

    fun isOnHome(key: String) = key in entries() || folderOf(key) != null

    /** Devuelve false si la pantalla de inicio está llena. */
    fun addToHome(key: String): Boolean {
        if (isOnHome(key)) return true
        val entries = entries()
        if (entries.size >= MAX_HOME_ITEMS) return false
        prefs.homeApps = entries + key
        return true
    }

    fun removeFromHome(key: String) {
        val folders = folders().onEach { it.apps.remove(key) }
        save(entries() - key, folders)
    }

    fun moveToFolder(key: String, folderId: String) {
        val folders = folders().onEach { it.apps.remove(key) }
        folders.firstOrNull { it.id == folderId }?.apps?.add(key) ?: return
        save(entries() - key, folders)
    }

    /** Crea una carpeta con la app dentro; ocupa el hueco de la app si estaba suelta en inicio. */
    fun createFolder(name: String, key: String): Boolean {
        val entries = entries().toMutableList()
        val folders = folders().onEach { it.apps.remove(key) }.toMutableList()
        val folder = AppFolder(System.currentTimeMillis().toString(), name, mutableListOf(key))
        val index = entries.indexOf(key)
        if (index >= 0) {
            entries[index] = folderEntry(folder.id)
        } else {
            if (entries.size >= MAX_HOME_ITEMS) return false
            entries.add(folderEntry(folder.id))
        }
        folders.add(folder)
        save(entries, folders)
        return true
    }

    /** Saca la app de su carpeta y la deja suelta justo después de ella. */
    fun takeOutOfFolder(key: String): Boolean {
        val entries = entries().toMutableList()
        val folders = folders()
        val folder = folders.firstOrNull { key in it.apps } ?: return true
        if (folder.apps.size > 1 && entries.size >= MAX_HOME_ITEMS) return false
        folder.apps.remove(key)
        entries.add(entries.indexOf(folderEntry(folder.id)) + 1, key)
        save(entries, folders)
        return true
    }

    fun renameFolder(folderId: String, name: String) {
        val folders = folders()
        folders.firstOrNull { it.id == folderId }?.name = name
        prefs.folders = folders
    }

    /** Borra la carpeta y deja sus apps sueltas en su lugar (mientras quepan). */
    fun deleteFolder(folderId: String) {
        val folders = folders().toMutableList()
        val folder = folders.firstOrNull { it.id == folderId } ?: return
        folders.remove(folder)
        val entries = entries().toMutableList()
        val index = entries.indexOf(folderEntry(folderId)).coerceAtLeast(0)
        entries.remove(folderEntry(folderId))
        val room = (MAX_HOME_ITEMS - entries.size).coerceAtLeast(0)
        entries.addAll(index.coerceAtMost(entries.size), folder.apps.take(room))
        save(entries, folders)
    }

    /** Guarda y elimina las carpetas que se hayan quedado vacías. */
    private fun save(entries: List<String>, folders: List<AppFolder>) {
        val (empty, kept) = folders.partition { it.apps.isEmpty() }
        val emptyEntries = empty.map { folderEntry(it.id) }.toSet()
        prefs.folders = kept
        prefs.homeApps = entries.filter { it !in emptyEntries }
    }

    companion object {
        const val MAX_HOME_ITEMS = 24
        private const val FOLDER_PREFIX = "folder:"

        fun folderEntry(folderId: String) = FOLDER_PREFIX + folderId
    }
}
