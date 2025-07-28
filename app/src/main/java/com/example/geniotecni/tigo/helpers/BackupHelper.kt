package com.example.geniotecni.tigo.helpers

import android.content.Context
import android.os.Build
import android.os.Environment
import com.example.geniotecni.tigo.data.database.AppDatabase
import com.example.geniotecni.tigo.managers.PreferencesManager
import com.example.geniotecni.tigo.managers.PrintDataManager
import com.example.geniotecni.tigo.models.PrintData
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import com.example.geniotecni.tigo.utils.AppLogger

class BackupHelper(private val context: Context) {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .setDateFormat("yyyy-MM-dd HH:mm:ss")
        .create()

    private val printDataManager = PrintDataManager(context)
    private val preferencesManager = PreferencesManager(context)
    private val database = AppDatabase.getDatabase(context)

    companion object {
        private const val BACKUP_FOLDER = "GenioTecni_Backups"
        private const val BACKUP_FILE_PREFIX = "backup_"
        private const val BACKUP_FILE_EXTENSION = ".json"
        private const val BACKUP_VERSION = "2.0"
        private const val MAX_BACKUPS = 10
    }

    data class BackupData(
        val version: String,
        val timestamp: Long,
        val deviceInfo: DeviceInfo,
        val printHistory: List<PrintData>,
        val preferences: Map<String, Any>,
        val statistics: Map<String, Any>,
        val customLayouts: Map<String, Any>? = null
    )

    data class DeviceInfo(
        val androidVersion: String,
        val deviceModel: String,
        val appVersion: String
    )

    fun performBackup(): Boolean {
        AppLogger.i("BackupHelper", "Iniciando proceso de backup")
        val startTime = System.currentTimeMillis()
        return try {
            val backupData = createBackupData()
            AppLogger.logDataProcessing("BackupHelper", "Crear datos de backup", "BackupData", 1, System.currentTimeMillis() - startTime)
            
            val backupFile = saveBackupFile(backupData)
            AppLogger.logFileOperation("BackupHelper", "Guardar backup", backupFile?.name ?: "desconocido", backupFile != null, backupFile?.length() ?: 0)

            if (backupFile != null) {
                preferencesManager.lastBackupTime = System.currentTimeMillis()
                cleanOldBackups()
                val totalTime = System.currentTimeMillis() - startTime
                AppLogger.i("BackupHelper", "Backup completado exitosamente en ${totalTime}ms - Archivo: ${backupFile.name}")
                true
            } else {
                AppLogger.e("BackupHelper", "Error: No se pudo crear el archivo de backup")
                false
            }
        } catch (e: Exception) {
            AppLogger.e("BackupHelper", "Error crítico durante el backup", e)
            false
        }
    }

    fun restoreBackup(backupFile: File): Boolean {
        AppLogger.i("BackupHelper", "Iniciando restauración desde: ${backupFile.name}")
        AppLogger.logFileOperation("BackupHelper", "Leer backup", backupFile.name, true, backupFile.length())
        val startTime = System.currentTimeMillis()
        
        return try {
            val jsonContent = backupFile.readText()
            AppLogger.d("BackupHelper", "Archivo JSON leído: ${jsonContent.length} caracteres")
            
            val backupData = gson.fromJson(jsonContent, BackupData::class.java)
            AppLogger.i("BackupHelper", "Backup parseado - Versión: ${backupData.version}, Timestamp: ${backupData.timestamp}")

            // Validate backup version
            if (backupData.version != BACKUP_VERSION) {
                AppLogger.w("BackupHelper", "Versión de backup incompatible: ${backupData.version} vs $BACKUP_VERSION")
                // Handle version mismatch if needed
            }

            // Restore print history
            AppLogger.d("BackupHelper", "Restaurando historial de impresión: ${backupData.printHistory.size} elementos")
            restorePrintHistory(backupData.printHistory)

            // Restore preferences
            AppLogger.d("BackupHelper", "Restaurando preferencias: ${backupData.preferences.size} elementos")
            restorePreferences(backupData.preferences)

            // Restore custom layouts if available
            backupData.customLayouts?.let { 
                AppLogger.d("BackupHelper", "Restaurando layouts personalizados: ${it.size} elementos")
                restoreCustomLayouts(it) 
            }

            val totalTime = System.currentTimeMillis() - startTime
            AppLogger.i("BackupHelper", "Restauración completada exitosamente en ${totalTime}ms")
            true
        } catch (e: Exception) {
            AppLogger.e("BackupHelper", "Error crítico durante la restauración", e)
            false
        }
    }

