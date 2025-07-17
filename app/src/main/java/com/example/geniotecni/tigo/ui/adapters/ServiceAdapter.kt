package com.example.geniotecni.tigo.ui.adapters

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

data class ServiceItem(
    val name: String,
    val description: String,
    val icon: Int,
    val colorResId: Int,
    val serviceType: Int
)

class ServiceAdapter(
    private var services: List<ServiceItem>,
    private val onItemClick: (ServiceItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_SERVICE = 0
        private const val TYPE_LOAD_MORE = 1

        // Service descriptions mapping
        private val serviceDescriptions = mapOf(
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
            "Telefonia Personal" to "Servicios de Personal",
            // Add more descriptions as needed
        )

        // Service icons mapping
        private val serviceIcons = mapOf(
            "Giros Tigo" to R.drawable.ic_money,
            "Retiros Tigo" to R.drawable.ic_money,
            "Carga Billetera Tigo" to R.drawable.ic_money,
            "Telefonia Tigo" to R.drawable.ic_phone,
            "Pago TV e Internet Hogar" to R.drawable.ic_home,
            "Antena (Wimax)" to R.drawable.ic_wifi,
            "Tigo TV anticipado" to R.drawable.ic_tv,
            "Reseteo de Cliente" to R.drawable.ic_reset,
            "ANDE" to R.drawable.ic_electricity,
            "ESSAP" to R.drawable.ic_water,
            "COPACO" to R.drawable.ic_phone,
            "Retiros Personal" to R.drawable.ic_money,
            "Telefonia Personal" to R.drawable.ic_phone,
            // Add more icons as needed
        )

        // Service colors mapping
        private val serviceColors = mapOf(
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
            "Telefonia Personal" to R.color.service_personal,
            // Add more colors as needed
        )
    }

    private var showingAll = false
    private var isSearchMode = false
    private val displayLimit = 8 // Show first 8 services initially

    init {
        updateDisplayedServices()
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == services.size && !showingAll && !isSearchMode && services.size > displayLimit) {
            TYPE_LOAD_MORE
        } else {
            TYPE_SERVICE
        }
    }

    override fun getItemCount(): Int {
        val displayCount = if (showingAll || isSearchMode) services.size else services.take(displayLimit).size
        return displayCount + if (!showingAll && !isSearchMode && services.size > displayLimit) 1 else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_SERVICE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_service, parent, false)
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
                val service = if (showingAll || isSearchMode) {
                    services[position]
                } else {
                    services.take(displayLimit)[position]
                }
                holder.bind(service)
            }
            is LoadMoreViewHolder -> {
                holder.bind()
            }
        }
    }

    fun updateServices(newServiceNames: List<String>) {
        isSearchMode = newServiceNames.size != services.size

        val newServices = newServiceNames.mapIndexed { index, name ->
            ServiceItem(
                name = name,
                description = serviceDescriptions[name] ?: "Servicio disponible",
                icon = serviceIcons[name] ?: R.drawable.ic_money,
                colorResId = serviceColors[name] ?: R.color.md_theme_light_primary,
                serviceType = index
            )
        }

        val diffCallback = ServiceDiffCallback(services, newServices)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        services = newServices
        updateDisplayedServices()
        diffResult.dispatchUpdatesTo(this)
    }

    private fun updateDisplayedServices() {
        // This is now handled in getItemCount and onBindViewHolder
    }

    private fun showAllServices() {
        showingAll = true
        notifyDataSetChanged()
    }

    inner class ServiceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val card: MaterialCardView = view.findViewById(R.id.root) ?: view as MaterialCardView
        private val iconBackground: View = view.findViewById(R.id.iconBackground)
        private val serviceIcon: ImageView = view.findViewById(R.id.serviceIcon)
        private val serviceName: TextView = view.findViewById(R.id.serviceName)
        private val serviceDescription: TextView = view.findViewById(R.id.serviceDescription)

        fun bind(service: ServiceItem) {
            serviceName.text = service.name
            serviceDescription.text = service.description
            serviceIcon.setImageResource(service.icon)

            // Set icon background color
            iconBackground.background.setTint(ContextCompat.getColor(itemView.context, service.colorResId))

            // Set click listener
            card.setOnClickListener {
                onItemClick(service)
            }
        }
    }

    inner class LoadMoreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val loadMoreButton: View = view.findViewById(R.id.btn_load_more)

        fun bind() {
            loadMoreButton.setOnClickListener {
                showAllServices()
            }
        }
    }

    class ServiceDiffCallback(
        private val oldList: List<ServiceItem>,
        private val newList: List<ServiceItem>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].name == newList[newItemPosition].name
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}

// Extension function to create ServiceItems from service names
fun List<String>.toServiceItems(): List<ServiceItem> {
    return this.mapIndexed { index, name ->
        ServiceItem(
            name = name,
            description = ServiceAdapter.serviceDescriptions[name] ?: "Servicio disponible",
            icon = ServiceAdapter.serviceIcons[name] ?: R.drawable.ic_money,
            colorResId = ServiceAdapter.serviceColors[name] ?: R.color.md_theme_light_primary,
            serviceType = index
        )
    }
}