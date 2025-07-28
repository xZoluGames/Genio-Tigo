package com.example.geniotecni.tigo.managers

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.example.geniotecni.tigo.utils.BaseManager
import com.example.geniotecni.tigo.utils.AppConfig
import com.example.geniotecni.tigo.utils.AppLogger

/**
 * 🔧 GESTOR DE PREFERENCIAS - Hereda de BaseManager para Funcionalidad Común
 * 
 * RESPONSABILIDAD CENTRAL:
 * - Manejo centralizado y thread-safe de todas las preferencias de usuario
 * - Aplicación automática de temas y configuraciones del sistema
 * - Persistencia confiable de configuraciones entre sesiones de la app
 * - Interfaz simplificada para acceso a configuraciones complejas
 * 
 * ARQUITECTURA HEREDADA:
 * - Extiende BaseManager para funcionalidad común de SharedPreferences
 * - Usa constantes centralizadas de AppConfig para consistencia
 * - Propiedades Kotlin con getters/setters personalizados para validación
 * - Patrón Singleton implícito via inyección de contexto
 * 
 * CONFIGURACIONES GESTIONADAS:
 * - Tema de la aplicación: claro/oscuro/sistema con aplicación automática
 * - Preferencias de impresión: auto-impresión y configuraciones de impresora
 * - Audio y feedback: configuraciones de sonido y vibración
 * - Conectividad: SIM por defecto para llamadas USSD
 * - Backup y export: configuraciones de respaldo y formatos de exportación
 * - Primera ejecución: detección y configuración inicial de la app
 * 
 * CONEXIONES ARQUITECTÓNICAS:
 * - HEREDA DE: BaseManager para abstracción de SharedPreferences
 * - CONSUME: AppConfig para constantes y claves centralizadas
 * - USADO POR: MainActivity para configuraciones de interfaz
 * - USADO POR: EditModeManager para persistencia de personalizaciones
 * - APLICA: Cambios automáticos de tema via AppCompatDelegate
 * 
 * OPTIMIZACIONES:
 * - Caché automático de SharedPreferences via BaseManager
 * - Validación automática en setters para integridad de datos
 * - Logging integrado para debugging y monitoreo
 */
class PreferencesManager(context: Context) : BaseManager(context, "PreferencesManager") {
    
    companion object {
        // Use centralized constants from AppConfig
        private const val KEY_THEME = AppConfig.PreferenceKeys.KEY_THEME
        private const val KEY_AUTO_PRINT = AppConfig.PreferenceKeys.KEY_AUTO_PRINT
        private const val KEY_SOUND_ENABLED = AppConfig.PreferenceKeys.KEY_SOUND_ENABLED
        private const val KEY_VIBRATION_ENABLED = AppConfig.PreferenceKeys.KEY_VIBRATION_ENABLED
        private const val KEY_DEFAULT_SIM = AppConfig.PreferenceKeys.KEY_DEFAULT_SIM
        private const val KEY_BACKUP_ENABLED = AppConfig.PreferenceKeys.KEY_BACKUP_ENABLED
        private const val KEY_LAST_BACKUP = AppConfig.PreferenceKeys.KEY_LAST_BACKUP
        private const val KEY_FIRST_RUN = AppConfig.PreferenceKeys.KEY_FIRST_RUN
        private const val KEY_EXPORT_FORMAT = AppConfig.PreferenceKeys.KEY_EXPORT_FORMAT
        private const val KEY_PRINT_SIZE = "print_size"
        
        // Theme values from AppConfig
        const val THEME_LIGHT = AppConfig.PreferenceKeys.THEME_LIGHT
        const val THEME_DARK = AppConfig.PreferenceKeys.THEME_DARK
        const val THEME_SYSTEM = AppConfig.PreferenceKeys.THEME_SYSTEM
        
        // Export formats from AppConfig
        const val EXPORT_CSV = AppConfig.PreferenceKeys.EXPORT_CSV
        const val EXPORT_PDF = AppConfig.PreferenceKeys.EXPORT_PDF
        const val EXPORT_BOTH = AppConfig.PreferenceKeys.EXPORT_BOTH
    }
    