    fun getAvailableBackups(): List<File> {
        AppLogger.d("BackupHelper", "Buscando backups disponibles")
        val backupDir = getBackupDirectory()
        AppLogger.d("BackupHelper", "Directorio de backups: ${backupDir.absolutePath}")
        
        return if (backupDir.exists()) {
            val files = backupDir.listFiles { file ->
                file.name.startsWith(BACKUP_FILE_PREFIX) &&
                        file.name.endsWith(BACKUP_FILE_EXTENSION)
            }?.sortedByDescending { it.lastModified() } ?: emptyList()
            AppLogger.i("BackupHelper", "Encontrados ${files.size} backups disponibles")
            files
        } else {
            AppLogger.w("BackupHelper", "Directorio de backups no existe: ${backupDir.absolutePath}")
            emptyList()
        }
    }

    fun performAutomaticBackup() {
        AppLogger.d("BackupHelper", "Verificando si es necesario backup automático")
        val lastBackup = preferencesManager.lastBackupTime
        val now = System.currentTimeMillis()
        val oneDayInMillis = 24 * 60 * 60 * 1000
        val timeSinceLastBackup = now - lastBackup
        
        AppLogger.d("BackupHelper", "Backup habilitado: ${preferencesManager.backupEnabled}")
        AppLogger.d("BackupHelper", "Tiempo desde último backup: ${timeSinceLastBackup / 1000 / 60} minutos")

        if (preferencesManager.backupEnabled && timeSinceLastBackup > oneDayInMillis) {
            AppLogger.i("BackupHelper", "Iniciando backup automático")
            val success = performBackup()
            AppLogger.i("BackupHelper", "Backup automático ${if (success) "exitoso" else "fallido"}")
        } else {
            AppLogger.d("BackupHelper", "Backup automático no necesario")
        }
    }

    private fun createBackupData(): BackupData {
        AppLogger.d("BackupHelper", "Creando datos de backup")
        val startTime = System.currentTimeMillis()
        
        val deviceInfo = getDeviceInfo()
        AppLogger.d("BackupHelper", "Información del dispositivo obtenida")
        
        val printHistory = printDataManager.getAllPrintData()
        AppLogger.d("BackupHelper", "Historial de impresión obtenido: ${printHistory.size} elementos")
        
        val preferences = getPreferencesMap()
        AppLogger.d("BackupHelper", "Preferencias obtenidas: ${preferences.size} elementos")
        
        val statistics = getStatisticsMap()
        AppLogger.d("BackupHelper", "Estadísticas obtenidas: ${statistics.size} elementos")
        
        val customLayouts = getCustomLayoutsMap()
        AppLogger.d("BackupHelper", "Layouts personalizados obtenidos: ${customLayouts.size} elementos")
        
        val backupData = BackupData(
            version = BACKUP_VERSION,
            timestamp = System.currentTimeMillis(),
            deviceInfo = deviceInfo,
            printHistory = printHistory,
            preferences = preferences,
            statistics = statistics,
            customLayouts = customLayouts
        )
        
        val createTime = System.currentTimeMillis() - startTime
        AppLogger.d("BackupHelper", "Datos de backup creados en ${createTime}ms")
        return backupData
    }

