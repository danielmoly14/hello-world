package com.danielmoly.launcher

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs

class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var store: HomeStore
    private lateinit var repository: AppRepository

    private lateinit var home: View
    private lateinit var homeEmpty: TextView
    private lateinit var homeGrid: GridView
    private lateinit var drawer: View
    private lateinit var search: EditText
    private lateinit var appsGrid: GridView

    private lateinit var homeAdapter: HomeAdapter
    private lateinit var drawerAdapter: AppAdapter
    private lateinit var gestures: GestureDetector

    private var allApps: List<AppInfo> = emptyList()
    private var folderDialog: AlertDialog? = null

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            repository.clearIcons()
            reloadApps()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = Prefs(this)
        store = HomeStore(prefs)
        repository = AppRepository(this)

        home = findViewById(R.id.home)
        homeEmpty = findViewById(R.id.home_empty)
        homeGrid = findViewById(R.id.home_grid)
        drawer = findViewById(R.id.drawer)
        search = findViewById(R.id.search)
        appsGrid = findViewById(R.id.apps_grid)

        homeAdapter = HomeAdapter(repository)
        drawerAdapter = AppAdapter(repository)
        homeGrid.adapter = homeAdapter
        appsGrid.adapter = drawerAdapter
        applyColumns()

        homeGrid.setOnItemClickListener { _, _, pos, _ ->
            when (val item = homeAdapter.getItem(pos)) {
                is HomeItem.App -> launch(item.app)
                is HomeItem.Folder -> openFolder(item)
            }
        }
        homeGrid.setOnItemLongClickListener { _, view, pos, _ ->
            when (val item = homeAdapter.getItem(pos)) {
                is HomeItem.App -> showAppMenu(view, item.app)
                is HomeItem.Folder -> showFolderMenu(view, item.folder)
            }
            true
        }
        appsGrid.setOnItemClickListener { _, _, pos, _ -> launch(drawerAdapter.getItem(pos)) }
        appsGrid.setOnItemLongClickListener { _, view, pos, _ ->
            showAppMenu(view, drawerAdapter.getItem(pos)); true
        }

        findViewById<View>(R.id.open_drawer).setOnClickListener { openDrawer() }
        findViewById<View>(R.id.menu_button).setOnClickListener { showHomeMenu(it) }
        home.setOnLongClickListener { showHomeMenu(findViewById(R.id.menu_button)); true }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = filterDrawer()
        })
        search.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO && drawerAdapter.count > 0) {
                launch(drawerAdapter.getItem(0))
                true
            } else {
                false
            }
        }

        gestures = GestureDetector(this, SwipeListener())
        registerPackageReceiver()
        reloadApps()
    }

    override fun onDestroy() {
        unregisterReceiver(packageReceiver)
        super.onDestroy()
    }

    /** Pulsar "Inicio" estando ya en el launcher cierra el cajón. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        closeDrawer()
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        // Un launcher nunca debe cerrarse con "Atrás".
        if (drawer.visibility == View.VISIBLE) closeDrawer()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestures.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    // ---------- Apps ----------

    private fun registerPackageReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(packageReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(packageReceiver, filter)
        }
    }

    private fun reloadApps() {
        Thread {
            val apps = repository.loadApps()
            runOnUiThread {
                allApps = apps
                refreshHome()
                filterDrawer()
            }
        }.start()
    }

    private fun refreshHome() {
        folderDialog?.dismiss()
        folderDialog = null

        val byKey = allApps.associateBy { it.key }
        val folders = store.folders().associateBy { HomeStore.folderEntry(it.id) }
        val items = store.entries().mapNotNull { entry ->
            val folder = folders[entry]
            if (folder != null) {
                HomeItem.Folder(folder, folder.apps.mapNotNull { byKey[it] })
            } else {
                byKey[entry]?.let { HomeItem.App(it) }
            }
        }
        homeAdapter.submit(items)
        homeEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun filterDrawer() {
        val query = search.text.toString().trim().lowercase()
        drawerAdapter.submit(
            if (query.isEmpty()) allApps
            else allApps.filter { it.label.lowercase().contains(query) }
        )
    }

    private fun launch(app: AppInfo) {
        try {
            startActivity(app.launchIntent())
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.cannot_open, Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, R.string.cannot_open, Toast.LENGTH_SHORT).show()
        }
    }

    /** Aplica un cambio a inicio; si no cabe, avisa. */
    private fun changeHome(change: () -> Boolean) {
        if (!change()) {
            Toast.makeText(this, getString(R.string.home_full, HomeStore.MAX_HOME_ITEMS), Toast.LENGTH_SHORT).show()
        }
        refreshHome()
    }

    // ---------- Carpetas ----------

    private fun openFolder(item: HomeItem.Folder) {
        val folderAdapter = AppAdapter(repository).apply { submit(item.apps) }
        val grid = GridView(this).apply {
            numColumns = prefs.columns.coerceAtMost(4)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            setSelector(android.R.color.transparent)
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            adapter = folderAdapter
        }
        val dialog = AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(item.folder.name)
            .setView(grid)
            .create()
        grid.setOnItemClickListener { _, _, pos, _ ->
            dialog.dismiss()
            launch(folderAdapter.getItem(pos))
        }
        grid.setOnItemLongClickListener { _, view, pos, _ ->
            showAppMenu(view, folderAdapter.getItem(pos)); true
        }
        folderDialog = dialog
        dialog.show()
    }

    private fun showFolderMenu(anchor: View, folder: AppFolder) {
        PopupMenu(this, anchor).apply {
            menu.add(0, 1, 0, R.string.rename_folder)
            menu.add(0, 2, 1, R.string.delete_folder)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    1 -> askName(R.string.rename_folder, folder.name) { name ->
                        store.renameFolder(folder.id, name)
                        refreshHome()
                    }
                    2 -> {
                        store.deleteFolder(folder.id)
                        refreshHome()
                    }
                }
                true
            }
            show()
        }
    }

    /** Elige carpeta de destino (o crea una nueva) para la app. */
    private fun chooseFolder(app: AppInfo) {
        val current = store.folderOf(app.key)
        val folders = store.folders().filter { it.id != current?.id }
        val options = folders.map { it.name } + getString(R.string.new_folder)
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(R.string.move_to_folder)
            .setItems(options.toTypedArray()) { _, which ->
                if (which < folders.size) {
                    store.moveToFolder(app.key, folders[which].id)
                    refreshHome()
                } else {
                    askName(R.string.new_folder, getString(R.string.default_folder_name)) { name ->
                        changeHome { store.createFolder(name, app.key) }
                    }
                }
            }
            .show()
    }

    private fun askName(titleRes: Int, initial: String, onName: (String) -> Unit) {
        val input = EditText(this).apply {
            setText(initial)
            setSelection(initial.length)
            setSingleLine()
        }
        val container = FrameLayout(this).apply {
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
            addView(input)
        }
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(titleRes)
            .setView(container)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = input.text.toString().trim()
                onName(name.ifEmpty { getString(R.string.default_folder_name) })
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // ---------- Menús ----------

    private fun showAppMenu(anchor: View, app: AppInfo) {
        val onHome = store.isOnHome(app.key)
        val inFolder = store.folderOf(app.key) != null
        PopupMenu(this, anchor).apply {
            menu.add(0, 1, 0, if (onHome) R.string.remove_from_home else R.string.add_to_home)
            menu.add(0, 4, 1, R.string.move_to_folder)
            if (inFolder) menu.add(0, 5, 2, R.string.take_out_of_folder)
            menu.add(0, 2, 3, R.string.app_info)
            menu.add(0, 3, 4, R.string.uninstall)
            setOnMenuItemClickListener {
                val pkg = app.component.packageName
                when (it.itemId) {
                    1 -> changeHome {
                        if (onHome) {
                            store.removeFromHome(app.key)
                            true
                        } else {
                            store.addToHome(app.key)
                        }
                    }
                    4 -> chooseFolder(app)
                    5 -> changeHome { store.takeOutOfFolder(app.key) }
                    2 -> safeStart(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
                    )
                    3 -> safeStart(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")))
                }
                true
            }
            show()
        }
    }

    private fun showHomeMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add(0, 1, 0, R.string.change_wallpaper)
            menu.add(0, 2, 1, getString(R.string.columns, prefs.columns))
            menu.add(0, 3, 2, R.string.choose_default_launcher)
            menu.add(0, 4, 3, R.string.system_settings)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    1 -> safeStart(
                        Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), getString(R.string.change_wallpaper))
                    )
                    2 -> {
                        prefs.columns = if (prefs.columns >= 6) 3 else prefs.columns + 1
                        applyColumns()
                    }
                    3 -> safeStart(Intent(Settings.ACTION_HOME_SETTINGS))
                    4 -> safeStart(Intent(Settings.ACTION_SETTINGS))
                }
                true
            }
            show()
        }
    }

    private fun applyColumns() {
        homeGrid.numColumns = prefs.columns
        appsGrid.numColumns = prefs.columns
    }

    private fun safeStart(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.cannot_open, Toast.LENGTH_SHORT).show()
        }
    }

    // ---------- Cajón ----------

    private fun openDrawer() {
        if (drawer.visibility == View.VISIBLE) return
        drawer.alpha = 0f
        drawer.translationY = drawer.height * 0.15f
        drawer.visibility = View.VISIBLE
        drawer.animate().alpha(1f).translationY(0f).setDuration(200).start()
        home.visibility = View.INVISIBLE
        appsGrid.setSelection(0)
    }

    private fun closeDrawer() {
        if (drawer.visibility != View.VISIBLE) return
        hideKeyboard()
        search.setText("")
        drawer.visibility = View.GONE
        home.visibility = View.VISIBLE
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(search.windowToken, 0)
        search.clearFocus()
    }

    /** Usa una API oculta del sistema para bajar la barra de notificaciones. */
    @SuppressLint("WrongConstant")
    private fun expandNotifications() {
        try {
            val service = getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager")
                .getMethod("expandNotificationsPanel")
                .invoke(service)
        } catch (e: Exception) {
            // No soportado en este dispositivo: ignorar.
        }
    }

    /** Deslizar arriba abre el cajón; abajo lo cierra (o baja las notificaciones en inicio). */
    private inner class SwipeListener : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (e1 == null) return false
            val dy = e2.y - e1.y
            val dx = e2.x - e1.x
            if (abs(dy) < SWIPE_DISTANCE || abs(dy) < abs(dx) || abs(velocityY) < SWIPE_VELOCITY) return false

            val drawerOpen = drawer.visibility == View.VISIBLE
            when {
                dy < 0 && !drawerOpen -> openDrawer()
                dy > 0 && drawerOpen && !appsGrid.canScrollVertically(-1) -> closeDrawer()
                dy > 0 && !drawerOpen -> expandNotifications()
                else -> return false
            }
            return true
        }
    }

    companion object {
        private const val SWIPE_DISTANCE = 120
        private const val SWIPE_VELOCITY = 300
    }
}
