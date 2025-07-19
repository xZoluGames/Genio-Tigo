package com.example.geniotecni.tigo.utils

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

object AppLogger {
    
    private const val DEFAULT_TAG = "GenioTigo"
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    
    fun d(tag: String = DEFAULT_TAG, message: String) {
        val timestamp = dateFormat.format(Date())
        Log.d(tag, "[$timestamp] $message")
    }
    
    fun i(tag: String = DEFAULT_TAG, message: String) {
        val timestamp = dateFormat.format(Date())
        Log.i(tag, "[$timestamp] $message")
    }
    
    fun w(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        val timestamp = dateFormat.format(Date())
        if (throwable != null) {
            Log.w(tag, "[$timestamp] $message", throwable)
        } else {
            Log.w(tag, "[$timestamp] $message")
        }
    }
    
    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        val timestamp = dateFormat.format(Date())
        if (throwable != null) {
            Log.e(tag, "[$timestamp] $message", throwable)
        } else {
            Log.e(tag, "[$timestamp] $message")
        }
    }
    
    fun v(tag: String = DEFAULT_TAG, message: String) {
        val timestamp = dateFormat.format(Date())
        Log.v(tag, "[$timestamp] $message")
    }
    
    // NUEVAS FUNCIONES PARA LOGGING AVANZADO
    fun logUserAction(tag: String = DEFAULT_TAG, action: String, details: String = "") {
        val timestamp = dateFormat.format(Date())
        i(tag, "🔴 ACCIÓN USUARIO: $action ${if (details.isNotEmpty()) "- $details" else ""}")
    }
    
    fun logImageLoad(tag: String = DEFAULT_TAG, imageName: String, resourceId: Int, success: Boolean, loadTime: Long = 0) {
        val timestamp = dateFormat.format(Date())
        val status = if (success) "✅ ÉXITO" else "❌ ERROR"
        val timeInfo = if (loadTime > 0) " (${loadTime}ms)" else ""
        i(tag, "🖼️ CARGA IMAGEN: $imageName (ID: $resourceId) - $status$timeInfo")
    }
    
    fun logButtonClick(tag: String = DEFAULT_TAG, buttonName: String, action: String = "") {
        val timestamp = dateFormat.format(Date())
        i(tag, "🔘 BOTÓN PRESIONADO: $buttonName ${if (action.isNotEmpty()) "-> $action" else ""}")
    }
    
    fun logServiceSelection(tag: String = DEFAULT_TAG, serviceName: String, serviceType: Int) {
        val timestamp = dateFormat.format(Date())
        i(tag, "🎯 SERVICIO SELECCIONADO: $serviceName (Tipo: $serviceType)")
    }
    
    fun logNavigationStart(tag: String = DEFAULT_TAG, from: String, to: String, extras: String = "") {
        val timestamp = dateFormat.format(Date())
        i(tag, "🚀 NAVEGACIÓN INICIADA: $from -> $to ${if (extras.isNotEmpty()) "[$extras]" else ""}")
    }
    
    fun logNavigationEnd(tag: String = DEFAULT_TAG, activity: String, duration: Long = 0) {
        val timestamp = dateFormat.format(Date())
        val timeInfo = if (duration > 0) " (${duration}ms)" else ""
        i(tag, "✅ NAVEGACIÓN COMPLETADA: $activity$timeInfo")
    }
    
    fun logPermissionRequest(tag: String = DEFAULT_TAG, permission: String, granted: Boolean) {
        val timestamp = dateFormat.format(Date())
        val status = if (granted) "✅ CONCEDIDO" else "❌ DENEGADO"
        i(tag, "🔐 PERMISO: $permission - $status")
    }
    
    fun logBluetoothEvent(tag: String = DEFAULT_TAG, event: String, device: String = "", success: Boolean = true) {
        val timestamp = dateFormat.format(Date())
        val status = if (success) "✅" else "❌"
        val deviceInfo = if (device.isNotEmpty()) " [$device]" else ""
        i(tag, "$status 📶 BLUETOOTH: $event$deviceInfo")
    }
    
    fun logPrintEvent(tag: String = DEFAULT_TAG, event: String, details: String = "", success: Boolean = true) {
        val timestamp = dateFormat.format(Date())
        val status = if (success) "✅" else "❌"
        i(tag, "$status 🖨️ IMPRESIÓN: $event ${if (details.isNotEmpty()) "- $details" else ""}")
    }
    
    fun logNetworkEvent(tag: String = DEFAULT_TAG, event: String, url: String = "", responseCode: Int = 0) {
        val timestamp = dateFormat.format(Date())
        val urlInfo = if (url.isNotEmpty()) " [$url]" else ""
        val responseInfo = if (responseCode > 0) " (${responseCode})" else ""
        i(tag, "🌐 RED: $event$urlInfo$responseInfo")
    }
    
    fun logDataProcessing(tag: String = DEFAULT_TAG, operation: String, dataType: String, count: Int = 0, duration: Long = 0) {
        val timestamp = dateFormat.format(Date())
        val countInfo = if (count > 0) " [$count items]" else ""
        val timeInfo = if (duration > 0) " (${duration}ms)" else ""
        i(tag, "⚙️ PROCESAMIENTO: $operation - $dataType$countInfo$timeInfo")
    }
    
    fun logFileOperation(tag: String = DEFAULT_TAG, operation: String, fileName: String, success: Boolean, size: Long = 0) {
        val timestamp = dateFormat.format(Date())
        val status = if (success) "✅" else "❌"
        val sizeInfo = if (size > 0) " (${size} bytes)" else ""
        i(tag, "$status 📁 ARCHIVO: $operation - $fileName$sizeInfo")
    }
    
    fun logValidation(tag: String = DEFAULT_TAG, field: String, value: String, isValid: Boolean, errorMessage: String = "") {
        val timestamp = dateFormat.format(Date())
        val status = if (isValid) "✅ VÁLIDO" else "❌ INVÁLIDO"
        val errorInfo = if (errorMessage.isNotEmpty() && !isValid) " - $errorMessage" else ""
        i(tag, "🔍 VALIDACIÓN: $field = '$value' - $status$errorInfo")
    }
    
    fun logSearchQuery(tag: String = DEFAULT_TAG, query: String, resultsCount: Int, duration: Long = 0) {
        val timestamp = dateFormat.format(Date())
        val timeInfo = if (duration > 0) " (${duration}ms)" else ""
        i(tag, "🔍 BÚSQUEDA: '$query' -> $resultsCount resultados$timeInfo")
    }
    
    fun logMemoryUsage(tag: String = DEFAULT_TAG, context: String = "") {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val usedMB = usedMemory / (1024 * 1024)
        val maxMB = maxMemory / (1024 * 1024)
        val percentage = (usedMemory * 100) / maxMemory
        val contextInfo = if (context.isNotEmpty()) " [$context]" else ""
        i(tag, "🧠 MEMORIA: ${usedMB}MB/${maxMB}MB (${percentage}%)$contextInfo")
    }
    
    // Funciones específicas para debugging
    fun debugViewState(tag: String, viewName: String, view: android.view.View) {
        d(tag, "=== ESTADO DE VISTA: $viewName ===")
        d(tag, "ID: ${view.id}")
        d(tag, "Visibility: ${view.visibility}")
        d(tag, "Width: ${view.width}, Height: ${view.height}")
        d(tag, "X: ${view.x}, Y: ${view.y}")
        d(tag, "TranslationX: ${view.translationX}, TranslationY: ${view.translationY}")
        d(tag, "ScaleX: ${view.scaleX}, ScaleY: ${view.scaleY}")
        d(tag, "Alpha: ${view.alpha}")
        d(tag, "Background: ${view.background}")
        if (view is android.widget.ImageView) {
            d(tag, "ImageView drawable: ${view.drawable}")
            d(tag, "ImageView scaleType: ${view.scaleType}")
            d(tag, "ImageView imageTintList: ${view.imageTintList}")
        }
        d(tag, "=== FIN ESTADO DE VISTA: $viewName ===")
    }
    
    fun debugResourceLoading(tag: String, resourceName: String, resourceId: Int, success: Boolean, error: Throwable? = null) {
        if (success) {
            d(tag, "✓ Recurso cargado exitosamente: $resourceName (ID: $resourceId)")
        } else {
            e(tag, "✗ Error cargando recurso: $resourceName (ID: $resourceId)", error)
        }
    }
    
    fun debugColorValue(tag: String, colorName: String, colorValue: Int) {
        val hexColor = String.format("#%08X", colorValue)
        d(tag, "Color $colorName: $hexColor ($colorValue)")
    }
    
    fun debugMethodStart(tag: String, methodName: String, vararg params: Any?) {
        val paramStr = if (params.isNotEmpty()) {
            params.joinToString(", ") { it.toString() }
        } else {
            "sin parámetros"
        }
        d(tag, "→ INICIO: $methodName($paramStr)")
    }
    
    fun debugMethodEnd(tag: String, methodName: String, result: Any? = null) {
        val resultStr = result?.let { " → $it" } ?: ""
        d(tag, "← FIN: $methodName$resultStr")
    }
    
    fun separator(tag: String = DEFAULT_TAG) {
        d(tag, "═══════════════════════════════════════════════════════════════")
    }
}