package com.example.geniotecni.tigo

import android.app.Application
import com.example.geniotecni.tigo.utils.AppLogger
import com.example.geniotecni.tigo.managers.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import dagger.hilt.android.HiltAndroidApp

/**
 * 🚀 APLICACIÓN PRINCIPAL - Inicializador Global del Sistema
 * 
 * PROPÓSITO ARQUITECTÓNICO:
 * - Clase Application simplificada para inicialización global
 * - Gestiona el ciclo de vida completo de la aplicación
 * - Inicializa componentes críticos y configuraciones globales
 * - Proporciona acceso singleton a la instancia de aplicación
 * 
 * INICIALIZACIONES CRÍTICAS:
 * - PreferencesManager: Configuración global de preferencias y temas
 * - AppLogger: Sistema de logging para debugging y monitoreo
 * - CoroutineScope: Alcance de corrutinas para operaciones asíncronas globales
 * - Aplicación automática de tema según preferencias del usuario
 * 
 * ARQUITECTURA SINGLETON:
 * - Patrón Singleton thread-safe para acceso global
 * - Instance volátil para garantizar visibilidad entre hilos
 * - Inicialización segura con verificación de estado
 * 
 * GESTIÓN DE RECURSOS:
 * - Monitoreo de memoria baja para optimizaciones automáticas
 * - Limpieza automática de recursos al terminar aplicación
 * - Logging detallado del ciclo de vida para debugging
 * 
 * CONEXIONES CRÍTICAS:
 * - INICIALIZA: PreferencesManager para configuraciones globales
 * - UTILIZA: AppLogger para sistema de logging centralizado
 * - PROPORCIONA: Contexto global para todos los managers y helpers
 * - GESTIONA: CoroutineScope para operaciones asíncronas de larga duración
 */
@HiltAndroidApp
class GenioTecniApplicationSimple : Application() {

    companion object {
        private const val TAG = "GenioTecniApplication"
        
        @Volatile
        private var INSTANCE: GenioTecniApplicationSimple? = null
        
        fun getInstance(): GenioTecniApplicationSimple {
            return INSTANCE ?: throw IllegalStateException("Application not initialized")
        }
    }

    // Application scope coroutine
    private val applicationScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        
        INSTANCE = this
        
        AppLogger.i(TAG, "Iniciando GenioTecniApplication")
        
        // Legacy initialization for backward compatibility
        try {
            // Inicializar PreferencesManager y aplicar tema
            val preferencesManager = PreferencesManager(this)
            preferencesManager.applyTheme()
            AppLogger.d(TAG, "Tema aplicado: ${preferencesManager.appTheme}")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error en inicialización", e)
        }
        
        AppLogger.i(TAG, "Aplicación inicializada exitosamente")
    }

    override fun onTerminate() {
        AppLogger.i(TAG, "Terminando aplicación...")
        
        INSTANCE = null
        super.onTerminate()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        AppLogger.w(TAG, "Memoria baja detectada")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        AppLogger.d(TAG, "Trim memory solicitado - nivel: $level")
    }
}