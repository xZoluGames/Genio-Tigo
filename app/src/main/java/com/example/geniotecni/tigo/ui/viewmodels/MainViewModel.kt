package com.example.geniotecni.tigo.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geniotecni.tigo.data.repository.ServiceRepository
import com.example.geniotecni.tigo.data.processors.TransactionDataProcessor
import com.example.geniotecni.tigo.managers.*
import com.example.geniotecni.tigo.helpers.USSDIntegrationHelper
import com.example.geniotecni.tigo.models.*
import com.example.geniotecni.tigo.ui.adapters.ServiceItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🎯 MAIN VIEW MODEL - Lógica de Negocio Centralizada para MainActivity
 * 
 * PROPÓSITO ARQUITECTÓNICO:
 * - Separación completa entre lógica de negocio y presentación UI
 * - Supervivencia a cambios de configuración (rotación, etc.)
 * - Gestión de estado reactivo con StateFlow y LiveData
 * - Coordinación entre múltiples managers y helpers
 * 
 * GESTIÓN DE ESTADO:
 * - currentService: Servicio actualmente seleccionado
 * - transactionState: Estado de la transacción en curso
 * - validationErrors: Errores de validación en tiempo real
 * - ussdState: Estado de las operaciones USSD/SMS
 * - amountSuggestions: Sugerencias inteligentes de montos
 * 
 * OPERACIONES PRINCIPALES:
 * - validateInputs(): Validación reactiva de campos de entrada
 * - processTransaction(): Orquestación completa de transacciones
 * - handleUSSDResponse(): Manejo de respuestas USSD asíncronas
 * - saveTransactionData(): Persistencia de datos de transacciones
 * 
 * INYECCIÓN DE DEPENDENCIAS:
 * - Todas las dependencias inyectadas vía Hilt para testabilidad
 * - Managers para persistencia y configuración
 * - Helpers para operaciones especializadas
 * - Repository para acceso a datos de servicios
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val serviceRepository: ServiceRepository,
    private val transactionProcessor: TransactionDataProcessor,
    private val preferencesManager: PreferencesManager,
    private val printDataManager: PrintDataManager,
    private val bluetoothManager: BluetoothManager,
    private val ussdIntegrationHelper: USSDIntegrationHelper,
    private val amountUsageManager: AmountUsageManager,
    private val printCooldownManager: PrintCooldownManager
) : ViewModel(), USSDIntegrationHelper.USSDCallback {

    // Estados reactivos
    private val _currentService = MutableStateFlow<ServiceItem?>(null)
    val currentService: StateFlow<ServiceItem?> = _currentService.asStateFlow()

    private val _transactionState = MutableStateFlow<TransactionState>(TransactionState.Idle)
    val transactionState: StateFlow<TransactionState> = _transactionState.asStateFlow()

    private val _validationErrors = MutableStateFlow<ValidationErrors>(ValidationErrors())
    val validationErrors: StateFlow<ValidationErrors> = _validationErrors.asStateFlow()

    private val _ussdState = MutableStateFlow<USSDState>(USSDState.Idle)
    val ussdState: StateFlow<USSDState> = _ussdState.asStateFlow()

    private val _amountSuggestions = MutableStateFlow<List<AmountUsageManager.AmountUsageData>>(emptyList())
    val amountSuggestions: StateFlow<List<AmountUsageManager.AmountUsageData>> = _amountSuggestions.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UIEvent>()
    val uiEvents: SharedFlow<UIEvent> = _uiEvents.asSharedFlow()

    init {
        loadAmountSuggestions()
    }

    /**
     * Configura el servicio actual y carga sus configuraciones
     */
    fun setCurrentService(service: ServiceItem) {
        viewModelScope.launch {
            _currentService.value = service
            _transactionState.value = TransactionState.Idle
            _validationErrors.value = ValidationErrors()
            
            // Limpiar estado USSD anterior
            _ussdState.value = USSDState.Idle
            
            // Emitir evento de cambio de servicio
            _uiEvents.emit(UIEvent.ServiceChanged(service))
        }
    }

    /**
     * Valida los inputs de forma reactiva
     */
    fun validateInputs(phone: String, cedula: String, amount: String, date: String = ""): Boolean {
        val currentService = _currentService.value ?: return false
        val config = serviceRepository.getServiceConfig(currentService.id)
        val errors = ValidationErrors()

        // Validación de teléfono
        if (config.showPhone && phone.isBlank()) {
            errors.phoneError = "Teléfono requerido"
        } else if (config.showPhone && !transactionProcessor.isValidPhoneNumber(phone)) {
            errors.phoneError = "Formato de teléfono inválido"
        }

        // Validación de cédula
        if (config.showCedula && cedula.isBlank()) {
            errors.cedulaError = "Cédula requerida"
        } else if (config.showCedula && !transactionProcessor.isValidCedula(cedula)) {
            errors.cedulaError = "Formato de cédula inválido"
        }

        // Validación de monto
        if (config.showAmount && amount.isBlank()) {
            errors.amountError = "Monto requerido"
        } else if (config.showAmount) {
            val numericAmount = amount.replace(",", "").toLongOrNull()
            if (numericAmount == null || numericAmount <= 0) {
                errors.amountError = "Monto debe ser mayor a 0"
            }
        }

        // Validación de fecha de nacimiento
        if (config.showNacimiento && date.isBlank()) {
            errors.dateError = "Fecha de nacimiento requerida"
        }

        _validationErrors.value = errors
        return !errors.hasErrors()
    }

    /**
     * Procesa una transacción completa
     */
    fun processTransaction(phone: String, cedula: String, amount: String, date: String = "") {
        viewModelScope.launch {
            val currentService = _currentService.value ?: return@launch
            
            if (!validateInputs(phone, cedula, amount, date)) {
                _uiEvents.emit(UIEvent.ShowError("Por favor complete todos los campos requeridos"))
                return@launch
            }

            _transactionState.value = TransactionState.Processing

            try {
                if (serviceRepository.hasUSSDSupport(currentService.id)) {
                    processUSSDTransaction(currentService, phone, cedula, amount, date)
                } else {
                    processManualTransaction(currentService, phone, cedula, amount, date)
                }
            } catch (e: Exception) {
                _transactionState.value = TransactionState.Error(e.message ?: "Error desconocido")
                _uiEvents.emit(UIEvent.ShowError("Error procesando transacción: ${e.message}"))
            }
        }
    }

    /**
     * Procesa transacción USSD
     */
    private fun processUSSDTransaction(
        service: ServiceItem,
        phone: String,
        cedula: String,
        amount: String,
        date: String
    ) {
        val params = mapOf(
            "phone" to phone,
            "cedula" to cedula,
            "amount" to amount.replace(",", ""),
            "nacimiento" to date
        )

        val ussdCode = serviceRepository.generateUSSDCode(service.id, params)
        if (ussdCode != null) {
            _ussdState.value = USSDState.Executing
            ussdIntegrationHelper.executeUSSD(ussdCode, service.name, this)
            
            // Registrar uso del monto
            if (amount.isNotBlank()) {
                viewModelScope.launch {
                    val numericAmount = amount.replace(",", "").toLongOrNull()
                    if (numericAmount != null && numericAmount > 0) {
                        amountUsageManager.recordAmountUsage(numericAmount)
                        loadAmountSuggestions() // Recargar sugerencias
                    }
                }
            }
        } else {
            _transactionState.value = TransactionState.Error("No se pudo generar código USSD")
        }
    }

    /**
     * Procesa transacción manual (sin USSD)
     */
    private fun processManualTransaction(
        service: ServiceItem,
        phone: String,
        cedula: String,
        amount: String,
        date: String
    ) {
        viewModelScope.launch {
            val message = buildManualTransactionMessage(service, phone, cedula, amount, date)
            val printData = createPrintData(service, phone, cedula, amount, date, message)
            
            // Guardar datos
            printDataManager.savePrintData(printData)
            _transactionState.value = TransactionState.Success(printData)
            
            _uiEvents.emit(UIEvent.TransactionCompleted(printData))
        }
    }

    /**
     * Construye mensaje para transacciones manuales
     */
    private fun buildManualTransactionMessage(
        service: ServiceItem,
        phone: String,
        cedula: String,
        amount: String,
        date: String
    ): String {
        // Usar TransactionDataProcessor para construir mensaje consistente
        return "Servicio: ${service.name}\n" +
               "Teléfono: $phone\n" +
               "Cédula: $cedula\n" +
               if (amount.isNotBlank()) "Monto: ${transactionProcessor.formatAmountString(amount)}\n" else "" +
               if (date.isNotBlank()) "Fecha: $date\n" else "" +
               "Fecha: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}"
    }

    /**
     * Crea PrintData para persistencia
     */
    private fun createPrintData(
        service: ServiceItem,
        phone: String,
        cedula: String,
        amount: String,
        date: String,
        message: String
    ): PrintData {
        val currentDate = java.util.Date()
        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        
        val transactionData = TransactionData(
            phone = phone,
            cedula = cedula,
            amount = amount.replace(",", ""),
            date = date,
            additionalData = emptyMap()
        )

        return PrintData(
            service = service.name,
            date = dateFormat.format(currentDate),
            time = timeFormat.format(currentDate),
            message = message,
            referenceData = ReferenceData("N/A", "N/A"),
            transactionData = transactionData
        )
    }

    /**
     * Carga sugerencias de montos
     */
    private fun loadAmountSuggestions() {
        viewModelScope.launch {
            try {
                val suggestions = amountUsageManager.getTopUsedAmounts(5)
                _amountSuggestions.value = suggestions
            } catch (e: Exception) {
                // Silently handle errors in suggestions
                _amountSuggestions.value = emptyList()
            }
        }
    }

    /**
     * Maneja respuesta manual de referencia
     */
    fun processManualReference(ref1: String, ref2: String) {
        viewModelScope.launch {
            val referenceData = ReferenceData(ref1, ref2)
            
            // Actualizar estado con referencias
            val currentState = _transactionState.value
            if (currentState is TransactionState.Success) {
                val updatedPrintData = currentState.printData.copy(referenceData = referenceData)
                printDataManager.savePrintData(updatedPrintData)
                _transactionState.value = TransactionState.Success(updatedPrintData)
                _uiEvents.emit(UIEvent.ReferencesUpdated(referenceData))
            }
        }
    }

    // Implementación de USSDCallback
    override fun onReferenceFound(referenceData: ReferenceData, smsBody: String) {
        viewModelScope.launch {
            _ussdState.value = USSDState.Success(referenceData)
            
            // Actualizar transacción con referencias
            val currentState = _transactionState.value
            if (currentState is TransactionState.Processing) {
                // Crear PrintData con referencias
                val service = _currentService.value ?: return@launch
                val printData = createPrintDataWithReferences(service, referenceData, smsBody)
                
                printDataManager.savePrintData(printData)
                _transactionState.value = TransactionState.Success(printData)
                _uiEvents.emit(UIEvent.TransactionCompleted(printData))
            }
        }
    }

    override fun onSearchTimeout() {
        viewModelScope.launch {
            _ussdState.value = USSDState.Timeout
            _transactionState.value = TransactionState.Error("Tiempo de búsqueda agotado")
            _uiEvents.emit(UIEvent.ShowError("Tiempo de búsqueda agotado. Intente nuevamente."))
        }
    }

    override fun onError(error: String) {
        viewModelScope.launch {
            _ussdState.value = USSDState.Error(error)
            _transactionState.value = TransactionState.Error(error)
            _uiEvents.emit(UIEvent.ShowError("Error USSD: $error"))
        }
    }

    private fun createPrintDataWithReferences(
        service: ServiceItem,
        referenceData: ReferenceData,
        smsBody: String
    ): PrintData {
        val currentDate = java.util.Date()
        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())

        return PrintData(
            service = service.name,
            date = dateFormat.format(currentDate),
            time = timeFormat.format(currentDate),
            message = smsBody,
            referenceData = referenceData,
            transactionData = TransactionData() // Datos mínimos
        )
    }

    override fun onCleared() {
        super.onCleared()
        ussdIntegrationHelper.onDestroy()
    }

    // Clases de estado
    sealed class TransactionState {
        object Idle : TransactionState()
        object Processing : TransactionState()
        data class Success(val printData: PrintData) : TransactionState()
        data class Error(val message: String) : TransactionState()
    }

    sealed class USSDState {
        object Idle : USSDState()
        object Executing : USSDState()
        data class Success(val referenceData: ReferenceData) : USSDState()
        object Timeout : USSDState()
        data class Error(val message: String) : USSDState()
    }

    data class ValidationErrors(
        var phoneError: String? = null,
        var cedulaError: String? = null,
        var amountError: String? = null,
        var dateError: String? = null
    ) {
        fun hasErrors(): Boolean = phoneError != null || cedulaError != null || 
                                   amountError != null || dateError != null
    }

    sealed class UIEvent {
        data class ServiceChanged(val service: ServiceItem) : UIEvent()
        data class ShowError(val message: String) : UIEvent()
        data class TransactionCompleted(val printData: PrintData) : UIEvent()
        data class ReferencesUpdated(val referenceData: ReferenceData) : UIEvent()
    }
}