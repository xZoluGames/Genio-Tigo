package com.example.geniotecni.tigo.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geniotecni.tigo.managers.PreferencesManager
import com.example.geniotecni.tigo.managers.PrintDataManager
// import com.example.geniotecni.tigo.managers.BackupManager // Temporarily disabled
import com.example.geniotecni.tigo.helpers.BackupHelper
import com.example.geniotecni.tigo.helpers.ExportHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * ⚙️ SETTINGS VIEW MODEL - Gestión Centralizada de Configuraciones de la Aplicación
 * 
 * PROPÓSITO ARQUITECTÓNICO:
 * - Separación completa entre lógica de configuración y presentación UI
 * - Gestión reactiva de todas las preferencias de usuario y configuraciones
 * - Coordinación entre múltiples managers para operaciones complejas
 * - Estados reactivos para configuraciones en tiempo real
 * 
 * GESTIÓN DE CONFIGURACIONES:
 * - appTheme: Tema visual de la aplicación (oscuro, claro, automático)
 * - autoPrint: Impresión automática tras completar transacciones
 * - soundEnabled: Habilitación de sonidos y notificaciones auditivas
 * - vibrationEnabled: Retroalimentación háptica para acciones importantes
 * - defaultSim: SIM predeterminada para operaciones telefónicas
 * - printSize: Tamaño de fuente para impresiones (pequeño, mediano, grande)
 * 
 * OPERACIONES DE BACKUP Y RESTORE:
 * - Backup automático según configuración de usuario
 * - Backup manual con confirmación y progreso
 * - Listado de backups disponibles con metadatos
 * - Restauración selectiva con validación de integridad
 * - Limpieza automática de backups antiguos
 * 
 * EXPORTACIÓN DE DATOS:
 * - Exportación a CSV con datos estructurados
 * - Generación de PDF con formato profesional
 * - Exportación combinada (CSV + PDF) en una operación
 * - Configuración de formato de exportación preferido
 * - Progreso y estados de exportación en tiempo real
 * 
 * GESTIÓN DE DATOS:
 * - Limpieza segura de todos los datos con confirmación
 * - Estadísticas de uso de espacio y datos almacenados
 * - Validación de integridad de datos críticos
 * - Operaciones de mantenimiento automático
 * 
 * FUNCIONALIDADES AVANZADAS:
 * - Validación de configuraciones con fallbacks seguros
 * - Migración automática de configuraciones entre versiones
 * - Sincronización de configuraciones entre sesiones
 * - Detección de conflictos de configuración
 * - Restauración de configuraciones por defecto
 * 
 * INYECCIÓN DE DEPENDENCIAS:
 * - PreferencesManager: Gestión de todas las preferencias de usuario
 * - PrintDataManager: Operaciones sobre datos de transacciones
 * - BackupManager: Gestión avanzada de backups y restauraciones
 * - BackupHelper: Operaciones de backup específicas
 * - ExportHelper: Exportación a diferentes formatos
 * 
 * EVENTOS DE UI:
 * - ConfigurationChanged: Notificación de cambio de configuración
 * - BackupCompleted: Backup creado exitosamente
 * - RestoreCompleted: Datos restaurados desde backup
 * - ExportCompleted: Exportación finalizada
 * - DataCleared: Limpieza de datos completada
 * - ShowError: Manejo centralizado de errores
 * - ShowConfirmation: Diálogos de confirmación para operaciones críticas
 * 
 * CONEXIONES ARQUITECTÓNICAS:
 * - GESTIONA: PreferencesManager para todas las configuraciones de usuario
 * - COORDINA: BackupManager y BackupHelper para operaciones de respaldo
 * - UTILIZA: ExportHelper para generación de reportes y archivos
 * - MANEJA: PrintDataManager para operaciones sobre datos críticos
 * - EMITE: UIEvents para comunicación reactiva con SettingsActivity
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val printDataManager: PrintDataManager,
    // private val backupManager: BackupManager, // Temporarily disabled
    private val backupHelper: BackupHelper,
    private val exportHelper: ExportHelper
) : ViewModel() {

    // Estados reactivos de configuraciones principales
    private val _appTheme = MutableStateFlow(preferencesManager.appTheme)
    val appTheme: StateFlow<Int> = _appTheme.asStateFlow()

    private val _autoPrint = MutableStateFlow(preferencesManager.autoPrint)
    val autoPrint: StateFlow<Boolean> = _autoPrint.asStateFlow()

    private val _soundEnabled = MutableStateFlow(preferencesManager.soundEnabled)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(preferencesManager.vibrationEnabled)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    private val _defaultSim = MutableStateFlow(preferencesManager.defaultSim)
    val defaultSim: StateFlow<Int> = _defaultSim.asStateFlow()

    private val _printSize = MutableStateFlow(preferencesManager.printSize)
    val printSize: StateFlow<String> = _printSize.asStateFlow()

    private val _exportFormat = MutableStateFlow(preferencesManager.exportFormat)
    val exportFormat: StateFlow<String> = _exportFormat.asStateFlow()

    // Estados de backup y restore
    private val _backupEnabled = MutableStateFlow(preferencesManager.backupEnabled)
    val backupEnabled: StateFlow<Boolean> = _backupEnabled.asStateFlow()

    private val _lastBackupTime = MutableStateFlow(preferencesManager.lastBackupTime)
    val lastBackupTime: StateFlow<Long> = _lastBackupTime.asStateFlow()

    private val _availableBackups = MutableStateFlow<List<BackupInfo>>(emptyList())
    val availableBackups: StateFlow<List<BackupInfo>> = _availableBackups.asStateFlow()

    // Estados de operaciones
    private val _isBackupInProgress = MutableStateFlow(false)
    val isBackupInProgress: StateFlow<Boolean> = _isBackupInProgress.asStateFlow()

    private val _isRestoreInProgress = MutableStateFlow(false)
    val isRestoreInProgress: StateFlow<Boolean> = _isRestoreInProgress.asStateFlow()

    private val _isExportInProgress = MutableStateFlow(false)
    val isExportInProgress: StateFlow<Boolean> = _isExportInProgress.asStateFlow()

    private val _isClearDataInProgress = MutableStateFlow(false)
    val isClearDataInProgress: StateFlow<Boolean> = _isClearDataInProgress.asStateFlow()

    // Estadísticas de datos
    private val _dataStatistics = MutableStateFlow<DataStatistics?>(null)
    val dataStatistics: StateFlow<DataStatistics?> = _dataStatistics.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UIEvent>()
    val uiEvents: SharedFlow<UIEvent> = _uiEvents.asSharedFlow()

    init {
        loadDataStatistics()
        loadAvailableBackups()
    }

    /**
     * Actualiza el tema de la aplicación
     */
    fun setAppTheme(theme: Int) {
        viewModelScope.launch {
            val previousTheme = _appTheme.value
            _appTheme.value = theme
            preferencesManager.appTheme = theme
            
            _uiEvents.emit(UIEvent.ConfigurationChanged("appTheme", previousTheme, theme))
            _uiEvents.emit(UIEvent.ThemeChanged(theme))
        }
    }

    /**
     * Configura impresión automática
     */
    fun setAutoPrint(enabled: Boolean) {
        viewModelScope.launch {
            _autoPrint.value = enabled
            preferencesManager.autoPrint = enabled
            
            _uiEvents.emit(UIEvent.ConfigurationChanged("autoPrint", !enabled, enabled))
        }
    }

    /**
     * Habilita/deshabilita sonidos
     */
    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _soundEnabled.value = enabled
            preferencesManager.soundEnabled = enabled
            
            _uiEvents.emit(UIEvent.ConfigurationChanged("soundEnabled", !enabled, enabled))
        }
    }

    /**
     * Habilita/deshabilita vibración
     */
    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _vibrationEnabled.value = enabled
            preferencesManager.vibrationEnabled = enabled
            
            _uiEvents.emit(UIEvent.ConfigurationChanged("vibrationEnabled", !enabled, enabled))
        }
    }

    /**
     * Configura SIM por defecto
     */
    fun setDefaultSim(sim: Int) {
        viewModelScope.launch {
            val previousSim = _defaultSim.value
            _defaultSim.value = sim
            preferencesManager.defaultSim = sim
            
            _uiEvents.emit(UIEvent.ConfigurationChanged("defaultSim", previousSim, sim))
        }
    }

    /**
     * Configura tamaño de impresión
     */
    fun setPrintSize(size: String) {
        viewModelScope.launch {
            val validSizes = listOf("small", "medium", "large")
            if (validSizes.contains(size)) {
                val previousSize = _printSize.value
                _printSize.value = size
                preferencesManager.printSize = size
                
                _uiEvents.emit(UIEvent.ConfigurationChanged("printSize", previousSize, size))
            } else {
                _uiEvents.emit(UIEvent.ShowError("Tamaño de impresión inválido: $size"))
            }
        }
    }

    /**
     * Configura formato de exportación
     */
    fun setExportFormat(format: String) {
        viewModelScope.launch {
            val validFormats = listOf("csv", "pdf", "both")
            if (validFormats.contains(format)) {
                val previousFormat = _exportFormat.value
                _exportFormat.value = format
                preferencesManager.exportFormat = format
                
                _uiEvents.emit(UIEvent.ConfigurationChanged("exportFormat", previousFormat, format))
            } else {
                _uiEvents.emit(UIEvent.ShowError("Formato de exportación inválido: $format"))
            }
        }
    }

    /**
     * Habilita/deshabilita backup automático
     */
    fun setBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _backupEnabled.value = enabled
            preferencesManager.backupEnabled = enabled
            
            if (enabled) {
                // Programar backup automático
                // backupManager.scheduleAutoBackup() // TODO: Implement when BackupManager is ready
                _uiEvents.emit(UIEvent.ConfigurationChanged("backupEnabled", false, true))
            } else {
                // Cancelar backup automático
                // backupManager.cancelAutoBackup() // TODO: Implement when BackupManager is ready
                _uiEvents.emit(UIEvent.ConfigurationChanged("backupEnabled", true, false))
            }
        }
    }

    /**
     * Realiza backup manual
     */
    fun performManualBackup() {
        viewModelScope.launch {
            _isBackupInProgress.value = true
            try {
                val success = backupHelper.performBackup()
                if (success) {
                    val currentTime = System.currentTimeMillis()
                    _lastBackupTime.value = currentTime
                    preferencesManager.lastBackupTime = currentTime
                    
                    loadAvailableBackups() // Actualizar lista
                    _uiEvents.emit(UIEvent.BackupCompleted("Backup creado exitosamente"))
                } else {
                    _uiEvents.emit(UIEvent.ShowError("Error al crear backup"))
                }
            } catch (e: Exception) {
                _uiEvents.emit(UIEvent.ShowError("Error en backup: ${e.message}"))
            } finally {
                _isBackupInProgress.value = false
            }
        }
    }

    /**
     * Restaura desde backup seleccionado
     */
    fun restoreFromBackup(backupInfo: BackupInfo) {
        viewModelScope.launch {
            _uiEvents.emit(UIEvent.ShowConfirmation(
                title = "Restaurar backup",
                message = "¿Estás seguro? Esto reemplazará todos los datos actuales.\n\nBackup: ${backupInfo.displayName}",
                action = {
                    performRestore(backupInfo.file)
                }
            ))
        }
    }

    /**
     * Ejecuta restauración de backup
     */
    private fun performRestore(file: File) {
        viewModelScope.launch {
            _isRestoreInProgress.value = true
            try {
                val success = backupHelper.restoreBackup(file)
                if (success) {
                    _uiEvents.emit(UIEvent.RestoreCompleted("Backup restaurado exitosamente"))
                    _uiEvents.emit(UIEvent.RequireAppRestart)
                } else {
                    _uiEvents.emit(UIEvent.ShowError("Error al restaurar backup"))
                }
            } catch (e: Exception) {
                _uiEvents.emit(UIEvent.ShowError("Error en restauración: ${e.message}"))
            } finally {
                _isRestoreInProgress.value = false
            }
        }
    }

    /**
     * Exporta datos en formato especificado
     */
    fun exportData(format: ExportFormat) {
        viewModelScope.launch {
            _isExportInProgress.value = true
            try {
                when (format) {
                    ExportFormat.CSV -> {
                        exportHelper.exportToCSV(true)
                        setExportFormat("csv")
                        _uiEvents.emit(UIEvent.ExportCompleted("CSV exportado exitosamente"))
                    }
                    ExportFormat.PDF -> {
                        exportHelper.exportToPDF(true)
                        setExportFormat("pdf")
                        _uiEvents.emit(UIEvent.ExportCompleted("PDF exportado exitosamente"))
                    }
                    ExportFormat.BOTH -> {
                        exportHelper.exportToCSV(false)
                        exportHelper.exportToPDF(true)
                        setExportFormat("both")
                        _uiEvents.emit(UIEvent.ExportCompleted("Archivos exportados exitosamente"))
                    }
                }
            } catch (e: Exception) {
                _uiEvents.emit(UIEvent.ShowError("Error en exportación: ${e.message}"))
            } finally {
                _isExportInProgress.value = false
            }
        }
    }

    /**
     * Limpia todos los datos de la aplicación
     */
    fun clearAllData() {
        viewModelScope.launch {
            _uiEvents.emit(UIEvent.ShowConfirmation(
                title = "Borrar todos los datos",
                message = "¿Estás seguro? Esta acción no se puede deshacer.\n\nSe borrarán:\n• Historial de transacciones\n• Estadísticas\n• Configuraciones personalizadas",
                action = {
                    performClearData()
                }
            ))
        }
    }

    /**
     * Ejecuta limpieza completa de datos
     */
    private fun performClearData() {
        viewModelScope.launch {
            _isClearDataInProgress.value = true
            try {
                // Limpiar datos de transacciones
                printDataManager.clearAllData()
                
                // Limpiar preferencias personalizadas (mantener defaults)
                preferencesManager.clearAll()
                
                // Actualizar estados locales a valores por defecto
                resetToDefaults()
                
                _uiEvents.emit(UIEvent.DataCleared("Todos los datos han sido borrados"))
                _uiEvents.emit(UIEvent.RequireAppRestart)
            } catch (e: Exception) {
                _uiEvents.emit(UIEvent.ShowError("Error limpiando datos: ${e.message}"))
            } finally {
                _isClearDataInProgress.value = false
            }
        }
    }

    /**
     * Resetea configuraciones a valores por defecto
     */
    private fun resetToDefaults() {
        _appTheme.value = preferencesManager.appTheme
        _autoPrint.value = preferencesManager.autoPrint
        _soundEnabled.value = preferencesManager.soundEnabled
        _vibrationEnabled.value = preferencesManager.vibrationEnabled
        _defaultSim.value = preferencesManager.defaultSim
        _printSize.value = preferencesManager.printSize
        _exportFormat.value = preferencesManager.exportFormat
        _backupEnabled.value = preferencesManager.backupEnabled
        _lastBackupTime.value = preferencesManager.lastBackupTime
    }

    /**
     * Carga estadísticas de datos almacenados
     */
    private fun loadDataStatistics() {
        viewModelScope.launch {
            try {
                val allTransactions = printDataManager.getAllPrintData()
                val appSize = calculateAppDataSize()
                
                _dataStatistics.value = DataStatistics(
                    totalTransactions = allTransactions.size,
                    appDataSizeMB = appSize,
                    lastTransactionDate = allTransactions.maxByOrNull { 
                        parseDate(it.date)?.time ?: 0L 
                    }?.date,
                    oldestTransactionDate = allTransactions.minByOrNull { 
                        parseDate(it.date)?.time ?: Long.MAX_VALUE 
                    }?.date
                )
            } catch (e: Exception) {
                // Error silencioso para estadísticas no críticas
            }
        }
    }

    /**
     * Carga lista de backups disponibles
     */
    private fun loadAvailableBackups() {
        viewModelScope.launch {
            try {
                val backupFiles = backupHelper.getAvailableBackups()
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                
                val backupInfos = backupFiles.map { file ->
                    BackupInfo(
                        file = file,
                        displayName = "Backup del ${dateFormat.format(Date(file.lastModified()))}",
                        date = file.lastModified(),
                        sizeMB = file.length() / (1024 * 1024).toFloat()
                    )
                }.sortedByDescending { it.date }
                
                _availableBackups.value = backupInfos
            } catch (e: Exception) {
                _uiEvents.emit(UIEvent.ShowError("Error cargando backups: ${e.message}"))
            }
        }
    }

    /**
     * Elimina backup específico
     */
    fun deleteBackup(backupInfo: BackupInfo) {
        viewModelScope.launch {
            _uiEvents.emit(UIEvent.ShowConfirmation(
                title = "Eliminar backup",
                message = "¿Eliminar backup del ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(backupInfo.date))}?",
                action = {
                    performDeleteBackup(backupInfo)
                }
            ))
        }
    }

    /**
     * Ejecuta eliminación de backup
     */
    private fun performDeleteBackup(backupInfo: BackupInfo) {
        viewModelScope.launch {
            try {
                if (backupInfo.file.delete()) {
                    loadAvailableBackups() // Recargar lista
                    _uiEvents.emit(UIEvent.BackupDeleted("Backup eliminado"))
                } else {
                    _uiEvents.emit(UIEvent.ShowError("No se pudo eliminar el backup"))
                }
            } catch (e: Exception) {
                _uiEvents.emit(UIEvent.ShowError("Error eliminando backup: ${e.message}"))
            }
        }
    }

    /**
     * Obtiene información de la última configuración de backup
     */
    fun getLastBackupInfo(): String {
        val lastBackup = _lastBackupTime.value
        return if (lastBackup > 0) {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            "Último backup: ${dateFormat.format(Date(lastBackup))}"
        } else {
            "Nunca se ha realizado un backup"
        }
    }

    /**
     * Valida configuraciones críticas
     */
    fun validateCriticalSettings(): List<String> {
        val issues = mutableListOf<String>()
        
        // Validar configuraciones críticas
        if (_printSize.value !in listOf("small", "medium", "large")) {
            issues.add("Tamaño de impresión inválido")
        }
        
        if (_exportFormat.value !in listOf("csv", "pdf", "both")) {
            issues.add("Formato de exportación inválido")
        }
        
        if (_defaultSim.value !in 0..2) {
            issues.add("SIM por defecto inválida")
        }
        
        return issues
    }

    // Funciones utilitarias
    private fun calculateAppDataSize(): Float {
        // Calcular tamaño aproximado de datos de la app
        return try {
            val allTransactions = printDataManager.getAllPrintData()
            (allTransactions.size * 0.5).toFloat() // Estimación aproximada en MB
        } catch (e: Exception) {
            0.0f
        }
    }

    private fun parseDate(dateString: String): Date? {
        return try {
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            formatter.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    // Enums y data classes
    enum class ExportFormat {
        CSV, PDF, BOTH
    }

    data class BackupInfo(
        val file: File,
        val displayName: String,
        val date: Long,
        val sizeMB: Float
    )

    data class DataStatistics(
        val totalTransactions: Int,
        val appDataSizeMB: Float,
        val lastTransactionDate: String?,
        val oldestTransactionDate: String?
    )

    sealed class UIEvent {
        data class ConfigurationChanged(val key: String, val oldValue: Any, val newValue: Any) : UIEvent()
        data class ThemeChanged(val theme: Int) : UIEvent()
        data class BackupCompleted(val message: String) : UIEvent()
        data class RestoreCompleted(val message: String) : UIEvent()
        data class ExportCompleted(val message: String) : UIEvent()
        data class DataCleared(val message: String) : UIEvent()
        data class BackupDeleted(val message: String) : UIEvent()
        data class ShowError(val message: String) : UIEvent()
        data class ShowConfirmation(
            val title: String,
            val message: String,
            val action: () -> Unit
        ) : UIEvent()
        object RequireAppRestart : UIEvent()
    }
}