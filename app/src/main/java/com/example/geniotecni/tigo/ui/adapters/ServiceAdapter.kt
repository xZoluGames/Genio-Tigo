package com.example.geniotecni.tigo.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.geniotecni.tigo.R

class ServiceAdapter(
    private var allServices: List<String>,
    private val onItemClick: (String) -> Unit,
    private val reseteoClientePosition: Int = 7 // Position of Reseteo de Cliente in the full list
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_SERVICE = 0
        private const val TYPE_LOAD_MORE = 1
    }

    private var displayedServices = mutableListOf<String>()
    private var showingAll = false
    private var isSearchMode = false

    init {
        // Initially show services up to Reseteo de Cliente + 1
        displayedServices.addAll(allServices.take(reseteoClientePosition + 1))
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == displayedServices.size && !showingAll && !isSearchMode) {
            TYPE_LOAD_MORE
        } else {
            TYPE_SERVICE
        }
    }

    override fun getItemCount(): Int {
        return displayedServices.size + if (!showingAll && !isSearchMode && allServices.size > displayedServices.size) 1 else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_SERVICE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false)
                ServiceViewHolder(view)
            }
            TYPE_LOAD_MORE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_load_more, parent, false)
                LoadMoreViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ServiceViewHolder -> {
                val service = displayedServices[position]
                holder.textView.text = service
                holder.itemView.setOnClickListener { onItemClick(service) }
            }
            is LoadMoreViewHolder -> {
                holder.loadMoreButton.setOnClickListener {
                    showAllServices()
                }
            }
        }
    }

    private fun showAllServices() {
        displayedServices.clear()
        displayedServices.addAll(allServices)
        showingAll = true
        notifyDataSetChanged()
    }

    fun updateServices(newServices: List<String>) {
        allServices = newServices
        isSearchMode = newServices.size != allServices.size
        
        if (isSearchMode) {
            // In search mode, show all filtered services
            displayedServices.clear()
            displayedServices.addAll(newServices)
        } else {
            // Not in search mode, reset to initial state
            displayedServices.clear()
            displayedServices.addAll(allServices.take(reseteoClientePosition + 1))
            showingAll = false
        }
        notifyDataSetChanged()
    }

    class ServiceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(android.R.id.text1)
    }

    class LoadMoreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val loadMoreButton: Button = view.findViewById(R.id.btn_load_more)
    }
}
