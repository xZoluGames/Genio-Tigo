package com.example.geniotecni.tigo.data.repository

import com.example.geniotecni.tigo.models.ServiceConfig
import com.example.geniotecni.tigo.ui.adapters.ServiceItem
import com.example.geniotecni.tigo.utils.Constants
import com.example.geniotecni.tigo.utils.OptimizedServiceConfiguration

/**
 * 🏦 REPOSITORIO DE SERVICIOS - Patrón Singleton para Acceso Centralizado
 * 
 * PROPÓSITO ARQUITECTÓNICO:
 * - Abstrae completamente el acceso a configuraciones de servicios
 * - Proporciona interfaz unificada y consistente para toda la aplicación
 * - Implementa caché inteligente y optimizaciones de rendimiento
 * - Actúa como única fuente de verdad para datos de servicios
 * 
 * ARQUITECTURA IMPLEMENTADA:
 * - Singleton thread-safe para acceso global controlado
 * - Delegación inteligente a OptimizedServiceConfiguration
 * - Interfaz consistente independiente de implementación interna
 * - Abstracción completa de la complejidad de configuración
 * 
 * RESPONSABILIDADES PRINCIPALES:
 * - Búsqueda, filtrado y categorización de servicios
 * - Conversión automática entre diferentes formatos de datos
 * - Validación de IDs de servicios y existencia
 * - Generación de códigos USSD específicos por servicio
 * - Gestión de metadatos y configuraciones avanzadas
 * 
 * CONEXIONES CRÍTICAS:
 * - CONSUME: OptimizedServiceConfiguration para datos y configuraciones reales
 * - CONSUME: Constants para definiciones base y constantes del sistema
 * - PRODUCE: ServiceItem para consumo directo de componentes UI
 * - USADO POR: MainActivity, USSDIntegrationHelper, y otros managers
 * 
 * OPTIMIZACIONES:
 * - Lazy loading de configuraciones pesadas
 * - Caché inmutable para thread-safety
 * - Métodos optimizados para búsquedas frecuentes
 */
