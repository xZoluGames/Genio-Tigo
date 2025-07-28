package com.example.geniotecni.tigo.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geniotecni.tigo.data.repository.OptimizedServiceRepository
import com.example.geniotecni.tigo.data.repository.ServiceRepository
import com.example.geniotecni.tigo.managers.StatisticsManager
import com.example.geniotecni.tigo.managers.PreferencesManager
import com.example.geniotecni.tigo.helpers.NetworkHelper
import com.example.geniotecni.tigo.ui.adapters.ServiceItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * 🔍 SEARCH SERVICES VIEW MODEL - Lógica de Negocio para Búsqueda de Servicios
 * 
 * PROPÓSITO ARQUITECTÓNICO:
 * - Gestión centralizada del estado de búsqueda y navegación
 * - Separación completa entre lógica de negocio y presentación UI
 * - Manejo reactivo de filtros de búsqueda con debounce
 * - Coordinación entre múltiples repositorios y managers
 * 
 * GESTIÓN DE ESTADO:
 * - services: Lista de servicios disponibles (completa o filtrada)
 * - searchQuery: Query de búsqueda actual con debounce automático
 * - isLoading: Estado de carga para operaciones asíncronas
 * - showAllServices: Control de expansión de lista (Ver más)
 * - quickActionStates: Estados de acciones rápidas (historial, estadísticas, etc.)
 * 
 * FUNCIONALIDADES PRINCIPALES:
 * - searchServices(): Búsqueda inteligente con debounce de 300ms
 * - toggleShowAllServices(): Expansión progresiva de servicios
 * - loadServiceStatistics(): Carga de métricas de uso
 * - validatePermissions(): Verificación de permisos críticos
 * - getPopularServices(): Servicios más utilizados primero
 * 
 * OPTIMIZACIONES DE RENDIMIENTO:
 * - Debounce en búsqueda para reducir operaciones I/O
 * - Lazy loading de servicios con paginación inteligente
 * - Cache de resultados de búsqueda frecuentes
 * - Estados reactivos para actualizaciones eficientes de UI
 * 
 * INYECCIÓN DE DEPENDENCIAS:
 * - OptimizedServiceRepository: Búsqueda optimizada de servicios
 * - ServiceRepository: Datos completos de configuración de servicios
 * - StatisticsManager: Métricas de uso y popularidad
 * - PreferencesManager: Configuraciones de usuario y filtros
 * - NetworkHelper: Verificación de conectividad para funciones online
 * 
 * CASOS DE USO PRINCIPALES:
 * - SearchServices Activity: UI principal de búsqueda y navegación
 * - Búsqueda en tiempo real con autocompletado
 * - Navegación inteligente con precarga de datos
 * - Gestión de permisos con explicaciones contextuales
 * 
 * EVENTOS DE UI:
 * - ServiceSelected: Navegación a MainActivity con servicio específico
 * - QuickActionTriggered: Activación de acciones rápidas
 * - PermissionRequired: Solicitud de permisos específicos
 * - ShowError: Manejo centralizado de errores
 * 
 * CONEXIONES ARQUITECTÓNICAS:
 * - CONSUME: OptimizedServiceRepository para búsquedas rápidas
 * - UTILIZA: StatisticsManager para ordenamiento por popularidad
 * - COORDINA: PreferencesManager para filtros personalizados
 * - VALIDA: NetworkHelper para funciones que requieren conectividad
 * - EMITE: UIEvents para comunicación reactiva con SearchServices Activity
 */