    private fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            androidVersion = Build.VERSION.RELEASE,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            appVersion = getAppVersion()
        )
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getPreferencesMap(): Map<String, Any> {
        return mapOf(
            "theme" to preferencesManager.appTheme,
            "autoPrint" to preferencesManager.autoPrint,
            "soundEnabled" to preferencesManager.soundEnabled,
            "vibrationEnabled" to preferencesManager.vibrationEnabled,
            "defaultSim" to preferencesManager.defaultSim,
            "exportFormat" to preferencesManager.exportFormat,
            "backupEnabled" to preferencesManager.backupEnabled
        )
    }

    private fun getStatisticsMap(): Map<String, Any> = runBlocking {
        withContext(Dispatchers.IO) {
            val transactionDao = database.transactionDao()
            mapOf(
                "totalTransactions" to transactionDao.getTransactionCount(),
                "totalAmount" to (transactionDao.getTotalAmount() ?: 0L),
                "totalCommission" to (transactionDao.getTotalCommission() ?: 0L),
                "mostUsedService" to (transactionDao.getMostUsedServiceName() ?: "N/A")
            )
        }
    }

    private fun getCustomLayoutsMap(): Map<String, Any> {
        // Get all custom layout configurations
        val layoutConfigs = mutableMapOf<String, Any>()

        // Add service-specific configurations
        val services = listOf("giros_tigo", "ande", "reseteo_cliente", "telefonia_tigo")
        services.forEach { service ->
            val config = mutableMapOf<String, Any>()
            config["scale"] = 1.0f // preferencesManager.getFloat("${service}_scale", 1.0f)
            config["textSize"] = 16f // preferencesManager.getFloat("${service}_text_size", 16f)
            config["padding"] = 16f // preferencesManager.getFloat("${service}_padding", 16f)
            config["letterSpacing"] = 0f // preferencesManager.getFloat("${service}_letter_spacing", 0f)
            layoutConfigs[service] = config
        }

        return layoutConfigs
    }

    private fun saveBackupFile(backupData: BackupData): File? {
        return try {
            val backupDir = getBackupDirectory()
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val timestamp = dateFormat.format(Date())
            val fileName = "$BACKUP_FILE_PREFIX$timestamp$BACKUP_FILE_EXTENSION"
            val backupFile = File(backupDir, fileName)

            FileWriter(backupFile).use { writer ->
                gson.toJson(backupData, writer)
            }

            backupFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getBackupDirectory(): File {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), BACKUP_FOLDER)
        } else {
            @Suppress("DEPRECATION")
            File(Environment.getExternalStorageDirectory(), "Documents/$BACKUP_FOLDER")
        }
    }

    private fun restorePrintHistory(printHistory: List<PrintData>) {
        // Clear existing data
        printDataManager.clearAllData()

        // Restore each print data
        printHistory.forEach { printData ->
            printDataManager.savePrintData(printData)
        }

        // Also save to Room database
        runBlocking {
            withContext(Dispatchers.IO) {
                val repository = com.example.geniotecni.tigo.data.repository.TransactionRepository(
                    database.transactionDao(),
                    context
                )
                printHistory.forEach { printData ->
                    repository.insertFromPrintData(printData)
                }
            }
        }
    }

    private fun restorePreferences(preferences: Map<String, Any>) {
        preferences["theme"]?.let {
            preferencesManager.appTheme = when (it) {
                is Double -> it.toInt()
                is Int -> it
                else -> 2
            }
        }
        preferences["autoPrint"]?.let {
            preferencesManager.autoPrint = it as Boolean
        }
        preferences["soundEnabled"]?.let {
            preferencesManager.soundEnabled = it as Boolean
        }
        preferences["vibrationEnabled"]?.let {
            preferencesManager.vibrationEnabled = it as Boolean
        }
        preferences["defaultSim"]?.let {
            preferencesManager.defaultSim = when (it) {
                is Double -> it.toInt()
                is Int -> it
                else -> 0
            }
        }
        preferences["exportFormat"]?.let {
            preferencesManager.exportFormat = when (it) {
                is String -> it
                else -> "csv"
            }
        }
        preferences["backupEnabled"]?.let {
            preferencesManager.backupEnabled = it as Boolean
        }
    }

    private fun restoreCustomLayouts(layouts: Map<String, Any>) {
        layouts.forEach { (service, config) ->
            if (config is Map<*, *>) {
                config["scale"]?.let {
                    val value = when (it) {
                        is Double -> it.toFloat()
                        is Float -> it
                        else -> 1.0f
                    }
                    preferencesManager.setFloat("${service}_scale", value)
                }
                config["textSize"]?.let {
                    val value = when (it) {
                        is Double -> it.toFloat()
                        is Float -> it
                        else -> 16f
                    }
                    preferencesManager.setFloat("${service}_text_size", value)
                }
                config["padding"]?.let {
                    val value = when (it) {
                        is Double -> it.toFloat()
                        is Float -> it
                        else -> 16f
                    }
                    preferencesManager.setFloat("${service}_padding", value)
                }
                config["letterSpacing"]?.let {
                    val value = when (it) {
                        is Double -> it.toFloat()
                        is Float -> it
                        else -> 0f
                    }
                    preferencesManager.setFloat("${service}_letter_spacing", value)
                }
            }
        }
    }

    private fun cleanOldBackups() {
        AppLogger.d("BackupHelper", "Limpiando backups antiguos")
        val backups = getAvailableBackups()
        if (backups.size > MAX_BACKUPS) {
            val filesToDelete = backups.drop(MAX_BACKUPS)
            AppLogger.i("BackupHelper", "Eliminando ${filesToDelete.size} backups antiguos (máximo: $MAX_BACKUPS)")
            
            filesToDelete.forEach { file ->
                val deleted = file.delete()
                AppLogger.logFileOperation("BackupHelper", "Eliminar backup antiguo", file.name, deleted)
            }
        } else {
            AppLogger.d("BackupHelper", "No es necesario limpiar backups (${backups.size}/$MAX_BACKUPS)")
        }
    }
}