package com.example.geniotecni.tigo.ui.adapters

import android.util.Log
import com.example.geniotecni.tigo.utils.AppLogger
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.geniotecni.tigo.R
import com.google.android.material.card.MaterialCardView

class ServiceAdapter(
    private var services: List<ServiceItem>,
    private val onItemClick: (ServiceItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_SERVICE = 0
        private const val TYPE_LOAD_MORE = 1

        // Service descriptions mapping
        val serviceDescriptions = mapOf(
            "Giros Tigo" to "Envía dinero de forma rápida y segura",
            "Retiros Tigo" to "Retira efectivo desde tu billetera",
            "Carga Billetera Tigo" to "Recarga tu billetera digital",
            "Telefonia Tigo" to "Gestiona tu línea telefónica",
            "Pago TV e Internet Hogar" to "Paga tus servicios del hogar",
            "Antena (Wimax)" to "Servicio de internet inalámbrico",
            "Tigo TV anticipado" to "Prepago de televisión digital",
            "Reseteo de Cliente" to "Restablece información del cliente",
            "ANDE" to "Pago de energía eléctrica",
            "ESSAP" to "Pago de agua potable",
            "COPACO" to "Telefonía fija nacional",
            "Retiros Personal" to "Retira dinero de Personal",
            "Telefonia Personal" to "Servicios de Personal"
        )

        // Service icons mapping - FIXED WITH ACTUAL DRAWABLE NAMES
        val serviceIcons = mapOf(
            "Giros Tigo" to R.drawable.tigo_cuadrado,
            "Retiros Tigo" to R.drawable.tigo_cuadrado,
            "Carga Billetera Tigo" to R.drawable.tigo_cuadrado,
            "Telefonia Tigo" to R.drawable.tigo_cuadrado,
            "Pago TV e Internet Hogar" to R.drawable.tigo_cuadrado,
            "Antena (Wimax)" to R.drawable.tigo_cuadrado,
            "Tigo TV anticipado" to R.drawable.tigo_cuadrado,
            "Reseteo de Cliente" to R.drawable.tigo_cuadrado,
            "ANDE" to R.drawable.ande_icon,
            "ESSAP" to R.drawable.essap_icon,
            "COPACO" to R.drawable.cocapo_icon,
            "Retiros Personal" to R.drawable.personal_logo,
            "Telefonia Personal" to R.drawable.personal_logo
        )

        // Service colors mapping
        val serviceColors = mapOf(
            "Giros Tigo" to R.color.service_tigo,
            "Retiros Tigo" to R.color.service_tigo,
            "Carga Billetera Tigo" to R.color.service_tigo,
            "Telefonia Tigo" to R.color.service_tigo,
            "Pago TV e Internet Hogar" to R.color.service_tigo,
            "Antena (Wimax)" to R.color.service_tigo,
            "Tigo TV anticipado" to R.color.service_tigo,
            "Reseteo de Cliente" to R.color.service_tigo,
            "ANDE" to R.color.service_ande,
            "ESSAP" to R.color.status_info,
            "COPACO" to R.color.status_success,
            "Retiros Personal" to R.color.service_personal,
            "Telefonia Personal" to R.color.service_personal
        )

        // Convert string list to ServiceItem list
        fun List<String>.toServiceItems(): List<ServiceItem> {
            return this.mapIndexed { index, name ->
                ServiceItem(
                    id = index,
                    name = name,
                    description = serviceDescriptions[name] ?: "Servicio disponible",
                    icon = serviceIcons[name] ?: R.drawable.ic_service_default,
                    color = serviceColors[name] ?: R.color.md_theme_light_primary,
                    isActive = true
                )
            }
        }
    }

    inner class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.serviceCard)
        val iconBackground: View = itemView.findViewById(R.id.iconBackground)
        val serviceIcon: ImageView = itemView.findViewById(R.id.serviceIcon)
        val serviceName: TextView = itemView.findViewById(R.id.serviceName)
        val serviceDescription: TextView = itemView.findViewById(R.id.serviceDescription)

        init {
            cardView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION && position < services.size) {
                    onItemClick(services[position])
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return TYPE_SERVICE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_service, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ServiceViewHolder) {
            val service = services[position]

            // Set service name and description
            holder.serviceName.text = service.name
            holder.serviceDescription.text = service.description

            // Set icon - FIXED: Now properly loads drawable resources
            try {
                holder.serviceIcon.setImageResource(service.icon)
                AppLogger.d("ServiceAdapter", "Icon loaded successfully for ${service.name}")
            } catch (e: Exception) {
                AppLogger.e("ServiceAdapter", "Error loading icon for ${service.name}", e)
                holder.serviceIcon.setImageResource(R.drawable.ic_service_default)
            }

            // Set background color for icon
            try {
                val color = ContextCompat.getColor(holder.itemView.context, service.color)
                holder.iconBackground.background?.setTint(color)
            } catch (e: Exception) {
                AppLogger.e("ServiceAdapter", "Error setting color for ${service.name}", e)
                val defaultColor = ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.md_theme_light_primary
                )
                holder.iconBackground.background?.setTint(defaultColor)
            }

            // Add animation
            holder.itemView.alpha = 0f
            holder.itemView.animate()
                .alpha(1f)
                .setDuration(200)
                .setStartDelay((position * 50).toLong())
                .start()
        }
    }

    override fun getItemCount(): Int = services.size

    fun updateServices(newServices: List<ServiceItem>) {
        val diffCallback = ServiceDiffCallback(services, newServices)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        services = newServices
        diffResult.dispatchUpdatesTo(this)
    }

    fun updateServicesFromStrings(newServices: List<String>) {
        updateServices(newServices.toServiceItems())
    }

    class ServiceDiffCallback(
        private val oldList: List<ServiceItem>,
        private val newList: List<ServiceItem>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}

