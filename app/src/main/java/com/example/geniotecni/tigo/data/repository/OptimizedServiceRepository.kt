package com.example.geniotecni.tigo.data.repository

import com.example.geniotecni.tigo.R
import com.example.geniotecni.tigo.models.ServiceConfig
import com.example.geniotecni.tigo.ui.adapters.ServiceItem
import com.example.geniotecni.tigo.utils.Constants
import com.example.geniotecni.tigo.utils.OptimizedServiceConfiguration
import com.example.geniotecni.tigo.utils.OptimizedServiceConfiguration.ServiceCategory

/**
 * Optimized service repository using functional patterns and caching.
 * Dramatically reduces code complexity and improves performance.
 */
class OptimizedServiceRepository private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: OptimizedServiceRepository? = null
        
        fun getInstance(): OptimizedServiceRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OptimizedServiceRepository().also { INSTANCE = it }
            }
        }
    }
    
    // Lazy-loaded cache for performance optimization
    private val serviceCache: Map<Int, ServiceItem> by lazy {
        Constants.SERVICE_NAMES.mapIndexed { index, _ ->
            index to createServiceItem(index)
        }.toMap()
    }
    
    private val viewMoreItem: ServiceItem by lazy {
        ServiceItem(
            id = -1,
            name = "Ver más",
            description = "Mostrar todos los servicios disponibles",
            icon = R.drawable.ic_chevron_right,
            color = R.color.md_theme_light_tertiary,
            isActive = true
        )
    }
    
    /**
     * Functional service item creation with fallback
     */
    private fun createServiceItem(serviceId: Int): ServiceItem {
        return OptimizedServiceConfiguration.getServiceItem(serviceId)
    }
    
    /**
     * Public API with functional composition
     */
    
    // Core service access with caching
    fun getServiceById(serviceId: Int): ServiceItem {
        return serviceCache[serviceId] ?: createFallbackServiceItem(serviceId)
    }
    
    fun getAllServices(): List<ServiceItem> = serviceCache.values.toList()
    
    // Functional service filtering
    fun searchServices(query: String): List<ServiceItem> {
        return if (query.isBlank()) {
            getAllServices()
        } else {
            serviceCache.values.filter { service ->
                service.name.contains(query, ignoreCase = true) ||
                service.description.contains(query, ignoreCase = true)
            }
        }
    }
    
    // Smart service grouping with "Ver más" functionality
    fun getServicesWithViewMore(limitIndex: Int): List<ServiceItem> {
        val limitedServices = serviceCache.values
            .filter { it.id <= limitIndex }
            .sortedBy { it.id }
            .toMutableList()
        
        limitedServices.add(viewMoreItem)
        return limitedServices
    }
    
    // Category-based filtering using functional composition
    fun getServicesByCategory(category: ServiceCategory): List<ServiceItem> {
        return OptimizedServiceConfiguration.searchServices("")
            .filter { it.category == category }
            .map { config ->
                ServiceItem(
                    id = config.id,
                    name = config.name,
                    description = config.description,
                    icon = config.icon,
                    color = config.color,
                    isActive = true
                )
            }
    }
    
    // Delegation to optimized configuration
    fun getServiceConfig(serviceId: Int): ServiceConfig {
        return OptimizedServiceConfiguration.getServiceConfig(serviceId)
    }
    
    fun getServiceName(serviceId: Int): String {
        return serviceCache[serviceId]?.name ?: OptimizedServiceConfiguration.getServiceName(serviceId)
    }
    
    fun getServiceDescription(serviceId: Int): String {
        return serviceCache[serviceId]?.description ?: OptimizedServiceConfiguration.getServiceDescription(serviceId)
    }
    
    fun getServiceIcon(serviceId: Int): Int {
        return serviceCache[serviceId]?.icon ?: OptimizedServiceConfiguration.getServiceIcon(serviceId)
    }
    
    fun getServiceColor(serviceId: Int): Int {
        return serviceCache[serviceId]?.color ?: OptimizedServiceConfiguration.getServiceColor(serviceId)
    }
    
    fun hasUSSDSupport(serviceId: Int): Boolean {
        return OptimizedServiceConfiguration.hasUSSDSupport(serviceId)
    }
    
    fun generateUSSDCode(serviceId: Int, params: Map<String, String>): String? {
        return OptimizedServiceConfiguration.generateUSSDCode(serviceId, params)
    }
    
    // Utility functions with functional composition
    fun getServiceCount(): Int = Constants.SERVICE_NAMES.size
    
    fun serviceExists(serviceId: Int): Boolean = serviceId in 0 until getServiceCount()
    
    fun findServiceIdByName(serviceName: String): Int {
        return Constants.SERVICE_NAMES.indexOfFirst { 
            it.equals(serviceName, ignoreCase = true) 
        }
    }
    
    fun getPrintLabel(serviceId: Int, fieldName: String): String {
        return OptimizedServiceConfiguration.getPrintLabel(serviceId, fieldName)
    }
    
    fun getPrintLabelByServiceName(serviceName: String, fieldName: String): String {
        return OptimizedServiceConfiguration.getPrintLabelByServiceName(serviceName, fieldName)
    }
    
    // Functional string conversion with caching
    fun convertStringsToServiceItems(serviceNames: List<String>): List<ServiceItem> {
        return serviceNames.map { name ->
            when (name) {
                "Ver más" -> viewMoreItem
                else -> {
                    val serviceId = findServiceIdByName(name)
                    if (serviceId >= 0) {
                        getServiceById(serviceId)
                    } else {
                        createFallbackServiceItem(serviceNames.indexOf(name), name)
                    }
                }
            }
        }
    }
    
    // Fallback service creation
    private fun createFallbackServiceItem(id: Int, name: String = "Servicio $id"): ServiceItem {
        return ServiceItem(
            id = id,
            name = name,
            description = "Servicio disponible",
            icon = R.drawable.ic_service_default,
            color = R.color.md_theme_light_primary,
            isActive = true
        )
    }
    
    // Cache management
    fun clearCache() {
        // Cache is lazy-loaded, so clearing requires recreation
        // This could be implemented with a mutable cache if needed
    }
    
    // Performance monitoring
    fun getCacheSize(): Int = serviceCache.size
    
    // Batch operations for performance
    fun getServicesBatch(serviceIds: List<Int>): List<ServiceItem> {
        return serviceIds.mapNotNull { serviceCache[it] }
    }
    
    // Functional filtering with predicates
    fun filterServices(predicate: (ServiceItem) -> Boolean): List<ServiceItem> {
        return serviceCache.values.filter(predicate)
    }
    
    // Service validation
    fun validateService(serviceItem: ServiceItem): Boolean {
        return serviceItem.name.isNotEmpty() && 
               serviceItem.description.isNotEmpty() &&
               serviceItem.id >= 0
    }
}