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
        return try {
            val backupData = createBackupData()
            val backupFile = saveBackupFile(backupData)

            if (backupFile != null) {
                preferencesManager.lastBackupTime = System.currentTimeMillis()
                cleanOldBackups()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun restoreBackup(backupFile: File): Boolean {
        return try {
            val jsonContent = backupFile.readText()
            val backupData = gson.fromJson(jsonContent, BackupData::class.java)

            // Validate backup version
            if (backupData.version != BACKUP_VERSION) {
                // Handle version mismatch if needed
            }

            // Restore print history
            restorePrintHistory(backupData.printHistory)

            // Restore preferences
            restorePreferences(backupData.preferences)

            // Restore custom layouts if available
            backupData.customLayouts?.let { restoreCustomLayouts(it) }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getAvailableBackups(): List<File> {
        val backupDir = getBackupDirectory()
        return if (backupDir.exists()) {
            backupDir.listFiles { file ->
                file.name.startsWith(BACKUP_FILE_PREFIX) &&
                        file.name.endsWith(BACKUP_FILE_EXTENSION)
            }?.sortedByDescending { it.lastModified() } ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun performAutomaticBackup() {
        val lastBackup = preferencesManager.lastBackupTime
        val now = System.currentTimeMillis()
        val oneDayInMillis = 24 * 60 * 60 * 1000

        if (preferencesManager.backupEnabled && (now - lastBackup) > oneDayInMillis) {
            performBackup()
        }
    }

    private fun createBackupData(): BackupData {
        return BackupData(
            version = BACKUP_VERSION,
            timestamp = System.currentTimeMillis(),
            deviceInfo = getDeviceInfo(),
            printHistory = printDataManager.getAllPrintData(),
            preferences = getPreferencesMap(),
            statistics = getStatisticsMap(),
            customLayouts = getCustomLayoutsMap()
        )
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
            config["scale"] = preferencesManager.getFloat("${service}_scale", 1.0f)
            config["textSize"] = preferencesManager.getFloat("${service}_text_size", 16f)
            config["padding"] = preferencesManager.getFloat("${service}_padding", 16f)
            config["letterSpacing"] = preferencesManager.getFloat("${service}_letter_spacing", 0f)
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
                    database.transactionDao()
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
                is Double -> it.toInt()
                is Int -> it
                else -> 0
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
                    preferencesManager.putFloat("${service}_scale", value)
                }
                config["textSize"]?.let {
                    val value = when (it) {
                        is Double -> it.toFloat()
                        is Float -> it
                        else -> 16f
                    }
                    preferencesManager.putFloat("${service}_text_size", value)
                }
                config["padding"]?.let {
                    val value = when (it) {
                        is Double -> it.toFloat()
                        is Float -> it
                        else -> 16f
                    }
                    preferencesManager.putFloat("${service}_padding", value)
                }
                config["letterSpacing"]?.let {
                    val value = when (it) {
                        is Double -> it.toFloat()
                        is Float -> it
                        else -> 0f
                    }
                    preferencesManager.putFloat("${service}_letter_spacing", value)
                }
            }
        }
    }

    private fun cleanOldBackups() {
        val backups = getAvailableBackups()
        if (backups.size > MAX_BACKUPS) {
            // Delete oldest backups
            backups.drop(MAX_BACKUPS).forEach { file ->
                file.delete()
            }
        }
    }
}