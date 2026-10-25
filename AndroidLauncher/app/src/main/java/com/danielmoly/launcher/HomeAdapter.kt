package com.danielmoly.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class HomeAdapter(private val repository: AppRepository) : BaseAdapter() {

    private var items: List<HomeItem> = emptyList()

    fun submit(list: List<HomeItem>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getCount() = items.size

    override fun getItem(position: Int): HomeItem = items[position]

    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView
            ?: LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        val item = items[position]
        val icon = when (item) {
            is HomeItem.App -> repository.icon(item.app)
            is HomeItem.Folder -> FolderIcon(item.apps.map { repository.icon(it) })
        }
        view.findViewById<ImageView>(R.id.icon).setImageDrawable(icon)
        view.findViewById<TextView>(R.id.label).text = item.label
        view.contentDescription = item.label
        return view
    }
}