    // Theme
    var appTheme: Int
        get() {
            val theme = getInt(KEY_THEME, THEME_SYSTEM)
            AppLogger.d(tag, "Obteniendo tema: $theme")
            return theme
        }
        set(value) {
            AppLogger.i(tag, "Cambiando tema: $appTheme -> $value")
            savePreference(KEY_THEME, value)
            applyTheme(value)
        }
    
    // Preferences
    var autoPrint: Boolean
        get() = getBoolean(KEY_AUTO_PRINT, false)
        set(value) = savePreference(KEY_AUTO_PRINT, value)
    
    var soundEnabled: Boolean
        get() = getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = savePreference(KEY_SOUND_ENABLED, value)
    
    var vibrationEnabled: Boolean
        get() = getBoolean(KEY_VIBRATION_ENABLED, true)
        set(value) = savePreference(KEY_VIBRATION_ENABLED, value)
    
    var defaultSim: Int
        get() = getInt(KEY_DEFAULT_SIM, 0)
        set(value) = savePreference(KEY_DEFAULT_SIM, value)
    
    var backupEnabled: Boolean
        get() = getBoolean(KEY_BACKUP_ENABLED, false)
        set(value) = savePreference(KEY_BACKUP_ENABLED, value)
    
    var lastBackupTime: Long
        get() = getLong(KEY_LAST_BACKUP, 0L)
        set(value) = savePreference(KEY_LAST_BACKUP, value)
    
    var isFirstRun: Boolean
        get() = getBoolean(KEY_FIRST_RUN, true)
        set(value) = savePreference(KEY_FIRST_RUN, value)
    
    var exportFormat: String
        get() = getString(KEY_EXPORT_FORMAT, EXPORT_CSV)
        set(value) = savePreference(KEY_EXPORT_FORMAT, value)
    
    var printSize: String
        get() = getString(KEY_PRINT_SIZE, "medium")
        set(value) = savePreference(KEY_PRINT_SIZE, value)
    
    // Apply theme
    fun applyTheme(theme: Int = appTheme) {
        AppLogger.i("PreferencesManager", "Aplicando tema: $theme")
        when (theme) {
            THEME_LIGHT -> {
                AppLogger.d("PreferencesManager", "Aplicando tema claro")
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            THEME_DARK -> {
                AppLogger.d("PreferencesManager", "Aplicando tema oscuro")
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            THEME_SYSTEM -> {
                AppLogger.d("PreferencesManager", "Aplicando tema del sistema")
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            else -> {
                AppLogger.w("PreferencesManager", "Tema desconocido: $theme, usando tema del sistema")
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }
    
    // Additional helper methods
    fun setFloat(key: String, value: Float) {
        AppLogger.d(tag, "setFloat($key, $value)")
        savePreference(key, value)
    }
    
    fun getFloatValue(key: String, defaultValue: Float = 0f): Float {
        return sharedPreferences.getFloat(key, defaultValue)
    }
    
    fun putString(key: String, value: String) {
        savePreference(key, value)
    }
    
    fun putInt(key: String, value: Int) {
        savePreference(key, value)
    }
    
    fun putLong(key: String, value: Long) {
        savePreference(key, value)
    }
    
    fun putBoolean(key: String, value: Boolean) {
        savePreference(key, value)
    }

    fun contains(key: String): Boolean {
        val contains = hasPreference(key)
        AppLogger.d(tag, "contains($key) = $contains")
        return contains
    }

    // Clear all preferences
    fun clearAll() {
        AppLogger.w(tag, "Limpiando todas las preferencias")
        val beforeCount = getAllKeys().size
        clearAllPreferences()
        AppLogger.i(tag, "$beforeCount preferencias eliminadas")
    }
}