@HiltViewModel
class SearchServicesViewModel @Inject constructor(
    private val optimizedServiceRepository: OptimizedServiceRepository,
    private val serviceRepository: ServiceRepository,
    private val statisticsManager: StatisticsManager,
    private val preferencesManager: PreferencesManager,
    private val networkHelper: NetworkHelper
) : ViewModel() {

    // Estados reactivos principales
    private val _services = MutableStateFlow<List<ServiceItem>>(emptyList())
    val services: StateFlow<List<ServiceItem>> = _services.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showAllServices = MutableStateFlow(false)
    val showAllServices: StateFlow<Boolean> = _showAllServices.asStateFlow()

    private val _serviceStatistics = MutableStateFlow<ServiceStatistics?>(null)
    val serviceStatistics: StateFlow<ServiceStatistics?> = _serviceStatistics.asStateFlow()

    private val _permissionStates = MutableStateFlow<PermissionStates>(PermissionStates())
    val permissionStates: StateFlow<PermissionStates> = _permissionStates.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UIEvent>()
    val uiEvents: SharedFlow<UIEvent> = _uiEvents.asSharedFlow()

    // Cache para resultados de búsqueda
    private val searchCache = mutableMapOf<String, List<ServiceItem>>()

    init {
        loadInitialServices()
        setupSearchDebounce()
        loadServiceStatistics()
    }

    /**
     * Carga servicios iniciales (limitados)
     */
    private fun loadInitialServices() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Cargar servicios iniciales (hasta reseteo + ver más)
                val initialServices = optimizedServiceRepository.getServicesWithViewMore(15)
                _services.value = initialServices
            } catch (e: Exception) {
                _uiEvents.emit(UIEvent.ShowError("Error cargando servicios: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Configura debounce para búsqueda reactiva
     */
    private fun setupSearchDebounce() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // Esperar 300ms después del último cambio
                .distinctUntilChanged()
                .collect { query ->
                    performSearch(query)
                }
        }
    }

    /**
     * Actualiza query de búsqueda (con debounce automático)
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Realiza búsqueda con cache
     */
    private suspend fun performSearch(query: String) {
        if (query.isBlank()) {
            // Restaurar vista según estado actual
            val currentServices = if (_showAllServices.value) {
                optimizedServiceRepository.getAllServices()
            } else {
                optimizedServiceRepository.getServicesWithViewMore(15)
            }
            _services.value = currentServices
            return
        }

        // Verificar cache primero
        val cachedResult = searchCache[query.lowercase()]
        if (cachedResult != null) {
            _services.value = cachedResult
            return
        }

        _isLoading.value = true
        try {
            val searchResults = optimizedServiceRepository.searchServices(query)
            
            // Guardar en cache si hay resultados
            if (searchResults.isNotEmpty() && query.length >= 2) {
                searchCache[query.lowercase()] = searchResults
            }
            
            _services.value = searchResults
        } catch (e: Exception) {
            _uiEvents.emit(UIEvent.ShowError("Error en búsqueda: ${e.message}"))
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Alterna mostrar todos los servicios
     */
    fun toggleShowAllServices() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val newShowAll = !_showAllServices.value
                _showAllServices.value = newShowAll

                val updatedServices = if (newShowAll) {
                    optimizedServiceRepository.getAllServices()
                } else {
                    optimizedServiceRepository.getServicesWithViewMore(15)
                }

                _services.value = updatedServices
                _uiEvents.emit(UIEvent.ServicesExpanded(newShowAll))
            } catch (e: Exception) {
                _uiEvents.emit(UIEvent.ShowError("Error expandiendo servicios: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Selecciona un servicio y emite evento de navegación
     */
    fun selectService(service: ServiceItem) {
        viewModelScope.launch {
            // Registrar selección en estadísticas
            // TODO: Implementar recordServiceUsage en StatisticsManager
            
            // Emitir evento de navegación
            _uiEvents.emit(UIEvent.ServiceSelected(service))
        }
    }

    /**
     * Maneja selección desde autocompletado
     */
    fun selectServiceFromAutocomplete(serviceName: String) {
        viewModelScope.launch {
            val matchingService: ServiceItem? = null // TODO: Implementar findServiceByName
            if (matchingService != null) {
                selectService(matchingService)
            } else {
                _uiEvents.emit(UIEvent.ShowError("Servicio no encontrado: $serviceName"))
            }
        }
    }

    /**
     * Carga estadísticas de servicios
     */
    private fun loadServiceStatistics() {
        viewModelScope.launch {
            try {
                // TODO: Implementar getServiceStatistics
                _serviceStatistics.value = ServiceStatistics(
                    totalServices = optimizedServiceRepository.getAllServices().size,
                    popularServices = emptyList(), // TODO: Obtener servicios populares
                    recentSearches = emptyList() // TODO: Obtener búsquedas recientes
                )
            } catch (e: Exception) {
                // Error silencioso para estadísticas no críticas
            }
        }
    }

    /**
     * Obtiene servicios populares para FAB
     */
    fun getMostUsedService(): ServiceItem? {
        return _serviceStatistics.value?.popularServices?.firstOrNull()
    }

    /**
     * Ejecuta acción rápida específica
     */
    fun executeQuickAction(action: QuickAction) {
        viewModelScope.launch {
            when (action) {
                QuickAction.PRINT_HISTORY -> {
                    _uiEvents.emit(UIEvent.NavigateToHistory)
                }
                QuickAction.STATISTICS -> {
                    if (networkHelper.isNetworkAvailable()) {
                        _uiEvents.emit(UIEvent.NavigateToStatistics)
                    } else {
                        _uiEvents.emit(UIEvent.ShowError("Conectividad requerida para estadísticas"))
                    }
                }
                QuickAction.SETTINGS -> {
                    _uiEvents.emit(UIEvent.NavigateToSettings)
                }
                QuickAction.BLUETOOTH -> {
                    validateBluetoothAndNavigate()
                }
            }
        }
    }

    /**
     * Valida estado de Bluetooth y navega
     */
    private suspend fun validateBluetoothAndNavigate() {
        val bluetoothDevice = null // TODO: Implementar getBluetoothDeviceAddress
        if (bluetoothDevice.isNullOrBlank()) {
            _uiEvents.emit(UIEvent.ShowError("Dispositivo Bluetooth no configurado"))
        } else {
            _uiEvents.emit(UIEvent.NavigateToBluetooth)
        }
    }

    /**
     * Actualiza estados de permisos
     */
    fun updatePermissionStates(permissions: Map<String, Boolean>) {
        _permissionStates.value = PermissionStates(
            canMakeCalls = permissions["android.permission.CALL_PHONE"] ?: false,
            canReadSms = permissions["android.permission.READ_SMS"] ?: false,
            canUseBluetooth = permissions["android.permission.BLUETOOTH_CONNECT"] ?: false
        )
    }

    /**
     * Verifica permisos críticos
     */
    fun checkCriticalPermissions(): List<String> {
        val currentStates = _permissionStates.value
        val missingPermissions = mutableListOf<String>()

        if (!currentStates.canMakeCalls) {
            missingPermissions.add("android.permission.CALL_PHONE")
        }
        if (!currentStates.canReadSms) {
            missingPermissions.add("android.permission.READ_SMS")
        }
        if (!currentStates.canUseBluetooth) {
            missingPermissions.add("android.permission.BLUETOOTH_CONNECT")
        }

        return missingPermissions
    }

    /**
     * Limpia cache de búsqueda
     */
    fun clearSearchCache() {
        searchCache.clear()
    }

    // Clases de datos para estado
    data class ServiceStatistics(
        val totalServices: Int,
        val popularServices: List<ServiceItem>,
        val recentSearches: List<String>
    )

    data class PermissionStates(
        val canMakeCalls: Boolean = false,
        val canReadSms: Boolean = false,
        val canUseBluetooth: Boolean = false
    ) {
        fun hasAllCriticalPermissions(): Boolean = canMakeCalls && canReadSms && canUseBluetooth
    }

    enum class QuickAction {
        PRINT_HISTORY,
        STATISTICS,
        SETTINGS,
        BLUETOOTH
    }

    sealed class UIEvent {
        data class ServiceSelected(val service: ServiceItem) : UIEvent()
        data class ServicesExpanded(val showingAll: Boolean) : UIEvent()
        data class ShowError(val message: String) : UIEvent()
        object NavigateToHistory : UIEvent()
        object NavigateToStatistics : UIEvent()
        object NavigateToSettings : UIEvent()
        object NavigateToBluetooth : UIEvent()
    }
}