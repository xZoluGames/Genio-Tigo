package com.example.geniotecni.tigo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ServiceAdapter(
    private var services: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ServiceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val service = services[position]
        holder.textView.text = service
        holder.itemView.setOnClickListener { onItemClick(service) }
    }

    override fun getItemCount() = services.size

    fun updateServices(newServices: List<String>) {
        services = newServices
        notifyDataSetChanged()
    }
}
