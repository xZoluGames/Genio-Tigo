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
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import com.example.geniotecni.tigo.models.ReferenceData
import com.example.geniotecni.tigo.utils.AppLogger
import kotlinx.coroutines.*

class USSDIntegrationHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "USSDIntegrationHelper"
        private const val SMS_SEARCH_TIMEOUT = 300000L // 5 minutes
    }
    
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
    
    fun executeUSSD(ussdCode: String, searchType: String, callback: USSDCallback) {
        AppLogger.i(TAG, "Ejecutando USSD: $ussdCode para $searchType")
        currentCallback = callback
        currentSearchType = searchType
        
        if (!hasCallPermission()) {
            callback.onError("Permiso de llamada no otorgado")
            return
        }
        
        try {
            makeCall(ussdCode)
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
    
    // USSD code generators for different services
    fun generateUSSDForTigoGiros(phone: String, cedula: String, amount: String): String {
        return "*555*1*$phone*$cedula*1*$amount#"
    }
    
    fun generateUSSDForTigoRetiros(phone: String, cedula: String, amount: String): String {
        return "*555*2*$phone*$cedula*1*$amount#"
    }
    
    fun generateUSSDForTigoBilletera(phone: String, cedula: String, amount: String): String {
        return "*555*3*1*$cedula*1*$phone*$amount#"
    }
    
    fun generateUSSDForTigoTelefonia(phone: String): String {
        return "*555*5*1*1*1*$phone*$phone#"
    }
    
    fun generateUSSDForReseteoCliente(phone: String, cedula: String, birthDate: String): String {
        return "*555*6*3*$phone*1*$cedula*$birthDate#"
    }
    
    fun generateUSSDForANDE(nis: String): String {
        return "*222*1*2*$nis#"
    }
    
    fun generateUSSDForESSAP(issan: String): String {
        return "*222*2*1*$issan#"
    }
    
    fun generateUSSDForCOPACO(account: String): String {
        return "*222*3*1*$account#"
    }
    
    fun generateUSSDForPersonalRetiros(phone: String, amount: String): String {
        return "*200*2*$phone*$amount#"
    }
    
    fun generateUSSDForPersonalTelefonia(phone: String, amount: String): String {
        return "*200*4*$phone*$amount#"
    }
}