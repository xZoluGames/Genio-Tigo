package com.example.geniotecni.tigo

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PrintHistoryAdapter(
    private val printHistory: List<PrintData>,
    private val onItemClick: (PrintData) -> Unit
) : RecyclerView.Adapter<PrintHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewService: TextView = view.findViewById(R.id.textViewService)
        val textViewDateTime: TextView = view.findViewById(R.id.textViewDateTime)
        val textViewMessage: TextView = view.findViewById(R.id.textViewMessage)
        val buttonReprint: Button = view.findViewById(R.id.buttonReprint)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_print_history, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val printData = printHistory[position]
        holder.textViewService.text = printData.service
        holder.textViewDateTime.text = "${printData.date} ${printData.time}"
        holder.textViewMessage.text = printData.message
        holder.buttonReprint.setOnClickListener { onItemClick(printData) }
    }

    override fun getItemCount() = printHistory.size
}