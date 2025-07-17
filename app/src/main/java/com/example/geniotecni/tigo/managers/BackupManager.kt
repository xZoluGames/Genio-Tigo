package com.example.geniotecni.tigo

import android.content.Context
import com.example.geniotecni.tigo.managers.PreferencesManager
import com.example.geniotecni.tigo.managers.PrintDataManager
import com.google.gson.Gson
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupManager(private val context: Context) {

    private val prefsManager = PreferencesManager(context)

    fun performBackup(): Boolean {
        if (!prefsManager.backupEnabled) return false

        return try {
            // Backup de historial de impresiones
            val printDataManager = PrintDataManager(context)
            val history = printDataManager.getAllPrintData()

            val backupData = mapOf(
                "version" to 1,
                "timestamp" to System.currentTimeMillis(),
                "history" to history,
                "preferences" to getPreferencesBackup()
            )

            val gson = Gson()
            val json = gson.toJson(backupData)

            val fileName = "backup_geniotecni_${
                SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(
                    Date()
                )}.json"
            val file = File(context.getExternalFilesDir(null), "backups")
            if (!file.exists()) file.mkdirs()

            val backupFile = File(file, fileName)
            backupFile.writeText(json)

            prefsManager.lastBackupTime = System.currentTimeMillis()

            // Limpiar backups antiguos (mantener solo los últimos 7)
            cleanOldBackups(file)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getPreferencesBackup(): Map<String, Any> {
        return mapOf(
            "theme" to prefsManager.appTheme,
            "autoPrint" to prefsManager.autoPrint,
            "soundEnabled" to prefsManager.soundEnabled,
            "vibrationEnabled" to prefsManager.vibrationEnabled,
            "defaultSim" to prefsManager.defaultSim
        )
    }

    private fun cleanOldBackups(backupDir: File) {
        val backupFiles = backupDir.listFiles { file ->
            file.name.startsWith("backup_geniotecni_") && file.name.endsWith(".json")
        }?.sortedByDescending { it.lastModified() } ?: return

        // Mantener solo los últimos 7 backups
        if (backupFiles.size > 7) {
            backupFiles.drop(7).forEach { it.delete() }
        }
    }

    fun restoreBackup(backupFile: File): Boolean {
        return try {
            val json = backupFile.readText()
            val gson = Gson()
            val backupData = gson.fromJson(json, Map::class.java)

            // Restaurar historial
            @Suppress("UNCHECKED_CAST")
            val history = backupData["history"] as? List<Map<String, Any>>
            // TODO: Implementar restauración del historial

            // Restaurar preferencias
            @Suppress("UNCHECKED_CAST")
            val prefs = backupData["preferences"] as? Map<String, Any>
            prefs?.let {
                prefsManager.appTheme = (it["theme"] as? Double)?.toInt() ?: 2
                prefsManager.autoPrint = it["autoPrint"] as? Boolean ?: false
                prefsManager.soundEnabled = it["soundEnabled"] as? Boolean ?: true
                prefsManager.vibrationEnabled = it["vibrationEnabled"] as? Boolean ?: true
                prefsManager.defaultSim = (it["defaultSim"] as? Double)?.toInt() ?: 0
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}