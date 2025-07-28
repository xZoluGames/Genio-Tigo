package com.example.geniotecni.tigo.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🚌 APP EVENT BUS - Sistema Centralizado de Eventos para Desacoplamiento
 * 
 * PROPÓSITO ARQUITECTÓNICO:
 * - Comunicación desacoplada entre componentes de la aplicación
 * - Patrón Observer reactivo usando Kotlin Coroutines y Flow
 * - Eliminación de dependencias directas entre Activities, ViewModels y Managers
 * - Hub central para todos los eventos de la aplicación
 * 
 * BENEFICIOS DEL DESACOPLAMIENTO:
 * - Componentes no necesitan conocerse directamente
 * - Facilita testing con eventos mockeable
 * - Mejora la mantenibilidad y escalabilidad
 * - Permite comunicación multi-directional sin referencias circulares
 * - Gestión centralizada de todos los eventos del sistema
 * 
 * TIPOS DE EVENTOS SOPORTADOS:
 * - TransactionEvents: Eventos relacionados con transacciones y pagos
 * - UIEvents: Eventos de interfaz de usuario y navegación
 * - BluetoothEvents: Eventos de conectividad y dispositivos Bluetooth
 * - DataEvents: Eventos de persistencia y sincronización de datos
 * - SystemEvents: Eventos del sistema (permisos, configuraciones, errores)
 * 
 * PATRÓN DE COMUNICACIÓN:
 * - Emisores: ViewModels, Managers, Helpers emiten eventos
 * - Receptores: Activities, Fragments, otros ViewModels reciben eventos
 * - Filtrado: Cada receptor puede filtrar solo eventos relevantes
 * - Lifecycle-aware: Compatible con ciclo de vida de Android
 * 
 * OPTIMIZACIONES DE RENDIMIENTO:
 * - SharedFlow con replay = 0 para eventos en tiempo real
 * - extraBufferCapacity optimizado para alta concurrencia
 * - onBufferOverflow = DROP_OLDEST para evitar memory leaks
 * - Singleton thread-safe para acceso global eficiente
 * 
 * CASOS DE USO PRINCIPALES:
 * - MainActivity → PrintHistoryActivity: Notificar nueva transacción
 * - BluetoothManager → UI: Notificar cambios de conectividad
 * - SettingsViewModel → Toda la app: Cambios de configuración
 * - PrintDataManager → Statistics: Actualizar métricas en tiempo real
 * - USSDIntegrationHelper → UI: Resultados de operaciones USSD
 * 
 * EXAMPLES DE EVENTOS:
 * - TransactionCompleted: Transacción finalizada exitosamente
 * - BluetoothDeviceConnected: Dispositivo Bluetooth conectado
 * - SettingsChanged: Configuración de usuario modificada
 * - DataBackupCompleted: Backup de datos finalizado
 * - PermissionGranted: Permiso crítico otorgado por usuario
 * 
 * USO TÍPICO:
 * ```kotlin
 * // Emitir evento
 * appEventBus.emit(TransactionCompleted(transactionData))
 * 
 * // Recibir eventos
 * appEventBus.events
 *     .filterIsInstance<TransactionCompleted>()
 *     .collect { event -> 
 *         // Manejar evento
 *     }
 * ```
 * 
 * INTEGRACIÓN CON HILT:
 * - Singleton automático para toda la aplicación
 * - Inyectable en cualquier componente que necesite comunicación
 * - No requiere inicialización manual
 * - Compatible con tod0 el sistema de DI
 * 
 * THREAD SAFETY:
 * - Thread-safe por diseño usando Kotlin Coroutines
 * - Puede ser usado desde cualquier hilo sin sincronización adicional
 * - SharedFlow maneja concurrencia internamente
 * - Emission y collection son operaciones atómicas
 */
@Singleton
class AppEventBus @Inject constructor() {

    private val _events = MutableSharedFlow<AppEvent>(
        replay = 0, // No replay - eventos en tiempo real
        extraBufferCapacity = 100, // Buffer para alta concurrencia
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    
    /**
     * Flow público para suscripción a eventos
     */
    val events: SharedFlow<AppEvent> = _events.asSharedFlow()

    /**
     * Emite un evento al bus
     * @param event Evento a emitir
     */
    suspend fun emit(event: AppEvent) {
        _events.emit(event)
    }

    /**
     * Emite un evento de forma síncrona (no suspendable)
     * Útil para casos donde no se puede usar suspend functions
     * @param event Evento a emitir
     */
    fun tryEmit(event: AppEvent): Boolean {
        return _events.tryEmit(event)
    }

    /**
     * Obtiene número de suscriptores actuales
     */
    val subscriberCount: Int
        get() = _events.subscriptionCount.value
}

/**
 * 📋 INTERFAZ BASE PARA TODOS LOS EVENTOS
 * 
 * Interfaz marcadora que deben implementar todos los eventos del sistema.
 * Permite type-safety y facilita el filtrado de eventos específicos.
 */

/**
 * 💰 EVENTOS DE TRANSACCIONES
 * 
 * Eventos relacionados con operaciones de transacciones, pagos y servicios financieros.
 */

/**
 * 🎨 EVENTOS DE INTERFAZ DE USUARIO
 * 
 * Eventos relacionados con navegación, cambios de UI y interacciones de usuario.
 */

/**
 * 📶 EVENTOS DE BLUETOOTH
 * 
 * Eventos relacionados con conectividad Bluetooth y dispositivos de impresión.
 */

/**
 * 💾 EVENTOS DE DATOS
 * 
 * Eventos relacionados con persistencia, sincronización y gestión de datos.
 */

/**
 * ⚙️ EVENTOS DEL SISTEMA
 * 
 * Eventos relacionados con configuraciones, permisos y estado del sistema.
 */

/**
 * Funciones utilitarias para generar IDs únicos
 */
fun generateTransactionId(): String {
    return "TXN_${System.currentTimeMillis()}_${(1000..9999).random()}"
}

fun generateJobId(): String {
    return "JOB_${System.currentTimeMillis()}_${(100..999).random()}"
}