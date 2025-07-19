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
            "Telefonia Personal" to "Servicios de Personal",
            "Ver más" to "Mostrar servicios adicionales"
        )

        // Service icons mapping - COMPREHENSIVE WITH ALL AVAILABLE DRAWABLES
        val serviceIcons = mapOf(
            "Giros Tigo" to R.drawable.tigo_cuadrado,
            "Retiros Tigo" to R.drawable.billeterapersonal_icon,
            "Carga Billetera Tigo" to R.drawable.billeteratigo_icon,
            "Telefonia Tigo" to R.drawable.tigo_icon,
            "Pago TV e Internet Hogar" to R.drawable.tigo_cuadrado,
            "Antena (Wimax)" to R.drawable.ic_wifi,
            "Tigo TV anticipado" to R.drawable.ic_tv,
            "Reseteo de Cliente" to R.drawable.ic_refresh,
            "ANDE" to R.drawable.ande_icon,
            "ESSAP" to R.drawable.essap_icon,
            "COPACO" to R.drawable.cocapo_icon,
            "Retiros Personal" to R.drawable.personal_icon,
            "Telefonia Personal" to R.drawable.personal_logo,
            "Alex S.A" to R.drawable.alex,
            "Electroban" to R.drawable.electroban,
            "Leopard" to R.drawable.leopard,
            "Chacomer" to R.drawable.chacomer,
            "Inverfin" to R.drawable.inverfin,
            "Che Duo-Carsa (Prestamos)" to R.drawable.che_duo_carsa,
            "Banco Familar (Prestamos)" to R.drawable.banco_familiar,
            "Financiera El Comercio" to R.drawable.financiera_el_comercio,
            "Interfisa (Prestamos)" to R.drawable.interfisa_prestamo,
            "Financiera Paraguayo Japonesa (Prestamos)" to R.drawable.financiera_paraguayo_japonesa,
            "Credito Amigo (Prestamos)" to R.drawable.credito_amigo,
            "Tu Financiera (Prestamos)" to R.drawable.tu_financiera,
            "Funacion Industrial (Prestamos)" to R.drawable.fundacion_industrial,
            "Banco Vision Pago de Tarjetas" to R.drawable.vision_banco,
            "Banco Vision Pago de Prestamos" to R.drawable.vision_banco,
            "Fiado.Net (Prestamos)" to R.drawable.fiado_net,
            "Financiera Solar Pago de Tarjetas" to R.drawable.financiera_solar,
            "Financiera Solar Pago de Prestamos" to R.drawable.financiera_solar,
            "Interfisa Pago de Tarjetas" to R.drawable.interfisa_prestamo,
            "Banco Itau (Prestamos)" to R.drawable.banco_itau,
            "Cooperativa Universitaria (Prestamos)" to R.drawable.universitaria,
            "Cooperativa Universitaria (Tarjeta Mastercard)" to R.drawable.universitaria,
            "Cooperativa Universitaria (Tarjeta Cabal)" to R.drawable.universitaria,
            "Cooperativa Universitaria (Tarjeta Panal)" to R.drawable.universitaria,
            "CopexSanJo (Tarjeta Credito Visa)" to R.drawable.copexsanjo,
            "CopexSanJo (Tarjeta Credito Cabal)" to R.drawable.copexsanjo,
            "CopexSanJo (Solidaridad)" to R.drawable.copexsanjo,
            "CopexSanJo (Cuotas)" to R.drawable.copexsanjo,
            "CopexSanJo (Aportes)" to R.drawable.copexsanjo,
            "Caja Mutual De Cooperativistas Del Paraguay CMCP (Tarjeta Cabal)" to R.drawable.cmcp,
            "Caja Mutual De Cooperativistas Del Paraguay CMCP (Tarjeta Mastercard)" to R.drawable.cmcp,
            "Caja Mutual De Cooperativistas Del Paraguay CMCP (Credito)" to R.drawable.cmcp,
            "Caja Mutual De Cooperativistas Del Paraguay CMCP (Aporte)" to R.drawable.cmcp,
            "Cooperativa Tupãrenda (Aporte y Solidaridad)" to R.drawable.cooperativa_tuparenda,
            "Cooperativa Tupãrenda (Prestamos)" to R.drawable.cooperativa_tuparenda,
            "Cooperativa San Cristobal (Admision)" to R.drawable.cooperativa_san_cristobal,
            "Cooperativa San Cristobal (Tarjeta Mastercard)" to R.drawable.cooperativa_san_cristobal,
            "Cooperativa San Cristobal (Solidaridad)" to R.drawable.cooperativa_san_cristobal,
            "Cooperativa San Cristobal (Aporte)" to R.drawable.cooperativa_san_cristobal,
            "Cooperativa San Cristobal (Prestamo)" to R.drawable.cooperativa_san_cristobal,
            "Cooperativa San Cristobal (Tarjeta Unica)" to R.drawable.cooperativa_san_cristobal,
            "Cooperativa San Cristobal (Tarjeta Visa)" to R.drawable.cooperativa_san_cristobal,
            "Cooperativa San Cristobal (Tarjeta Credicard)" to R.drawable.cooperativa_san_cristobal,
            "Cooperativa Yoayu (Sepelios)" to R.drawable.cooperativa_yoayu,
            "Cooperativa Yoayu (Tarjeta Cabal)" to R.drawable.cooperativa_yoayu,
            "Cooperativa Yoayu (Tarjeta Visa)" to R.drawable.cooperativa_yoayu,
            "Cooperativa Yoayu (Fondos)" to R.drawable.cooperativa_yoayu,
            "Cooperativa Yoayu (Solidaridad)" to R.drawable.cooperativa_yoayu,
            "Cooperativa Yoayu (Aporte)" to R.drawable.cooperativa_yoayu,
            "Cooperativa Yoayu" to R.drawable.cooperativa_yoayu,
            "Cooperativa Coomecipar (Solidaridad)" to R.drawable.cooperativa_comecipar,
            "Cooperativa Coomecipar (Prestamo)" to R.drawable.cooperativa_comecipar,
            "Cooperativa Coomecipar (Tarjeta Mastercard)" to R.drawable.cooperativa_comecipar,
            "Cooperativa Coomecipar (Tarjeta Credicard)" to R.drawable.cooperativa_comecipar,
            "Cooperativa Coomecipar (Tarjeta Cabal)" to R.drawable.cooperativa_comecipar,
            "Cooperativa Coomecipar (Aportes)" to R.drawable.cooperativa_comecipar,
            "Cooperativa Medalla Milagrosa (Tarjeta Visa)" to R.drawable.cooperativa_medalla_milagrosa,
            "Cooperativa Medalla Milagrosa (Solidaridad)" to R.drawable.cooperativa_medalla_milagrosa,
            "Cooperativa Medalla Milagrosa (Tarjeta Mastercard)" to R.drawable.cooperativa_medalla_milagrosa,
            "Cooperativa Medalla Milagrosa (Tarjeta Credicard)" to R.drawable.cooperativa_medalla_milagrosa,
            "Cooperativa Medalla Milagrosa (Tarjeta Cabal)" to R.drawable.cooperativa_medalla_milagrosa,
            "Cooperativa Medalla Milagrosa (Creditos)" to R.drawable.cooperativa_medalla_milagrosa,
            "Ver más" to R.drawable.ic_chevron_right
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
            "Telefonia Personal" to R.color.service_personal,
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

            // Set icon - IMPROVED: Better error handling and logging
            try {
                Log.d("ServiceAdapter", "Loading icon for ${service.name}, resource ID: ${service.icon}")
                holder.serviceIcon.setImageResource(service.icon)
                
                // Set appropriate scale type for better icon display
                holder.serviceIcon.scaleType = ImageView.ScaleType.FIT_CENTER
                holder.serviceIcon.adjustViewBounds = true
                
                AppLogger.d("ServiceAdapter", "Icon loaded successfully for ${service.name}")
            } catch (e: Exception) {
                AppLogger.e("ServiceAdapter", "Error loading icon for ${service.name}, using default", e)
                holder.serviceIcon.setImageResource(R.drawable.ic_service_default)
                holder.serviceIcon.scaleType = ImageView.ScaleType.FIT_CENTER
            }

            // Set background color for icon - IMPROVED: Different handling for ic_ vs images
            try {
                val color = ContextCompat.getColor(holder.itemView.context, service.color)
                
                // Check if it's an ic_ drawable (vector icon) or an image
                val resourceName = holder.itemView.context.resources.getResourceName(service.icon)
                val isVectorIcon = resourceName.contains("ic_")
                
                if (isVectorIcon) {
                    // For ic_ icons: apply white tint and colored background
                    holder.iconBackground.background?.setTint(color)
                    holder.serviceIcon.setColorFilter(
                        ContextCompat.getColor(holder.itemView.context, android.R.color.white),
                        android.graphics.PorterDuff.Mode.SRC_IN
                    )
                    Log.d("ServiceAdapter", "Applied white tint to vector icon: ${service.name}")
                } else {
                    // For image files: keep original colors, light background
                    holder.iconBackground.background?.setTint(
                        ContextCompat.getColor(holder.itemView.context, R.color.md_theme_light_surfaceVariant)
                    )
                    holder.serviceIcon.clearColorFilter()
                    Log.d("ServiceAdapter", "Preserved original colors for image: ${service.name}")
                }
            } catch (e: Exception) {
                AppLogger.e("ServiceAdapter", "Error setting icon styling for ${service.name}", e)
                // Fallback: light background, no tint
                val defaultColor = ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.md_theme_light_surfaceVariant
                )
                holder.iconBackground.background?.setTint(defaultColor)
                holder.serviceIcon.clearColorFilter()
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

