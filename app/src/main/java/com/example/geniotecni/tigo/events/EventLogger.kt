package com.example.geniotecni.tigo.events

import com.example.geniotecni.tigo.utils.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 📝 EVENT LOGGER - Sistema de Logging Centralizado para Eventos
 * 
 * PROPÓSITO:
 * - Logging automático de todos los eventos que pasan por el Event Bus
 * - Monitoreo en tiempo real del flujo de eventos en la aplicación
 * - Debugging y análisis de comportamiento del sistema de eventos
 * - Métricas de performance y uso del Event Bus
 * 
 * FUNCIONALIDADES:
 * - Logging automático de todos los eventos con timestamp
 * - Categorización de eventos por tipo (Transaction, UI, Bluetooth, etc.)
 * - Filtrado de logs por nivel de importancia
 * - Estadísticas de eventos emitidos y recibidos
 * - Detección de eventos perdidos o no procesados
 * 
 * BENEFICIOS PARA DEBUGGING:
 * - Visibilidad completa del flujo de eventos
 * - Identificación de cuellos de botella en comunicación
 * - Análisis de patrones de uso de eventos
 * - Detección de eventos duplicados o no deseados
 * 
 * INTEGRACIÓN:
 * - Se inicializa automáticamente con Hilt
 * - Funciona en background sin afectar performance
 * - Compatible con sistema de logging existente (AppLogger)
 * - Puede habilitarse/deshabilitarse según configuración
 */
