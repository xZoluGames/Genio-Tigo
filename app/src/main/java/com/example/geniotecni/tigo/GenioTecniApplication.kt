package com.example.geniotecni.tigo

import android.app.Application
import android.util.Log
import com.example.geniotecni.tigo.managers.PreferencesManager

class GenioTecniApplication : Application() {
    
    companion object {
        private const val TAG = "GenioTecniApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Aplicación iniciada")
        
        try {
            // Inicializar PreferencesManager y aplicar tema
            val preferencesManager = PreferencesManager(this)
            preferencesManager.applyTheme()
            Log.d(TAG, "Tema aplicado: ${preferencesManager.appTheme}")
            
            Log.d(TAG, "Aplicación inicializada correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar la aplicación", e)
        }
    }
}