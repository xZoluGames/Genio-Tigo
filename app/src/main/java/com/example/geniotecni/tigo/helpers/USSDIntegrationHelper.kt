package com.example.geniotecni.tigo.helpers

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import com.example.geniotecni.tigo.data.repository.ServiceRepository
import com.example.geniotecni.tigo.models.ReferenceData
import com.example.geniotecni.tigo.utils.AppLogger
import com.example.geniotecni.tigo.utils.USSDConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 📱 INTEGRACIÓN USSD/SMS - Helper Complejo para Transacciones Automáticas
 * 
 * FUNCIONALIDAD PRINCIPAL:
 * - Generación automática de códigos USSD específicos por servicio
 * - Ejecución controlada de llamadas telefónicas con códigos USSD
 * - Observación en tiempo real de SMS de respuesta del sistema
 * - Extracción inteligente y automática de referencias de transacciones
 * - Gestión completa del flujo asíncrono USSD → SMS → Callback
 * 
 * FLUJO ASÍNCRONO COMPLETO:
 * 1. generateUSSDCode() → Genera código USSD según configuración de servicio
 * 2. executeUSSD() → Inicia llamada telefónica y configura observación SMS
 * 3. ContentObserver → Monitorea automáticamente SMS entrantes del sistema
 * 4. extractReferenceData() → Parsea y extrae referencias usando regex inteligente
 * 5. USSDCallback → Notifica resultado de forma asíncrona al caller
 * 
 * ARQUITECTURA ASÍNCRONA AVANZADA:
 * - Kotlin Coroutines para operaciones no bloqueantes y manejo de concurrencia
 * - ContentObserver para monitoreo SMS en tiempo real sin polling
 * - Timeout automático inteligente para evitar búsquedas infinitas
 * - Interface de callbacks para comunicación limpia con componentes UI
 * - Manejo robusto de errores y estados de la aplicación
 * 
 * DEPENDENCIAS CRÍTICAS DEL SISTEMA:
 * - ServiceRepository: Configuraciones centralizadas y generación USSD
 * - TelecomManager: Ejecución de llamadas con SIM específico seleccionado
 * - Telephony.Sms: Acceso privilegiado a SMS del sistema Android
 * - ContentResolver: Observación de cambios en base de datos SMS
 * - USADO POR: MainActivity para todos los flujos de transacciones USSD
 * 
 * OPTIMIZACIONES Y SEGURIDAD:
 * - Validación de permisos antes de operaciones críticas
 * - Limpieza automática de recursos al destruir componente
 * - Regex optimizados para extracción rápida de referencias
 * - Manejo thread-safe de callbacks y estado interno
 */
class USSDIntegrationHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "USSDIntegrationHelper"
        private const val SMS_SEARCH_TIMEOUT = 300000L // 5 minutes
    }
    
    // Use new architecture components
    private val serviceRepository = ServiceRepository.getInstance()
    
    private var smsObserver: ContentObserver? = null
    private var searchJob: Job? = null
    private var isSearching = false
    private var lastSmsTimestamp: Long = 0L
    private var currentSearchType = ""
    
    interface USSDCallback {
        fun onReferenceFound(referenceData: ReferenceData, smsBody: String)
        fun onSearchTimeout()
        fun onError(error: String)
    }
    
    private var currentCallback: USSDCallback? = null
    
    /**
     * FASE 8: Ejecuta USSD usando configuración dinámica
     */
    fun executeUSSDForService(
        serviceId: Int, 
        params: Map<String, String>, 
        callback: USSDCallback
    ) {
        AppLogger.i(TAG, "Ejecutando USSD para servicio ID: $serviceId")
        
        // Generar código USSD usando USSDConfiguration
        val ussdCode = USSDConfiguration.generateUSSDCode(serviceId, params)
        if (ussdCode == null) {
            callback.onError("Código USSD no encontrado para servicio $serviceId")
            return
        }
        
        // Obtener SIM requerida
        val requiredSIM = USSDConfiguration.getRequiredSIM(serviceId)
        val simIndex = requiredSIM?.index ?: 0
        
        // Obtener configuración del servicio para tipo de búsqueda SMS
        val serviceConfig = serviceRepository.getServiceConfig(serviceId)
        val searchType = serviceConfig?.smsSearchType ?: "Servicio"
        
        executeUSSD(ussdCode, searchType, callback, simIndex)
    }
    
    fun executeUSSD(ussdCode: String, searchType: String, callback: USSDCallback, simSlot: Int = 0) {
        AppLogger.i(TAG, "Ejecutando USSD: $ussdCode para $searchType en SIM $simSlot")
        currentCallback = callback
        currentSearchType = searchType
        
        if (!hasCallPermission()) {
            callback.onError("Permiso de llamada no otorgado")
            return
        }
        
        try {
            makeCall(ussdCode, simSlot)
            startSMSSearch(searchType)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error ejecutando USSD", e)
            callback.onError("Error ejecutando USSD: ${e.message}")
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun makeCall(ussdCode: String, simSlot: Int = 0) {
        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            val phoneAccountHandles = telecomManager.callCapablePhoneAccounts
            
            if (phoneAccountHandles.isNotEmpty()) {
                val selectedSim = if (simSlot < phoneAccountHandles.size) {
                    phoneAccountHandles[simSlot]
                } else {
                    phoneAccountHandles[0] // Default to first SIM
                }
                
                val callIntent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:${Uri.encode(ussdCode)}")
                    putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, selectedSim)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                
                context.startActivity(callIntent)
                AppLogger.i(TAG, "Llamada USSD iniciada: $ussdCode")
                
            } else {
                currentCallback?.onError("No se encontraron SIMs disponibles")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error realizando llamada USSD", e)
            currentCallback?.onError("Error realizando llamada: ${e.message}")
        }
    }
    
    private fun startSMSSearch(searchType: String) {
        if (isSearching) {
            cancelSMSSearch()
        }
        
        if (!hasSMSPermission()) {
            currentCallback?.onError("Permiso de lectura SMS no otorgado")
            return
        }
        
        isSearching = true
        lastSmsTimestamp = System.currentTimeMillis()
        AppLogger.i(TAG, "Iniciando búsqueda SMS para: $searchType")
        
        // Setup SMS observer
        smsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                searchInMessages()
            }
        }
        
        context.contentResolver.registerContentObserver(
            Telephony.Sms.CONTENT_URI,
            true,
            smsObserver!!
        )
        
        // Initial search
        searchInMessages()
        
        // Auto-cancel after timeout
        Handler(Looper.getMainLooper()).postDelayed({
            if (isSearching) {
                cancelSMSSearch()
                currentCallback?.onSearchTimeout()
            }
        }, SMS_SEARCH_TIMEOUT)
    }
    
    private fun searchInMessages() {
        if (!isSearching) return
        
        searchJob?.cancel()
        searchJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val smsUri: Uri = Telephony.Sms.Inbox.CONTENT_URI
                val selection = "${Telephony.Sms.DATE} > ? AND ${Telephony.Sms.ADDRESS} IN (?, ?, ?, ?)"
                val selectionArgs = arrayOf(
                    lastSmsTimestamp.toString(),
                    "555", "55", "200", "222"
                )
                
                val cursor: Cursor? = context.contentResolver.query(
                    smsUri,
                    arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.ADDRESS),
                    selection,
                    selectionArgs,
                    "${Telephony.Sms.DATE} DESC"
                )
                
                cursor?.use {
                    while (it.moveToNext()) {
                        val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
                        val date = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                        val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                        
                        AppLogger.d(TAG, "SMS encontrado: $body de $address en $date")
                        
                        val referenceData = extractReferenceData(body, currentSearchType)
                        if (referenceData != null) {
                            withContext(Dispatchers.Main) {
                                AppLogger.i(TAG, "Referencias encontradas: ${referenceData.ref1}")
                                currentCallback?.onReferenceFound(referenceData, body)
                                cancelSMSSearch()
                            }
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error buscando SMS", e)
                withContext(Dispatchers.Main) {
                    currentCallback?.onError("Error buscando SMS: ${e.message}")
                }
            }
        }
    }
    
    private fun extractReferenceData(smsBody: String, searchType: String): ReferenceData? {
        return try {
            when (searchType) {
                "Giros", "Retiros", "Billetera" -> {
                    val ref1Regex = Regex("""Ref 1: (\d+)""")
                    val ref2Regex = Regex("""Ref 2: (\d+)""")
                    
                    val ref1Match = ref1Regex.find(smsBody)
                    val ref2Match = ref2Regex.find(smsBody)
                    
                    if (ref1Match != null && ref2Match != null) {
                        ReferenceData(ref1Match.groupValues[1], ref2Match.groupValues[1])
                    } else {
                        // Try alternative patterns for Tigo
                        val montoYRefRegex = Regex("""Monto PYG ([\d.,]+)[^R]*Ref[ .:]+(\d+)""")
                        val montoMatch = montoYRefRegex.find(smsBody)
                        if (montoMatch != null) {
                            ReferenceData("", montoMatch.groupValues[2])
                        } else {
                            // Try another pattern
                            val refPattern = Regex("""referencia[:\s]+(\d+)""", RegexOption.IGNORE_CASE)
                            val refMatch = refPattern.find(smsBody)
                            refMatch?.let { ReferenceData(it.groupValues[1], "") }
                        }
                    }
                }
                "Retiros Personal", "Telefonia Personal" -> {
                    val comprobanteRegex = Regex("""Su comprobante es (\d+)""")
                    val match = comprobanteRegex.find(smsBody)
                    match?.let { ReferenceData(it.groupValues[1], "") }
                }
                "ANDE", "ESSAP", "COPACO" -> {
                    val codigoRegex = Regex("""Codigo de referencia: (\d+)""")
                    val match = codigoRegex.find(smsBody)
                    match?.let { ReferenceData(it.groupValues[1], "") }
                }
                "Reseteo" -> {
                    val reseteoRegex = Regex("""PIN reseteado[.\s]*Ref[:\s]*(\d+)""", RegexOption.IGNORE_CASE)
                    val match = reseteoRegex.find(smsBody)
                    match?.let { ReferenceData(it.groupValues[1], "") }
                }
                "Servicio", "Telefonia" -> {
                    val servicioRegex = Regex("""referencia[:\s]+(\d+)""", RegexOption.IGNORE_CASE)
                    val match = servicioRegex.find(smsBody)
                    match?.let { ReferenceData(it.groupValues[1], "") }
                }
                else -> null
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error extrayendo datos de referencia", e)
            null
        }
    }
    
    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Genio Tigo", text)
        clipboard.setPrimaryClip(clip)
        AppLogger.d(TAG, "Texto copiado al portapapeles: $text")
    }
    
    fun cancelSMSSearch() {
        isSearching = false
        smsObserver?.let {
            context.contentResolver.unregisterContentObserver(it)
            smsObserver = null
        }
        searchJob?.cancel()
        searchJob = null
        AppLogger.i(TAG, "Búsqueda SMS cancelada")
    }
    
    private fun hasCallPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun hasSMSPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun onDestroy() {
        cancelSMSSearch()
        currentCallback = null
    }
    
    /**
     * Generate USSD code using centralized service configuration
     * This replaces all the individual generateUSSDForXXX methods
     */
    fun generateUSSDCode(serviceId: Int, params: Map<String, String>): String? {
        return serviceRepository.generateUSSDCode(serviceId, params)
    }
    
    /**
     * Generate USSD code by service name (for backward compatibility)
     */
    fun generateUSSDByServiceName(serviceName: String, params: Map<String, String>): String? {
        val serviceId = serviceRepository.findServiceIdByName(serviceName)
        return if (serviceId >= 0) {
            generateUSSDCode(serviceId, params)
        } else {
            AppLogger.w(TAG, "Service not found: $serviceName")
            null
        }
    }
    
    // Legacy methods - DEPRECATED but kept for backward compatibility
    @Deprecated("Use generateUSSDCode with serviceId instead", ReplaceWith("generateUSSDCode(0, mapOf(\"phone\" to phone, \"cedula\" to cedula, \"amount\" to amount))"))
    fun generateUSSDForTigoGiros(phone: String, cedula: String, amount: String): String {
        return generateUSSDCode(0, mapOf("phone" to phone, "cedula" to cedula, "amount" to amount)) ?: "*555*1*$phone*$cedula*1*$amount#"
    }
    
    @Deprecated("Use generateUSSDCode with serviceId instead", ReplaceWith("generateUSSDCode(1, mapOf(\"phone\" to phone, \"cedula\" to cedula, \"amount\" to amount))"))
    fun generateUSSDForTigoRetiros(phone: String, cedula: String, amount: String): String {
        return generateUSSDCode(1, mapOf("phone" to phone, "cedula" to cedula, "amount" to amount)) ?: "*555*2*$phone*$cedula*1*$amount#"
    }
    
    @Deprecated("Use generateUSSDCode with serviceId instead", ReplaceWith("generateUSSDCode(2, mapOf(\"phone\" to phone, \"cedula\" to cedula, \"amount\" to amount))"))
    fun generateUSSDForTigoBilletera(phone: String, cedula: String, amount: String): String {
        return generateUSSDCode(2, mapOf("phone" to phone, "cedula" to cedula, "amount" to amount)) ?: "*555*3*1*$cedula*1*$phone*$amount#"
    }
    
    @Deprecated("Use generateUSSDCode with serviceId instead", ReplaceWith("generateUSSDCode(3, mapOf(\"phone\" to phone))"))
    fun generateUSSDForTigoTelefonia(phone: String): String {
        return generateUSSDCode(3, mapOf("phone" to phone)) ?: "*555*5*1*1*1*$phone*$phone#"
    }
    
    @Deprecated("Use generateUSSDCode with serviceId instead", ReplaceWith("generateUSSDCode(75, mapOf(\"phone\" to phone, \"cedula\" to cedula, \"nacimiento\" to birthDate))"))
    fun generateUSSDForReseteoCliente(phone: String, cedula: String, birthDate: String): String {
        return generateUSSDCode(75, mapOf("phone" to phone, "cedula" to cedula, "nacimiento" to birthDate)) ?: "*555*6*3*$phone*1*$cedula*$birthDate#"
    }
    
    @Deprecated("Use generateUSSDCode with serviceId instead", ReplaceWith("generateUSSDCode(7, mapOf(\"cedula\" to nis))"))
    fun generateUSSDForANDE(nis: String): String {
        return generateUSSDCode(7, mapOf("cedula" to nis)) ?: "*222*1*2*$nis#"
    }
    
    @Deprecated("Use generateUSSDCode with serviceId instead", ReplaceWith("generateUSSDCode(8, mapOf(\"cedula\" to issan))"))
    fun generateUSSDForESSAP(issan: String): String {
        return generateUSSDCode(8, mapOf("cedula" to issan)) ?: "*222*2*1*$issan#"
    }
    
    @Deprecated("Use generateUSSDCode with serviceId instead", ReplaceWith("generateUSSDCode(9, mapOf(\"phone\" to account))"))
    fun generateUSSDForCOPACO(account: String): String {
        return generateUSSDCode(9, mapOf("phone" to account)) ?: "*222*3*1*$account#"
    }
    
    @Deprecated("Use generateUSSDCode with serviceId instead", ReplaceWith("generateUSSDCode(11, mapOf(\"phone\" to phone, \"amount\" to amount))"))
    fun generateUSSDForPersonalRetiros(phone: String, amount: String): String {
        return generateUSSDCode(11, mapOf("phone" to phone, "amount" to amount)) ?: "*200*2*$phone*$amount#"
    }
    
    @Deprecated("Use generateUSSDCode with serviceId instead", ReplaceWith("generateUSSDCode(12, mapOf(\"phone\" to phone, \"amount\" to amount))"))
    fun generateUSSDForPersonalTelefonia(phone: String, amount: String): String {
        return generateUSSDCode(12, mapOf("phone" to phone, "amount" to amount)) ?: "*200*4*$phone*$amount#"
    }

    /**
     * Check if service has USSD support
     */
    fun hasUSSDSupport(serviceId: Int): Boolean {
        return serviceRepository.hasUSSDSupport(serviceId)
    }
    
    /**
     * Check if service has USSD support by name
     */
    fun hasUSSDSupportByName(serviceName: String): Boolean {
        val serviceId = serviceRepository.findServiceIdByName(serviceName)
        return if (serviceId >= 0) hasUSSDSupport(serviceId) else false
    }
    
    /**
     * Get service configuration for USSD operations
     */
    fun getServiceConfig(serviceId: Int) = serviceRepository.getServiceConfig(serviceId)
}