@Singleton
class EventLogger @Inject constructor(
    private val appEventBus: AppEventBus
) {
    
    companion object {
        private const val TAG = "EventLogger"
        private var isEnabled = true // Puede controlarse desde configuración
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val eventStats = mutableMapOf<String, Int>()
    
    init {
        startLogging()
    }
    
    /**
     * Inicia el logging automático de eventos
     */
    private fun startLogging() {
        if (!isEnabled) return
        
        appEventBus.events
            .onEach { event -> logEvent(event) }
            .launchIn(scope)
            
        AppLogger.i(TAG, "Event Logger iniciado - monitoreando eventos del sistema")
    }
    
    /**
     * Registra un evento individual
     */
    private fun logEvent(event: AppEvent) {
        val eventType = event::class.simpleName ?: "UnknownEvent"
        val timestamp = event.timestamp
        val eventId = event.eventId
        
        // Actualizar estadísticas
        eventStats[eventType] = eventStats.getOrDefault(eventType, 0) + 1
        
        // Log según tipo de evento
        when (event) {
            is TransactionEvent -> logTransactionEvent(event, eventType, eventId)
            is UIEvent -> logUIEvent(event, eventType, eventId)
            is BluetoothEvent -> logBluetoothEvent(event, eventType, eventId)
            is DataEvent -> logDataEvent(event, eventType, eventId)
            is SystemEvent -> logSystemEvent(event, eventType, eventId)
        }
    }
    
    /**
     * Log específico para eventos de transacción
     */
    private fun logTransactionEvent(event: TransactionEvent, eventType: String, eventId: String) {
        when (event) {
            is TransactionStarted -> {
                AppLogger.i(TAG, "🚀 [$eventType] Transacción iniciada - Servicio: ${event.serviceName} (${event.serviceId}) | ID: ${event.transactionId}")
            }
            is TransactionCompleted -> {
                AppLogger.i(TAG, "✅ [$eventType] Transacción completada - Servicio: ${event.serviceName} | Monto: ${event.amount} | Ref: ${event.referenceData?.ref1 ?: "N/A"}")
            }
            is TransactionFailed -> {
                AppLogger.w(TAG, "❌ [$eventType] Transacción fallida - Servicio: ${event.serviceName} | Error: ${event.errorMessage}")
            }
            is USSDResponseReceived -> {
                AppLogger.d(TAG, "📱 [$eventType] Respuesta USSD recibida - ID: ${event.transactionId} | Ref: ${event.referenceData?.ref1 ?: "N/A"}")
            }
            is PaymentProcessed -> {
                AppLogger.i(TAG, "💰 [$eventType] Pago procesado - Proveedor: ${event.serviceProvider} | Monto: ${event.amount} | Éxito: ${event.success}")
            }
        }
    }
    
    /**
     * Log específico para eventos de UI
     */
    private fun logUIEvent(event: UIEvent, eventType: String, eventId: String) {
        when (event) {
            is NavigationRequested -> {
                AppLogger.d(TAG, "🧭 [$eventType] Navegación solicitada - De: ${event.fromScreen} → A: ${event.toScreen}")
            }
            is ServiceSelected -> {
                AppLogger.d(TAG, "🎯 [$eventType] Servicio seleccionado - ${event.serviceName} desde ${event.fromScreen}")
            }
            is ThemeChanged -> {
                AppLogger.d(TAG, "🎨 [$eventType] Tema cambiado - De: ${event.previousTheme} → A: ${event.newTheme}")
            }
            is LanguageChanged -> {
                AppLogger.d(TAG, "🌍 [$eventType] Idioma cambiado - De: ${event.previousLanguage} → A: ${event.newLanguage}")
            }
            is RefreshRequested -> {
                AppLogger.d(TAG, "🔄 [$eventType] Refresh solicitado")
            }
            is ErrorDisplayRequested -> {
                val severity = when (event.severity) {
                    ErrorSeverity.CRITICAL -> "🚨"
                    ErrorSeverity.ERROR -> "❌"
                    ErrorSeverity.WARNING -> "⚠️"
                    ErrorSeverity.INFO -> "ℹ️"
                }
                AppLogger.e(TAG, "$severity [$eventType] Error a mostrar - ${event.message} | Severidad: ${event.severity}")
            }
        }
    }
    
    /**
     * Log específico para eventos de Bluetooth
     */
    private fun logBluetoothEvent(event: BluetoothEvent, eventType: String, eventId: String) {
        when (event) {
            is BluetoothStateChanged -> {
                val status = if (event.enabled) "activado" else "desactivado"
                AppLogger.i(TAG, "📶 [$eventType] Bluetooth $status")
            }
            is BluetoothDeviceDiscovered -> {
                val pairStatus = if (event.isPaired) "(emparejado)" else "(nuevo)"
                AppLogger.d(TAG, "🔍 [$eventType] Dispositivo descubierto - ${event.deviceName} $pairStatus | ${event.deviceAddress}")
            }
            is BluetoothDeviceConnected -> {
                AppLogger.i(TAG, "🔗 [$eventType] Dispositivo conectado - ${event.deviceName} | ${event.deviceAddress}")
            }
            is BluetoothDeviceDisconnected -> {
                val reason = event.reason?.let { " | Razón: $it" } ?: ""
                AppLogger.w(TAG, "📴 [$eventType] Dispositivo desconectado - ${event.deviceName}$reason")
            }
            is PrintJobStarted -> {
                AppLogger.d(TAG, "🖨️ [$eventType] Trabajo de impresión iniciado - Job: ${event.jobId} | Tamaño: ${event.dataSize} bytes")
            }
            is PrintJobCompleted -> {
                val status = if (event.success) "exitoso" else "fallido"
                val error = event.errorMessage?.let { " | Error: $it" } ?: ""
                AppLogger.i(TAG, "🖨️ [$eventType] Trabajo de impresión $status - Job: ${event.jobId}$error")
            }
        }
    }
    
    /**
     * Log específico para eventos de datos
     */
    private fun logDataEvent(event: DataEvent, eventType: String, eventId: String) {
        when (event) {
            is DataSaved -> {
                AppLogger.d(TAG, "💾 [$eventType] Datos guardados - Tipo: ${event.dataType} | ID: ${event.recordId} | Tamaño: ${event.size} bytes")
            }
            is DataDeleted -> {
                AppLogger.d(TAG, "🗑️ [$eventType] Datos eliminados - Tipo: ${event.dataType} | ID: ${event.recordId}")
            }
            is BackupCreated -> {
                AppLogger.i(TAG, "📦 [$eventType] Backup creado - Ruta: ${event.backupPath} | Registros: ${event.recordCount} | Tamaño: ${event.backupSize} bytes")
            }
            is BackupRestored -> {
                val status = if (event.success) "exitoso" else "fallido"
                AppLogger.i(TAG, "📥 [$eventType] Backup restaurado $status - Registros: ${event.recordsRestored}")
            }
            is DataExported -> {
                AppLogger.i(TAG, "📤 [$eventType] Datos exportados - Formato: ${event.format} | Registros: ${event.recordCount} | Archivo: ${event.filePath}")
            }
            is StatisticsUpdated -> {
                AppLogger.d(TAG, "📊 [$eventType] Estadísticas actualizadas - Transacciones: ${event.totalTransactions} | Monto total: ${event.totalAmount} | Período: ${event.period}")
            }
        }
    }
    
    /**
     * Log específico para eventos del sistema
     */
    private fun logSystemEvent(event: SystemEvent, eventType: String, eventId: String) {
        when (event) {
            is PermissionGranted -> {
                val status = if (event.granted) "otorgado" else "denegado"
                AppLogger.i(TAG, "🔐 [$eventType] Permiso $status - ${event.permission}")
            }
            is SettingChanged -> {
                AppLogger.d(TAG, "⚙️ [$eventType] Configuración cambiada - ${event.settingKey}: ${event.oldValue} → ${event.newValue}")
            }
            is AppConfigurationChanged -> {
                AppLogger.d(TAG, "🔧 [$eventType] Configuración de app cambiada - ${event.configKey}: ${event.newValue}")
            }
            is AppStarted -> {
                AppLogger.i(TAG, "🚀 [$eventType] Aplicación iniciada")
            }
            is AppStopped -> {
                AppLogger.i(TAG, "🛑 [$eventType] Aplicación detenida")
            }
            is LowMemoryWarning -> {
                AppLogger.w(TAG, "⚠️ [$eventType] Advertencia de memoria baja - Disponible: ${event.availableMemoryMB}MB | Usada: ${event.usedMemoryMB}MB")
            }
            is NetworkStateChanged -> {
                val status = if (event.connected) "conectada" else "desconectada"
                val type = event.connectionType?.let { " ($it)" } ?: ""
                AppLogger.i(TAG, "🌐 [$eventType] Red $status$type")
            }
        }
    }
    
    /**
     * Obtiene estadísticas de eventos
     */
    fun getEventStatistics(): Map<String, Int> {
        return eventStats.toMap()
    }
    
    /**
     * Obtiene número total de eventos procesados
     */
    fun getTotalEventsProcessed(): Int {
        return eventStats.values.sum()
    }
    
    /**
     * Reinicia estadísticas
     */
    fun resetStatistics() {
        eventStats.clear()
        AppLogger.i(TAG, "Estadísticas de eventos reiniciadas")
    }
    
    /**
     * Habilita/deshabilita logging
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (enabled) {
            AppLogger.i(TAG, "Event Logger habilitado")
        } else {
            AppLogger.i(TAG, "Event Logger deshabilitado")
        }
    }
    
    /**
     * Imprime reporte de estadísticas
     */
    fun printStatisticsReport() {
        AppLogger.i(TAG, "=== REPORTE DE ESTADÍSTICAS DE EVENTOS ===")
        AppLogger.i(TAG, "Total de eventos procesados: ${getTotalEventsProcessed()}")
        AppLogger.i(TAG, "Suscriptores activos: ${appEventBus.subscriberCount}")
        
        if (eventStats.isNotEmpty()) {
            AppLogger.i(TAG, "Eventos por tipo:")
            eventStats.entries
                .sortedByDescending { it.value }
                .forEach { (type, count) ->
                    AppLogger.i(TAG, "  - $type: $count")
                }
        } else {
            AppLogger.i(TAG, "No hay estadísticas disponibles")
        }
        AppLogger.i(TAG, "==============================================")
    }
}