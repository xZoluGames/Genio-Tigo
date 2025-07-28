package com.example.geniotecni.tigo.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * 🔧 EXTENSIONES PARA FACILITAR EL USO DEL EVENT BUS
 * 
 * Funciones de extensión que simplifican la emisión y recepción de eventos
 * en diferentes contextos de la aplicación (ViewModels, Activities, etc.)
 */

/**
 * Extensión para ViewModels - Emite evento usando viewModelScope
 */
fun ViewModel.emitEvent(eventBus: AppEventBus, event: AppEvent) {
    viewModelScope.launch {
        eventBus.emit(event)
    }
}

/**
 * Extensión para CoroutineScope - Emite evento
 */
fun CoroutineScope.emitEvent(eventBus: AppEventBus, event: AppEvent) {
    launch {
        eventBus.emit(event)
    }
}

/**
 * Extensión para suscribirse a eventos específicos en ViewModels
 */
inline fun <reified T : AppEvent> ViewModel.subscribeToEvent(
    eventBus: AppEventBus,
    crossinline onEvent: (T) -> Unit
) {
    eventBus.events
        .filterIsInstance<T>()
        .onEach { event -> onEvent(event) }
        .launchIn(viewModelScope)
}

/**
 * Extensión para suscribirse a eventos específicos en cualquier CoroutineScope
 */
inline fun <reified T : AppEvent> CoroutineScope.subscribeToEvent(
    eventBus: AppEventBus,
    crossinline onEvent: (T) -> Unit
) {
    eventBus.events
        .filterIsInstance<T>()
        .onEach { event -> onEvent(event) }
        .launchIn(this)
}

/**
 * Obtiene un Flow filtrado para un tipo específico de evento
 */
inline fun <reified T : AppEvent> AppEventBus.eventsOfType(): Flow<T> {
    return events.filterIsInstance<T>()
}

/**
 * Emite evento de transacción completada (función utilitaria)
 */
fun AppEventBus.emitTransactionCompleted(
    serviceId: String,
    serviceName: String,
    transactionId: String,
    amount: Long,
    referenceData: com.example.geniotecni.tigo.models.ReferenceData?,
    printData: com.example.geniotecni.tigo.models.PrintData
) {
    tryEmit(TransactionCompleted(serviceId, serviceName, transactionId, amount, referenceData, printData))
}

/**
 * Emite evento de error (función utilitaria)
 */
fun AppEventBus.emitError(
    message: String,
    severity: ErrorSeverity = ErrorSeverity.ERROR,
    actionRequired: Boolean = false
) {
    tryEmit(ErrorDisplayRequested(message, severity, actionRequired))
}

/**
 * Emite evento de navegación (función utilitaria)
 */
fun AppEventBus.emitNavigation(
    fromScreen: String,
    toScreen: String,
    data: Map<String, Any> = emptyMap()
) {
    tryEmit(NavigationRequested(fromScreen, toScreen, data))
}

/**
 * Emite evento de servicio seleccionado (función utilitaria)
 */
fun AppEventBus.emitServiceSelected(
    serviceId: String,
    serviceName: String,
    fromScreen: String
) {
    tryEmit(ServiceSelected(serviceId, serviceName, fromScreen))
}

/**
 * Emite evento de cambio de configuración (función utilitaria)
 */
fun AppEventBus.emitSettingChanged(
    settingKey: String,
    oldValue: Any?,
    newValue: Any?
) {
    tryEmit(SettingChanged(settingKey, oldValue, newValue))
}

/**
 * Emite evento de backup creado (función utilitaria)
 */
fun AppEventBus.emitBackupCreated(
    backupPath: String,
    backupSize: Long,
    recordCount: Int
) {
    tryEmit(BackupCreated(backupPath, backupSize, recordCount))
}

/**
 * Emite evento de datos exportados (función utilitaria)
 */
fun AppEventBus.emitDataExported(
    format: String,
    filePath: String,
    recordCount: Int
) {
    tryEmit(DataExported(format, filePath, recordCount))
}

/**
 * Emite evento de dispositivo Bluetooth conectado (función utilitaria)
 */
fun AppEventBus.emitBluetoothConnected(
    deviceName: String,
    deviceAddress: String,
    connectionType: String = "RFCOMM"
) {
    tryEmit(BluetoothDeviceConnected(deviceName, deviceAddress, connectionType))
}

/**
 * Emite evento de trabajo de impresión (función utilitaria)
 */
fun AppEventBus.emitPrintJobCompleted(
    deviceAddress: String,
    jobId: String,
    success: Boolean,
    errorMessage: String? = null
) {
    tryEmit(PrintJobCompleted(deviceAddress, jobId, success, errorMessage))
}

/**
 * Emite evento de estadísticas actualizadas (función utilitaria)
 */
fun AppEventBus.emitStatisticsUpdated(
    totalTransactions: Int,
    totalAmount: Long,
    period: String
) {
    tryEmit(StatisticsUpdated(totalTransactions, totalAmount, period))
}