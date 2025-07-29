package com.example.geniotecni.tigo.utils

/**
 * Configuración básica de la aplicación
 */
object AppConfig {
    
    object PreferenceKeys {
        const val KEY_THEME = "theme"
        const val KEY_AUTO_PRINT = "auto_print"
        const val KEY_SOUND_ENABLED = "sound_enabled"
        const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        const val KEY_DEFAULT_SIM = "default_sim"
        const val KEY_BACKUP_ENABLED = "backup_enabled"
        const val KEY_LAST_BACKUP = "last_backup"
        const val KEY_FIRST_RUN = "first_run"
        const val KEY_EXPORT_FORMAT = "export_format"
        const val KEY_DEVICE_ADDRESS = "device_address"
        const val KEY_DEVICE_NAME = "device_name"
        
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2
        const val THEME_SYSTEM = 0
        
        const val EXPORT_CSV = "csv"
        const val EXPORT_PDF = "pdf"
        const val EXPORT_BOTH = "both"
    }
    
    // Permissions y Database objects eliminados - no utilizados en el proyecto
    
    object Files {
        const val EXPORT_DIR = "GenioTigo"
        const val BACKUP_DIR = "Backup"
        const val USER_DATA_FILE = "user_data.json"
        const val PRINT_HISTORY_FILE = "print_history.json"
    }
    
    const val PREFS_NAME = "genio_tigo_prefs"
    const val BLUETOOTH_PREFS = "bluetooth_prefs"
    
    object Bluetooth {
        const val CONNECTION_TIMEOUT = 10000L
        const val DISCOVERY_TIMEOUT = 12000L
        const val UUID = "00001101-0000-1000-8000-00805F9B34FB"
    }
    
    object Formats {
        const val DATE_FORMAT = "dd/MM/yyyy"
        const val TIME_FORMAT = "HH:mm:ss"
        const val DATETIME_FORMAT = "dd/MM/yyyy HH:mm"
        const val DECIMAL_FORMAT = "#,###"
    }
    
    // Business object eliminado - constantes no utilizadas en el proyecto
}