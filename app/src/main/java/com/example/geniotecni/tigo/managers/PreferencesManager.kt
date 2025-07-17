package com.example.geniotecni.tigo.managers

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.example.geniotecni.tigo.utils.Constants

class PreferencesManager(context: Context) {
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        // Keys
        private const val KEY_THEME = "app_theme"
        private const val KEY_AUTO_PRINT = "auto_print"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_DEFAULT_SIM = "default_sim"
        private const val KEY_BACKUP_ENABLED = "backup_enabled"
        private const val KEY_LAST_BACKUP = "last_backup"
        private const val KEY_FIRST_RUN = "first_run"
        private const val KEY_EXPORT_FORMAT = "export_format"
        
        // Theme values
        const val THEME_LIGHT = 0
        const val THEME_DARK = 1
        const val THEME_SYSTEM = 2
        
        // Export formats
        const val EXPORT_CSV = 0
        const val EXPORT_PDF = 1
        const val EXPORT_BOTH = 2
    }
    
    // Theme
    var appTheme: Int
        get() = prefs.getInt(KEY_THEME, THEME_SYSTEM)
        set(value) {
            prefs.edit().putInt(KEY_THEME, value).apply()
            applyTheme(value)
        }
    
    // Preferences
    var autoPrint: Boolean
        get() = prefs.getBoolean(KEY_AUTO_PRINT, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_PRINT, value).apply()
    
    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()
    
    var vibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, value).apply()
    
    var defaultSim: Int
        get() = prefs.getInt(KEY_DEFAULT_SIM, 0)
        set(value) = prefs.edit().putInt(KEY_DEFAULT_SIM, value).apply()
    
    var backupEnabled: Boolean
        get() = prefs.getBoolean(KEY_BACKUP_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BACKUP_ENABLED, value).apply()
    
    var lastBackupTime: Long
        get() = prefs.getLong(KEY_LAST_BACKUP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BACKUP, value).apply()
    
    var isFirstRun: Boolean
        get() = prefs.getBoolean(KEY_FIRST_RUN, true)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_RUN, value).apply()
    
    var exportFormat: Int
        get() = prefs.getInt(KEY_EXPORT_FORMAT, EXPORT_CSV)
        set(value) = prefs.edit().putInt(KEY_EXPORT_FORMAT, value).apply()
    
    // Apply theme
    fun applyTheme(theme: Int = appTheme) {
        when (theme) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            THEME_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
    
    // Generic getter/setter methods
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return prefs.getInt(key, defaultValue)
    }
    
    fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }
    
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return prefs.getLong(key, defaultValue)
    }
    
    fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }
    
    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return prefs.getFloat(key, defaultValue)
    }
    
    fun putFloat(key: String, value: Float) {
        prefs.edit().putFloat(key, value).apply()
    }
    
    fun getString(key: String, defaultValue: String? = null): String? {
        return prefs.getString(key, defaultValue)
    }
    
    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
    
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }
    
    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    // Clear all preferences
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}