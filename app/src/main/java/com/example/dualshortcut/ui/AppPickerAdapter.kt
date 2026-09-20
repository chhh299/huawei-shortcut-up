package com.example.dualshortcut.ui

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dualshortcut.databinding.ItemAppPickerBinding

data class AppEntry(
    val label: String,
    val packageName: String,
    val className: String?,
    val icon: Drawable
)

class AppPickerAdapter(
    private val onItemClick: (AppEntry) -> Unit
) : RecyclerView.Adapter<AppPickerAdapter.AppViewHolder>() {

    private var allApps: List<AppEntry> = emptyList()
    private var filteredApps: List<AppEntry> = emptyList()

    fun submitList(list: List<AppEntry>) {
        allApps = list
        filteredApps = list
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        val trimmed = query.trim().lowercase()
        filteredApps = if (trimmed.isEmpty()) {
            allApps
        } else {
            allApps.filter {
                it.label.lowercase().contains(trimmed) ||
                it.packageName.lowercase().contains(trimmed)
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppPickerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(filteredApps[position])
    }

    override fun getItemCount(): Int = filteredApps.size

    inner class AppViewHolder(private val binding: ItemAppPickerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: AppEntry) {
            binding.tvAppName.text = entry.label
            binding.tvAppPackage.text = entry.packageName
            binding.ivAppIcon.setImageDrawable(entry.icon)
            binding.root.setOnClickListener {
                onItemClick(entry)
            }
        }
    }
}
