package com.example.geniotecni.tigo.helpers

import android.content.Context
import android.os.Build
import android.os.Environment
import com.example.geniotecni.tigo.managers.PreferencesManager
import com.example.geniotecni.tigo.managers.PrintDataManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class BackupHelper(private val context: Context) {
    
    private val preferencesManager = PreferencesManager(context)
    private val printDataManager = PrintDataManager(context)
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    
    companion object {
        private const val BACKUP_FOLDER = "GenioTecni_Backups"
        private const val BACKUP_VERSION = 1
        private const val MAX_BACKUPS = 7
    }
    
    data class BackupData(
        val version: Int,
        val timestamp: Long,
        val deviceInfo: DeviceInfo,
        val printHistory: List<Any>, // PrintData serialized
        val preferences: Map<String, Any>,
        val statistics: Map<String, Any>
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
            val json = backupFile.readText()
            val backupData = gson.fromJson(json, BackupData::class.java)
            
            // Verify backup version
            if (backupData.version != BACKUP_VERSION) {
                return false
            }
            
            // Restore preferences
            restorePreferences(backupData.preferences)
            
            // TODO: Restore print history
            // This would require modifying PrintDataManager to support bulk import
            
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
                file.name.startsWith("backup_") && file.name.endsWith(".json")
            }?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    private fun createBackupData(): BackupData {
        return BackupData(
            version = BACKUP_VERSION,
            timestamp = System.currentTimeMillis(),
            deviceInfo = getDeviceInfo(),
            printHistory = printDataManager.getAllPrintData(),
            preferences = getPreferencesMap(),
            statistics = getStatisticsMap()
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
            "exportFormat" to preferencesManager.exportFormat
        )
    }
    
    private fun getStatisticsMap(): Map<String, Any> {
        // TODO: Get statistics summary
        return mapOf(
            "totalTransactions" to printDataManager.getAllPrintData().size
        )
    }
    
    private fun restorePreferences(preferences: Map<String, Any>) {
        preferences["theme"]?.let {
            preferencesManager.appTheme = (it as Double).toInt()
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
            preferencesManager.defaultSim = (it as Double).toInt()
        }
        preferences["exportFormat"]?.let {
            preferencesManager.exportFormat = (it as Double).toInt()
        }
    }
    
    private fun saveBackupFile(backupData: BackupData): File? {
        return try {
            val json = gson.toJson(backupData)
            val fileName = "backup_${getTimestamp()}.json"
            val file = File(getBackupDirectory(), fileName)
            file.writeText(json)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun getBackupDirectory(): File {
        val dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), BACKUP_FOLDER)
        } else {
            @Suppress("DEPRECATION")
            File(Environment.getExternalStorageDirectory(), "Documents/$BACKUP_FOLDER")
        }
        
        if (!dir.exists()) {
            dir.mkdirs()
        }
        
        return dir
    }
    
    private fun cleanOldBackups() {
        val backups = getAvailableBackups()
        if (backups.size > MAX_BACKUPS) {
            backups.drop(MAX_BACKUPS).forEach { it.delete() }
        }
    }
    
    private fun getTimestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }
}