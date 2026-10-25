package com.danielmoly.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class AppAdapter(private val repository: AppRepository) : BaseAdapter() {

    private var apps: List<AppInfo> = emptyList()

    fun submit(list: List<AppInfo>) {
        apps = list
        notifyDataSetChanged()
    }

    override fun getCount() = apps.size

    override fun getItem(position: Int): AppInfo = apps[position]

    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView
            ?: LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        val app = apps[position]
        view.findViewById<ImageView>(R.id.icon).setImageDrawable(repository.icon(app))
        view.findViewById<TextView>(R.id.label).text = app.label
        view.contentDescription = app.label
        return view
    }
}
