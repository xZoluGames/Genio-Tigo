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
            // Add more descriptions as needed
        )

        // Service icons mapping using actual drawables from old code
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
            "Cooperativa Medalla Milagrosa (Creditos)" to R.drawable.cooperativa_medalla_milagrosa
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
            // Add more colors as needed
        )
        
        // Extension function to create ServiceItems from service names
        fun List<String>.toServiceItems(): List<ServiceItem> {
            return this.mapIndexed { index, name ->
                ServiceItem(
                    name = name,
                    description = serviceDescriptions[name] ?: "Servicio disponible",
                    icon = serviceIcons[name] ?: R.drawable.ic_money,
                    colorResId = serviceColors[name] ?: R.color.md_theme_light_primary,
                    serviceType = index
                )
            }
        }
    }

    private var showingAll = false
    private var isSearchMode = false
    private val displayLimit = 8 // Show first 8 services initially

    init {
        AppLogger.i("ServiceAdapter", "ServiceAdapter inicializado con ${services.size} servicios")
        AppLogger.logMemoryUsage("ServiceAdapter", "Inicialización")
        updateDisplayedServices()
    }

    override fun getItemViewType(position: Int): Int {
        val displayedServiceCount = if (showingAll || isSearchMode) services.size else minOf(services.size, displayLimit)
        return if (position == displayedServiceCount && !showingAll && !isSearchMode && services.size > displayLimit) {
            TYPE_LOAD_MORE
        } else {
            TYPE_SERVICE
        }
    }

    override fun getItemCount(): Int {
        val displayCount = if (showingAll || isSearchMode) services.size else minOf(services.size, displayLimit)
        return displayCount + if (!showingAll && !isSearchMode && services.size > displayLimit) 1 else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        AppLogger.d("ServiceAdapter", "Creando ViewHolder de tipo: $viewType")
        return when (viewType) {
            TYPE_SERVICE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_service, parent, false)
                AppLogger.d("ServiceAdapter", "ServiceViewHolder creado")
                ServiceViewHolder(view)
            }
            TYPE_LOAD_MORE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_load_more, parent, false)
                AppLogger.d("ServiceAdapter", "LoadMoreViewHolder creado")
                LoadMoreViewHolder(view)
            }
            else -> {
                AppLogger.e("ServiceAdapter", "Tipo de vista inválido: $viewType")
                throw IllegalArgumentException("Invalid view type")
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ServiceViewHolder -> {
                val service = if (showingAll || isSearchMode) {
                    services[position]
                } else {
                    // Ensure we don't exceed the display limit or available services
                    val limitedServices = services.take(displayLimit)
                    if (position < limitedServices.size) {
                        limitedServices[position]
                    } else {
                        return // Should not happen, but safety check
                    }
                }
                holder.bind(service)
            }
            is LoadMoreViewHolder -> {
                holder.bind()
            }
        }
    }

    fun updateServices(newServiceNames: List<String>) {
        AppLogger.i("ServiceAdapter", "Actualizando servicios: ${newServiceNames.size} elementos")
        val oldSize = services.size
        isSearchMode = newServiceNames.size != services.size
        AppLogger.d("ServiceAdapter", "Modo búsqueda: $isSearchMode (antes: $oldSize, ahora: ${newServiceNames.size})")

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
        val diffStart = System.currentTimeMillis()
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        val diffTime = System.currentTimeMillis() - diffStart
        AppLogger.logDataProcessing("ServiceAdapter", "Cálculo DiffUtil", "ServiceItems", newServices.size, diffTime)

        services = newServices
        updateDisplayedServices()
        diffResult.dispatchUpdatesTo(this)
        AppLogger.i("ServiceAdapter", "Servicios actualizados exitosamente")
    }

    private fun updateDisplayedServices() {
        // This is now handled in getItemCount and onBindViewHolder
    }

    private fun showAllServices() {
        AppLogger.logUserAction("ServiceAdapter", "Mostrar todos los servicios", "${services.size} servicios")
        showingAll = true
        notifyDataSetChanged()
        AppLogger.i("ServiceAdapter", "Mostrando todos los servicios (${services.size} elementos)")
    }

    inner class ServiceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val card: MaterialCardView = view as MaterialCardView
        private val iconBackground: View = view.findViewById(R.id.iconBackground)
        private val serviceIcon: ImageView = view.findViewById(R.id.serviceIcon)
        private val serviceName: TextView = view.findViewById(R.id.serviceName)
        private val serviceDescription: TextView = view.findViewById(R.id.serviceDescription)

        fun bind(service: ServiceItem) {
            val bindStart = System.currentTimeMillis()
            AppLogger.d("ServiceAdapter", "=== INICIANDO BIND: ${service.name} ===")
            AppLogger.d("ServiceAdapter", "Recurso icono: ${service.icon}")
            AppLogger.d("ServiceAdapter", "Recurso color: ${service.colorResId}")
            AppLogger.d("ServiceAdapter", "Descripción: ${service.description}")
            
            serviceName.text = service.name
            serviceDescription.text = service.description
            AppLogger.d("ServiceAdapter", "Textos asignados para ${service.name}")
            
            // Log drawable loading
            try {
                val imageStart = System.currentTimeMillis()
                val drawable = ContextCompat.getDrawable(itemView.context, service.icon)
                val imageTime = System.currentTimeMillis() - imageStart
                AppLogger.logImageLoad("ServiceAdapter", service.name, service.icon, true, imageTime)
                serviceIcon.setImageResource(service.icon)
                AppLogger.d("ServiceAdapter", "Icono configurado exitosamente para ${service.name}")
            } catch (e: Exception) {
                AppLogger.logImageLoad("ServiceAdapter", service.name, service.icon, false)
                AppLogger.e("ServiceAdapter", "Error cargando icono para ${service.name}", e)
                // Fallback icon
                serviceIcon.setImageResource(R.drawable.ic_money)
                AppLogger.i("ServiceAdapter", "Icono fallback aplicado para ${service.name}")
            }

            // Set icon background color
            try {
                val colorStart = System.currentTimeMillis()
                val color = ContextCompat.getColor(itemView.context, service.colorResId)
                val colorTime = System.currentTimeMillis() - colorStart
                AppLogger.d("ServiceAdapter", "Color cargado para ${service.name}: $color (${colorTime}ms)")
                iconBackground.background.setTint(color)
                AppLogger.d("ServiceAdapter", "Tinte de fondo aplicado exitosamente para ${service.name}")
            } catch (e: Exception) {
                AppLogger.e("ServiceAdapter", "Error configurando color de fondo para ${service.name}", e)
            }

            // Set click listener
            card.setOnClickListener {
                AppLogger.logUserAction("ServiceAdapter", "Selección de servicio", service.name)
                AppLogger.logServiceSelection("ServiceAdapter", service.name, service.serviceType)
                onItemClick(service)
            }
            
            val bindTime = System.currentTimeMillis() - bindStart
            AppLogger.d("ServiceAdapter", "=== BIND COMPLETADO: ${service.name} (${bindTime}ms) ===")
        }
    }

    inner class LoadMoreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val loadMoreButton: View = view.findViewById(R.id.btn_load_more)

        fun bind() {
            AppLogger.d("ServiceAdapter", "Configurando botón 'Cargar más'")
            loadMoreButton.setOnClickListener {
                AppLogger.logButtonClick("ServiceAdapter", "LoadMoreButton", "Expandir lista de servicios")
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