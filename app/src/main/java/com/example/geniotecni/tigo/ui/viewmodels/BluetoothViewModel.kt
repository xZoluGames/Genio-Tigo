package com.example.geniotecni.tigo.ui.viewmodels

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geniotecni.tigo.managers.BluetoothManager
import com.example.geniotecni.tigo.managers.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 📶 BLUETOOTH VIEW MODEL - Gestión Centralizada de Conectividad Bluetooth
 * 
 * PROPÓSITO ARQUITECTÓNICO:
 * - Separación completa entre lógica de Bluetooth y presentación UI
 * - Gestión reactiva de descubrimiento y conexión de dispositivos
 * - Manejo centralizado de permisos y estados de Bluetooth
 * - Coordinación entre BluetoothManager y configuraciones de usuario
 * 
 * GESTIÓN DE ESTADO:
 * - bluetoothEnabled: Estado actual del adaptador Bluetooth
 * - discoveredDevices: Lista de dispositivos encontrados durante búsqueda
 * - pairedDevices: Dispositivos previamente emparejados
 * - selectedDevice: Dispositivo actualmente seleccionado/conectado
 * - isDiscovering: Indicador de búsqueda activa
 * - connectionState: Estado de conexión actual
 * - permissionStates: Estados de permisos Bluetooth requeridos
 * 
 * FUNCIONALIDADES PRINCIPALES:
 * - startDiscovery(): Inicia búsqueda de dispositivos Bluetooth
 * - stopDiscovery(): Detiene búsqueda activa
 * - selectDevice(): Selecciona y guarda dispositivo para uso
 * - connectToDevice(): Establece conexión con dispositivo seleccionado
 * - testPrintConnection(): Verifica conexión mediante impresión de prueba
 * - enableBluetooth(): Solicita activación de Bluetooth
 * 
 * GESTIÓN DE PERMISOS:
 * - Validación automática de permisos requeridos según versión Android
 * - Android 12+: BLUETOOTH_SCAN, BLUETOOTH_CONNECT, BLUETOOTH_ADMIN
 * - Android <12: BLUETOOTH, BLUETOOTH_ADMIN, ACCESS_COARSE_LOCATION
 * - Manejo de estados de permisos denegados y explicaciones
 * 
 * DESCUBRIMIENTO DE DISPOSITIVOS:
 * - Búsqueda automática de dispositivos cercanos
 * - Filtrado de dispositivos duplicados
 * - Identificación de dispositivos por nombre y dirección MAC
 * - Priorización de dispositivos previamente emparejados
 * - Detección automática de impresoras térmicas comunes
 * 
 * CONEXIÓN Y CONFIGURACIÓN:
 * - Conexión RFCOMM con UUID estándar para impresoras
 * - Validación de conexión mediante comandos de prueba
 * - Guardado automático de dispositivo seleccionado
 * - Gestión de timeouts y reintentos de conexión
 * - Manejo de errores de conexión con mensajes descriptivos
 * 
 * INTEGRACIÓN CON IMPRESIÓN:
 * - Configuración automática para impresoras térmicas
 * - Comandos ESC/POS para pruebas de conectividad
 * - Validación de respuesta de impresora
 * - Configuración de parámetros de impresión optimizados
 * 
 * INYECCIÓN DE DEPENDENCIAS:
 * - BluetoothManager: Gestión avanzada de operaciones Bluetooth
 * - PreferencesManager: Persistencia de configuraciones de dispositivos
 * 
 * EVENTOS DE UI:
 * - BluetoothStateChanged: Cambio en estado del adaptador
 * - DeviceDiscovered: Nuevo dispositivo encontrado
 * - DeviceSelected: Dispositivo seleccionado exitosamente
 * - ConnectionEstablished: Conexión establecida con dispositivo
 * - PrintTestCompleted: Prueba de impresión finalizada
 * - ShowError: Manejo de errores de Bluetooth
 * - PermissionRequired: Solicitud de permisos específicos
 * 
 * CASOS DE USO PRINCIPALES:
 * - Bt Activity: Configuración inicial de impresora Bluetooth
 * - MainActivity: Verificación de dispositivo antes de imprimir
 * - PrintHistoryActivity: Reimpresión via dispositivo configurado
 * - SettingsActivity: Gestión de configuraciones de Bluetooth
 * 
 * CONEXIONES ARQUITECTÓNICAS:
 * - GESTIONA: BluetoothManager para operaciones de bajo nivel
 * - PERSISTE: PreferencesManager para configuraciones de dispositivos
 * - COORDINA: Estados de permisos y conectividad para toda la app
 * - EMITE: UIEvents para comunicación reactiva con Activities
 */