class ServiceRepository private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: ServiceRepository? = null
        
        fun getInstance(): ServiceRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ServiceRepository().also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Get all services as ServiceItem list
     */
    fun getAllServices(): List<ServiceItem> {
        return OptimizedServiceConfiguration.getAllServiceItems()
    }
    
    /**
     * Get limited services (up to a certain index) with "Ver más" option
     */
    fun getServicesWithViewMore(limitIndex: Int): List<ServiceItem> {
        val limitedServices = Constants.SERVICE_NAMES.toList()
            .take(limitIndex + 1)
            .mapIndexed { index, _ -> getServiceById(index) }
            .toMutableList()
        
        // Add "Ver más" item
        limitedServices.add(createViewMoreItem())
        return limitedServices
    }
    
    /**
     * Get service by ID
     */
    fun getServiceById(serviceId: Int): ServiceItem {
        return OptimizedServiceConfiguration.getServiceItem(serviceId)
    }
    
    /**
     * Search services by query
     */
    fun searchServices(query: String): List<ServiceItem> {
        if (query.isBlank()) return getAllServices()
        
        return Constants.SERVICE_NAMES
            .mapIndexed { index, name -> index to name }
            .filter { (_, name) -> name.contains(query, ignoreCase = true) }
            .map { (index, _) -> getServiceById(index) }
    }
    
    /**
     * Get services by category
     */
    fun getServicesByCategory(category: OptimizedServiceConfiguration.ServiceCategory): List<ServiceItem> {
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
    
    /**
     * Get service configuration (legacy method)
     */
    fun getServiceConfiguration(serviceId: Int): ServiceConfig {
        return OptimizedServiceConfiguration.getServiceConfig(serviceId)
    }
    
    /**
     * Get service name
     */
    fun getServiceName(serviceId: Int): String {
        return OptimizedServiceConfiguration.getServiceName(serviceId)
    }
    
    /**
     * Get service description
     */
    fun getServiceDescription(serviceId: Int): String {
        return OptimizedServiceConfiguration.getServiceDescription(serviceId)
    }
    
    /**
     * Get service icon resource ID
     */
    fun getServiceIcon(serviceId: Int): Int {
        return OptimizedServiceConfiguration.getServiceIcon(serviceId)
    }
    
    /**
     * Get service color resource ID
     */
    fun getServiceColor(serviceId: Int): Int {
        return OptimizedServiceConfiguration.getServiceColor(serviceId)
    }
    
    /**
     * Check if service has USSD support
     */
    fun hasUSSDSupport(serviceId: Int): Boolean {
        return OptimizedServiceConfiguration.hasUSSDSupport(serviceId)
    }
    
    /**
     * FASE 9: Generate USSD code for service using centralized configuration
     */
    fun generateUSSDCode(serviceId: Int, params: Map<String, String>): String? {
        // FASE 9: Usar USSDConfiguration para generar códigos USSD
        return com.example.geniotecni.tigo.utils.USSDConfiguration.generateUSSDCode(serviceId, params)
    }
    
    /**
     * Get total number of services
     */
    fun getServiceCount(): Int {
        return Constants.SERVICE_NAMES.size
    }
    
    /**
     * Check if service exists
     */
    fun serviceExists(serviceId: Int): Boolean {
        return serviceId >= 0 && serviceId < Constants.SERVICE_NAMES.size
    }
    
    /**
     * Find service ID by name
     */
    fun findServiceIdByName(serviceName: String): Int {
        return Constants.SERVICE_NAMES.indexOfFirst { 
            it.equals(serviceName, ignoreCase = true) 
        }
    }
    
    /**
     * Get print label for a service field
     */
    fun getPrintLabel(serviceId: Int, fieldName: String): String {
        return OptimizedServiceConfiguration.getPrintLabel(serviceId, fieldName)
    }
    
    /**
     * Get print label by service name (for backward compatibility)
     */
    fun getPrintLabelByServiceName(serviceName: String, fieldName: String): String {
        return OptimizedServiceConfiguration.getPrintLabelByServiceName(serviceName, fieldName)
    }
    
    /**
     * FASE 9: Get service configuration (ServiceConfig) by ID
     */
    fun getServiceConfig(serviceId: Int): ServiceConfig? {
        return OptimizedServiceConfiguration.getServiceConfiguration(serviceId)?.config
    }
    
    /**
     * FASE 9: Check if service requires alternative SIM (Personal uses SIM 2)
     */
    fun requiresAlternativeSIM(serviceId: Int): Boolean {
        return com.example.geniotecni.tigo.utils.USSDConfiguration.getRequiredSIM(serviceId)?.index == 1
    }
    
    /**
     * Convert string list to ServiceItem list
     */
    fun convertStringsToServiceItems(serviceNames: List<String>): List<ServiceItem> {
        return serviceNames.mapIndexed { index, name ->
            if (name == "Ver más") {
                createViewMoreItem()
            } else {
                // Find the actual service ID by name
                val serviceId = findServiceIdByName(name)
                if (serviceId >= 0) {
                    getServiceById(serviceId)
                } else {
                    // Fallback for unknown services
                    createFallbackServiceItem(index, name)
                }
            }
        }
    }
    
    /**
     * Create "Ver más" service item
     */
    private fun createViewMoreItem(): ServiceItem {
        return ServiceItem(
            id = -1, // Special ID for "Ver más"
            name = "Ver más",
            description = "Mostrar todos los servicios disponibles",
            icon = com.example.geniotecni.tigo.R.drawable.ic_chevron_right,
            color = com.example.geniotecni.tigo.R.color.md_theme_light_tertiary,
            isActive = true
        )
    }
    
    /**
     * Create fallback service item for unknown services
     */
    private fun createFallbackServiceItem(id: Int, name: String): ServiceItem {
        return ServiceItem(
            id = id,
            name = name,
            description = "Servicio disponible",
            icon = com.example.geniotecni.tigo.R.drawable.ic_service_default,
            color = com.example.geniotecni.tigo.R.color.md_theme_light_primary,
            isActive = true
        )
    }
    
    /**
     * Cache management (if needed in future)
     */
    fun clearCache() {
        // Implementation for cache clearing if needed
    }
}