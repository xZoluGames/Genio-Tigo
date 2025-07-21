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
    private val onItemClick: (ServiceItem) -> Unit,
    private val onViewMoreClick: () -> Unit = {} // Callback para "Ver más"
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_SERVICE = 0
        private const val TYPE_VIEW_MORE = 1

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
            // Personal services
            "Retiros Personal" to "Retira dinero de Personal",
            "Telefonia Personal" to "Servicios de Personal",
            // Cooperativas
            "Cooperativa Universitaria (Prestamos)" to "Préstamos disponibles",
            "Cooperativa Universitaria (Tarjeta Mastercard)" to "Tarjeta de crédito",
            "Cooperativa Medalla Milagrosa (Tarjeta Visa)" to "Tarjeta de crédito Visa",
            "Cooperativa Medalla Milagrosa (Solidaridad)" to "Fondo de solidaridad",
            "Cooperativa Medalla Milagrosa (Creditos)" to "Créditos disponibles",
            // Otros servicios
            "Ver más" to "Mostrar todos los servicios disponibles"
        )

        // Service icons mapping - COMPLETO con todos los drawables disponibles
        val serviceIcons = mapOf(
            // Tigo services
            "Giros Tigo" to R.drawable.tigo_cuadrado,
            "Retiros Tigo" to R.drawable.tigo_cuadrado,
            "Carga Billetera Tigo" to R.drawable.tigo_cuadrado,
            "Telefonia Tigo" to R.drawable.tigo_cuadrado,
            "Pago TV e Internet Hogar" to R.drawable.tigo_cuadrado,
            "Antena (Wimax)" to R.drawable.ic_wifi,
            "Tigo TV anticipado" to R.drawable.ic_tv,
            "Reseteo de Cliente" to R.drawable.ic_reset,

            // Servicios básicos
            "ANDE" to R.drawable.ande_icon,
            "ESSAP" to R.drawable.essap_icon,
            "COPACO" to R.drawable.cocapo_icon,

            // Personal
            "Retiros Personal" to R.drawable.personal_logo,
            "Telefonia Personal" to R.drawable.personal_logo,

            // Cooperativas
            "Cooperativa Universitaria (Prestamos)" to R.drawable.cooperativa_universitaria,
            "Cooperativa Universitaria (Tarjeta Mastercard)" to R.drawable.cooperativa_universitaria,
            "Cooperativa Universitaria (Tarjeta Cabal)" to R.drawable.cooperativa_universitaria,
            "Cooperativa Universitaria (Tarjeta Panal)" to R.drawable.cooperativa_universitaria,

            "Cooperativa Medalla Milagrosa (Tarjeta Visa)" to R.drawable.cooperativa_medalla_milagrosa,
            "Cooperativa Medalla Milagrosa (Solidaridad)" to R.drawable.cooperativa_medalla_milagrosa,
            "Cooperativa Medalla Milagrosa (Tarjeta Mastercard)" to R.drawable.cooperativa_medalla_milagrosa,
            "Cooperativa Medalla Milagrosa (Tarjeta Credicard)" to R.drawable.cooperativa_medalla_milagrosa,
            "Cooperativa Medalla Milagrosa (Tarjeta Cabal)" to R.drawable.cooperativa_medalla_milagrosa,
            "Cooperativa Medalla Milagrosa (Creditos)" to R.drawable.cooperativa_medalla_milagrosa,

            "Cooperativa Coomecipar (Solidaridad)" to R.drawable.cooperativa_comecipar,
            "Cooperativa Coomecipar (Prestamo)" to R.drawable.cooperativa_comecipar,
            "Cooperativa Coomecipar (Tarjeta Mastercard)" to R.drawable.cooperativa_comecipar,
            "Cooperativa Coomecipar (Tarjeta Credicard)" to R.drawable.cooperativa_comecipar,
            "Cooperativa Coomecipar (Tarjeta Cabal)" to R.drawable.cooperativa_comecipar,
            "Cooperativa Coomecipar (Aportes)" to R.drawable.cooperativa_comecipar,

            // Ver más
            "Ver más" to R.drawable.ic_chevron_right
        )

        // Service colors mapping
        val serviceColors = mapOf(
            // Tigo - Azul
            "Giros Tigo" to R.color.service_tigo,
            "Retiros Tigo" to R.color.service_tigo,
            "Carga Billetera Tigo" to R.color.service_tigo,
            "Telefonia Tigo" to R.color.service_tigo,
            "Pago TV e Internet Hogar" to R.color.service_tigo,
            "Antena (Wimax)" to R.color.service_tigo,
            "Tigo TV anticipado" to R.color.service_tigo,
            "Reseteo de Cliente" to R.color.service_tigo,

            // Servicios básicos
            "ANDE" to R.color.service_ande,
            "ESSAP" to R.color.status_info,
            "COPACO" to R.color.status_success,

            // Personal - Rojo
            "Retiros Personal" to R.color.service_personal,
            "Telefonia Personal" to R.color.service_personal,

            // Ver más
            "Ver más" to R.color.md_theme_light_tertiary
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
                    val service = services[position]
                    if (service.name == "Ver más") {
                        onViewMoreClick()
                    } else {
                        onItemClick(service)
                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (services[position].name == "Ver más") TYPE_VIEW_MORE else TYPE_SERVICE
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

            // Set icon - CORRECCIÓN: Manejar correctamente iconos vs imágenes
            try {
                val context = holder.itemView.context
                holder.serviceIcon.setImageResource(service.icon)

                // Verificar si es un icono vector (ic_) o una imagen
                val resourceName = context.resources.getResourceName(service.icon)
                val isVectorIcon = resourceName.contains("/ic_")

                Log.d("ServiceAdapter", "Loading ${service.name}: resource=$resourceName, isVector=$isVectorIcon")

                if (isVectorIcon) {
                    // Para iconos vectoriales: aplicar tint blanco y fondo con color
                    val color = ContextCompat.getColor(context, service.color)
                    holder.iconBackground.background?.setTint(color)
                    holder.serviceIcon.setColorFilter(
                        ContextCompat.getColor(context, android.R.color.white),
                        android.graphics.PorterDuff.Mode.SRC_IN
                    )
                } else {
                    // Para imágenes: NO aplicar tint, mantener colores originales
                    holder.iconBackground.background?.setTint(
                        ContextCompat.getColor(context, R.color.md_theme_light_surface)
                    )
                    holder.serviceIcon.clearColorFilter()
                }

                // Ajustar escala para mejor visualización
                holder.serviceIcon.scaleType = ImageView.ScaleType.FIT_CENTER
                holder.serviceIcon.adjustViewBounds = true

            } catch (e: Exception) {
                Log.e("ServiceAdapter", "Error loading icon for ${service.name}", e)
                holder.serviceIcon.setImageResource(R.drawable.ic_service_default)
                holder.serviceIcon.clearColorFilter()
            }

            // Add animation
            holder.itemView.alpha = 0f
            holder.itemView.animate()
                .alpha(1f)
                .setDuration(200)
                .setStartDelay((position * 30).toLong())
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