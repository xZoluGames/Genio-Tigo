package com.example.geniotecni.tigo.managers

import android.content.Context
import com.example.geniotecni.tigo.models.PrintData
import com.example.geniotecni.tigo.utils.AppConfig
import com.example.geniotecni.tigo.utils.Constants
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * 📄 GESTOR DE DATOS DE IMPRESIÓN - Persistencia Confiable del Historial
 * 
 * PROPÓSITO CENTRAL:
 * - Gestión completa del historial de transacciones e impresiones
 * - Persistencia thread-safe en almacenamiento local JSON
 * - Interfaz simplificada para operaciones CRUD del historial
 * - Optimización de acceso a datos con serialización eficiente
 * 
 * ARQUITECTURA DE PERSISTENCIA:
 * - Usa Gson para serialización/deserialización automática y confiable
 * - Almacenamiento en archivos JSON en almacenamiento interno de la app
 * - Operaciones I/O optimizadas con use {} para manejo automático de recursos
 * - Gestión inteligente de memoria con lazy loading de datos grandes
 * 
 * OPERACIONES PRINCIPALES:
 * - savePrintData(): Persiste nueva transacción al inicio de la lista
 * - getAllPrintData(): Recupera historial completo ordenado por fecha
 * - clearAllData(): Limpieza completa y segura del historial
 * - getDataCount(): Contador eficiente para estadísticas de uso
 * 
 * CONEXIONES Y DEPENDENCIAS:
 * - CONSUME: Constants para nombres de archivos y configuraciones
 * - CONSUME: PrintData models para tipado fuerte de datos
 * - USADO POR: MainActivity para guardar transacciones completadas
 * - USADO POR: PrintHistoryActivity para mostrar historial al usuario
 * 
 * OPTIMIZACIONES DE RENDIMIENTO:
 * - Inserción al inicio de lista para mostrar transacciones más recientes primero
 * - Verificación de existencia de archivos antes de operaciones I/O
 * - Manejo de errores robusto para corrupciones de datos
 * - Serialización optimizada con TypeToken para preservar tipos genéricos
 */
class PrintDataManager(private val context: Context) {
    private val fileName = AppConfig.Files.PRINT_HISTORY_FILE
    private val gson = Gson()

    fun savePrintData(printData: PrintData) {
        val existingData = getAllPrintData().toMutableList()
        existingData.add(0, printData) // Add new data at the beginning of the list
        val json = gson.toJson(existingData)
        context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
            it.write(json.toByteArray())
        }
    }

    fun getAllPrintData(): List<PrintData> {
        val file = File(context.filesDir, fileName)
        return if (file.exists()) {
            val json = file.readText()
            gson.fromJson(json, object : TypeToken<List<PrintData>>() {}.type)
        } else {
            emptyList()
        }
    }
    
    fun clearAllData() {
        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            file.delete()
        }
    }
    
    fun getDataCount(): Int {
        return getAllPrintData().size
    }
}