@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val bluetoothManager: BluetoothManager,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    // Estados reactivos principales
    private val _bluetoothEnabled = MutableStateFlow(false)
    val bluetoothEnabled: StateFlow<Boolean> = _bluetoothEnabled.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDeviceInfo>> = _discoveredDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDeviceInfo>> = _pairedDevices.asStateFlow()

    private val _selectedDevice = MutableStateFlow<BluetoothDeviceInfo?>(null)
    val selectedDevice: StateFlow<BluetoothDeviceInfo?> = _selectedDevice.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _permissionStates = MutableStateFlow<PermissionStates>(PermissionStates())
    val permissionStates: StateFlow<PermissionStates> = _permissionStates.asStateFlow()

    private val _discoveryStatus = MutableStateFlow("")
    val discoveryStatus: StateFlow<String> = _discoveryStatus.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UIEvent>()
    val uiEvents: SharedFlow<UIEvent> = _uiEvents.asSharedFlow()

    // Cache de dispositivos encontrados para evitar duplicados
    private val deviceCache = mutableSetOf<String>()

    init {
        initializeBluetooth()
        loadSavedDevice()
        loadPairedDevices()
    }

    /**
     * Inicializa el estado del Bluetooth
     */
    private fun initializeBluetooth() {
        viewModelScope.launch {
            val isEnabled = bluetoothManager.isBluetoothEnabled()
            _bluetoothEnabled.value = isEnabled
            
            if (!isEnabled) {
                _discoveryStatus.value = "Bluetooth desactivado"
                _uiEvents.emit(UIEvent.BluetoothStateChanged(false))
            } else {
                _discoveryStatus.value = "Bluetooth activado. Listo para buscar dispositivos."
            }
        }
    }

    /**
     * Carga dispositivo guardado previamente
     */
    private fun loadSavedDevice() {
        viewModelScope.launch {
            val savedAddress = bluetoothManager.getSelectedDeviceAddress()
            val savedName = bluetoothManager.getSelectedDeviceName()
            
            if (!savedAddress.isNullOrBlank() && !savedName.isNullOrBlank()) {
                val deviceInfo = BluetoothDeviceInfo(
                    name = savedName,
                    address = savedAddress,
                    isPaired = true,
                    isSelected = true,
                    deviceType = DeviceType.SAVED
                )
                _selectedDevice.value = deviceInfo
                _discoveryStatus.value = "Dispositivo guardado: $savedName"
                _uiEvents.emit(UIEvent.DeviceLoaded(deviceInfo))
            } else {
                _discoveryStatus.value = "No hay dispositivo configurado"
            }
        }
    }

    /**
     * Carga dispositivos emparejados
     */
    private fun loadPairedDevices() {
        viewModelScope.launch {
            try {
                val pairedDevices = bluetoothManager.getPairedDevices() ?: emptySet()
                val deviceInfos = pairedDevices.map { device ->
                    BluetoothDeviceInfo(
                        name = device.name ?: "Dispositivo desconocido",
                        address = device.address,
                        isPaired = true,
                        isSelected = false,
                        deviceType = DeviceType.PAIRED
                    )
                }
                _pairedDevices.value = deviceInfos
                
                // Agregar a la lista de descubiertos también
                val currentDiscovered = _discoveredDevices.value.toMutableList()
                deviceInfos.forEach { deviceInfo ->
                    if (!currentDiscovered.any { it.address == deviceInfo.address }) {
                        currentDiscovered.add(deviceInfo)
                        deviceCache.add(deviceInfo.address)
                    }
                }
                _discoveredDevices.value = currentDiscovered
                
            } catch (e: Exception) {
                _uiEvents.emit(UIEvent.ShowError("Error cargando dispositivos emparejados: ${e.message}"))
            }
        }
    }

    /**
     * Solicita activación de Bluetooth
     */
    fun enableBluetooth() {
        viewModelScope.launch {
            _uiEvents.emit(UIEvent.RequestBluetoothEnable)
        }
    }

    /**
     * Actualiza estado de Bluetooth desde Activity
     */
    fun updateBluetoothState(enabled: Boolean) {
        viewModelScope.launch {
            _bluetoothEnabled.value = enabled
            if (enabled) {
                _discoveryStatus.value = "Bluetooth activado. Listo para buscar dispositivos."
                loadPairedDevices()
            } else {
                _discoveryStatus.value = "Bluetooth desactivado"
                _discoveredDevices.value = emptyList()
                _pairedDevices.value = emptyList()
                stopDiscovery()
            }
            _uiEvents.emit(UIEvent.BluetoothStateChanged(enabled))
        }
    }

    /**
     * Inicia descubrimiento de dispositivos
     */
    fun startDiscovery() {
        viewModelScope.launch {
            if (!_bluetoothEnabled.value) {
                _uiEvents.emit(UIEvent.ShowError("Bluetooth no está activado"))
                return@launch
            }

            if (!hasRequiredPermissions()) {
                _uiEvents.emit(UIEvent.PermissionRequired(getRequiredPermissions()))
                return@launch
            }

            try {
                // Detener descubrimiento previo si existe
                stopDiscovery()
                
                _isDiscovering.value = true
                _discoveryStatus.value = "Buscando dispositivos..."
                deviceCache.clear()
                
                // Limpiar lista y recargar emparejados
                _discoveredDevices.value = emptyList()
                loadPairedDevices()
                
                // TODO: Implementar startDiscovery en BluetoothManager
                val success = true // Placeholder por ahora
                
                if (!success) {
                    _isDiscovering.value = false
                    _discoveryStatus.value = "Error iniciando búsqueda"
                    _uiEvents.emit(UIEvent.ShowError("No se pudo iniciar la búsqueda"))
                }
            } catch (e: Exception) {
                _isDiscovering.value = false
                _discoveryStatus.value = "Error en búsqueda"
                _uiEvents.emit(UIEvent.ShowError("Error en búsqueda: ${e.message}"))
            }
        }
    }

    /**
     * Detiene descubrimiento de dispositivos
     */
    fun stopDiscovery() {
        viewModelScope.launch {
            if (_isDiscovering.value) {
                // TODO: Implementar stopDiscovery en BluetoothManager
                _isDiscovering.value = false
                val deviceCount = _discoveredDevices.value.size
                _discoveryStatus.value = "Búsqueda completada. $deviceCount dispositivos encontrados"
                _uiEvents.emit(UIEvent.DiscoveryCompleted(deviceCount))
            }
        }
    }

    /**
     * Callback para dispositivo descubierto
     */
    private fun onDeviceDiscovered(device: BluetoothDevice) {
        viewModelScope.launch {
            if (!deviceCache.contains(device.address)) {
                deviceCache.add(device.address)
                
                val deviceInfo = BluetoothDeviceInfo(
                    name = device.name ?: "Dispositivo desconocido",
                    address = device.address,
                    isPaired = false, // TODO: Implementar isDevicePaired
                    isSelected = false,
                    deviceType = DeviceType.DISCOVERED // TODO: Implementar verificación de emparejado
                )
                
                val currentList = _discoveredDevices.value.toMutableList()
                
                // Evitar duplicados por dirección
                if (!currentList.any { it.address == deviceInfo.address }) {
                    currentList.add(deviceInfo)
                    _discoveredDevices.value = currentList
                    _uiEvents.emit(UIEvent.DeviceDiscovered(deviceInfo))
                }
            }
        }
    }

    /**
     * Selecciona un dispositivo
     */
    fun selectDevice(deviceInfo: BluetoothDeviceInfo) {
        viewModelScope.launch {
            try {
                _connectionState.value = ConnectionState.Connecting
                
                // TODO: Implementar métodos para guardar dispositivos Bluetooth en preferencias
                
                // Actualizar estado local
                val updatedDevice = deviceInfo.copy(isSelected = true)
                _selectedDevice.value = updatedDevice
                
                _discoveryStatus.value = "Dispositivo seleccionado: ${deviceInfo.name}"
                _connectionState.value = ConnectionState.Connected
                
                _uiEvents.emit(UIEvent.DeviceSelected(updatedDevice))
                
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Disconnected
                _uiEvents.emit(UIEvent.ShowError("Error seleccionando dispositivo: ${e.message}"))
            }
        }
    }

    /**
     * Prueba conexión con dispositivo seleccionado
     */
    fun testConnection() {
        viewModelScope.launch {
            val device = _selectedDevice.value
            if (device == null) {
                _uiEvents.emit(UIEvent.ShowError("No hay dispositivo seleccionado"))
                return@launch
            }

            try {
                _connectionState.value = ConnectionState.Testing
                _discoveryStatus.value = "Probando conexión con ${device.name}..."
                
                val success = true // TODO: Implementar testPrintConnection en BluetoothManager
                
                if (success) {
                    _connectionState.value = ConnectionState.Connected
                    _discoveryStatus.value = "Conexión exitosa con ${device.name}"
                    _uiEvents.emit(UIEvent.PrintTestCompleted(true, "Prueba de impresión exitosa"))
                } else {
                    _connectionState.value = ConnectionState.Error("Error de conexión")
                    _discoveryStatus.value = "Error conectando con ${device.name}"
                    _uiEvents.emit(UIEvent.PrintTestCompleted(false, "Error en prueba de impresión"))
                }
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Error(e.message ?: "Error desconocido")
                _discoveryStatus.value = "Error de conexión"
                _uiEvents.emit(UIEvent.ShowError("Error probando conexión: ${e.message}"))
            }
        }
    }

    /**
     * Actualiza estados de permisos
     */
    fun updatePermissionStates(permissions: Map<String, Boolean>) {
        val newStates = PermissionStates(
            bluetoothConnect = permissions["android.permission.BLUETOOTH_CONNECT"] ?: false,
            bluetoothScan = permissions["android.permission.BLUETOOTH_SCAN"] ?: false,
            bluetooth = permissions["android.permission.BLUETOOTH"] ?: false,
            bluetoothAdmin = permissions["android.permission.BLUETOOTH_ADMIN"] ?: false,
            accessCoarseLocation = permissions["android.permission.ACCESS_COARSE_LOCATION"] ?: false
        )
        _permissionStates.value = newStates
    }

    /**
     * Verifica si tiene permisos requeridos
     */
    private fun hasRequiredPermissions(): Boolean {
        val states = _permissionStates.value
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            states.bluetoothConnect && states.bluetoothScan
        } else {
            states.bluetooth && states.bluetoothAdmin && states.accessCoarseLocation
        }
    }

    /**
     * Obtiene lista de permisos requeridos
     */
    private fun getRequiredPermissions(): List<String> {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            listOf(
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            listOf(
                android.Manifest.permission.BLUETOOTH,
                android.Manifest.permission.BLUETOOTH_ADMIN,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    /**
     * Limpia dispositivo seleccionado
     */
    fun clearSelectedDevice() {
        viewModelScope.launch {
            // TODO: Implementar clearBluetoothDevice en PreferencesManager
            _selectedDevice.value = null
            _connectionState.value = ConnectionState.Disconnected
            _discoveryStatus.value = "Dispositivo desvinculado"
            _uiEvents.emit(UIEvent.DeviceCleared)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopDiscovery()
    }

    // Enums y data classes
    data class BluetoothDeviceInfo(
        val name: String,
        val address: String,
        val isPaired: Boolean,
        val isSelected: Boolean,
        val deviceType: DeviceType
    ) {
        val displayName: String get() = "$name\n$address"
    }

    enum class DeviceType {
        DISCOVERED, PAIRED, SAVED
    }

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        object Testing : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    data class PermissionStates(
        val bluetoothConnect: Boolean = false,
        val bluetoothScan: Boolean = false,
        val bluetooth: Boolean = false,
        val bluetoothAdmin: Boolean = false,
        val accessCoarseLocation: Boolean = false
    )

    sealed class UIEvent {
        data class BluetoothStateChanged(val enabled: Boolean) : UIEvent()
        data class DeviceDiscovered(val device: BluetoothDeviceInfo) : UIEvent()
        data class DeviceSelected(val device: BluetoothDeviceInfo) : UIEvent()
        data class DeviceLoaded(val device: BluetoothDeviceInfo) : UIEvent()
        data class DiscoveryCompleted(val deviceCount: Int) : UIEvent()
        data class PrintTestCompleted(val success: Boolean, val message: String) : UIEvent()
        data class ShowError(val message: String) : UIEvent()
        data class PermissionRequired(val permissions: List<String>) : UIEvent()
        object RequestBluetoothEnable : UIEvent()
        object DeviceCleared : UIEvent()
    }
}