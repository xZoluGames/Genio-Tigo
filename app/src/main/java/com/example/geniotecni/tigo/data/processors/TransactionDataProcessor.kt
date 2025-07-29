package com.example.geniotecni.tigo.data.processors

import com.example.geniotecni.tigo.models.PrintData
import com.example.geniotecni.tigo.utils.AppLogger
import java.text.DecimalFormat

/**
 * 🔄 PROCESADOR DE DATOS TRANSACCIONALES - Motor de Extracción y Formateo
 * 
 * RESPONSABILIDAD PRINCIPAL:
 * - Procesamiento complejo de datos de transacciones desde múltiples fuentes
 * - Extracción inteligente de información usando regex avanzados
 * - Formateo consistente de datos para presentación en UI
 * - Separación clara entre lógica de procesamiento y renderizado de interfaz
 * 
 * CAPACIDADES DE EXTRACCIÓN:
 * - Extracción de números telefónicos con múltiples patrones y formatos
 * - Detección automática de cédulas de identidad en varios formatos
 * - Procesamiento de montos con formateo automático de moneda
 * - Extracción de referencias y códigos de transacciones
 * - Datos especiales según el tipo de servicio (NIS, ISSAN, cuentas bancarias)
 * 
 * ARQUITECTURA SINGLETON:
 * - Patrón Singleton thread-safe para reutilización eficiente
 * - Métodos estáticos para operaciones de utilidad comunes
 * - Caché implícito de patrones regex para rendimiento
 * 
 * PROCESAMIENTO INTELIGENTE:
 * - Fallback automático entre datos estructurados y extracción regex
 * - Validación automática de formatos de teléfonos y cédulas
 * - Cálculo automático de comisiones y totales
 * - Formateo especializado según el contexto del servicio
 * 
 * CONEXIONES ARQUITECTÓNICAS:
 * - CONSUME: PrintData models para datos de entrada estructurados
 * - PRODUCE: ProcessedTransactionData para consumo directo de UI
 * - UTILIZA: AppLogger para debugging de procesos de extracción
 * - USADO POR: Adapters y Activities para presentación de datos
 */
class TransactionDataProcessor {
    
    companion object {
        private const val TAG = "TransactionDataProcessor"
        
        @Volatile
        private var INSTANCE: TransactionDataProcessor? = null
        
        fun getInstance(): TransactionDataProcessor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TransactionDataProcessor().also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Data class for processed transaction display data
     */
    data class ProcessedTransactionData(
        val phone: String?,
        val cedula: String?,
        val formattedAmount: String?,
        val reference1: String?,
        val reference2: String?,
        val legacyReference: String?,
        val specialData: Map<String, String>,
        val hasReferences: Boolean
    )
    
    /**
     * FASE 9: Process transaction data for display - usar rawMessage
     */
    fun processTransactionData(printData: PrintData): ProcessedTransactionData {
        val transactionData = printData.transactionData
        val service = printData.serviceName
        // FASE 9: Usar rawMessage para procesamiento consistente
        val message = printData.rawMessage
        
        AppLogger.d(TAG, "Processing transaction data for service: $service")
        
        // Get data from structured TransactionData first, fallback to regex extraction
        val phone = transactionData.phone.ifEmpty { extractPhone(message) }
        val cedula = transactionData.cedula.ifEmpty { extractCedula(message) }
        val amount = transactionData.amount.ifEmpty { null }
        
        val formattedAmount = if (!amount.isNullOrEmpty()) {
            formatAmountString(amount)
        } else {
            extractAndFormatAmount(message)
        }
        
        val ref1 = printData.referenceData.ref1.takeIf { it.isNotEmpty() && it != "N/A" }
        val ref2 = printData.referenceData.ref2.takeIf { it.isNotEmpty() && it != "N/A" }
        val legacyReference = extractLegacyReference(message)
        val specialData = extractSpecialData(message, service)
        val hasReferences = ref1 != null || ref2 != null
        
        return ProcessedTransactionData(
            phone = phone,
            cedula = cedula,
            formattedAmount = formattedAmount,
            reference1 = ref1,
            reference2 = ref2,
            legacyReference = legacyReference,
            specialData = specialData,
            hasReferences = hasReferences
        )
    }
    
    /**
     * Extract phone number from message using multiple patterns
     */
    fun extractPhone(message: String): String? {
        val patterns = listOf(
            Regex("""Tel[eé]fono:\s*([0-9\-\s]+)"""),
            Regex("""Tel:\s*([0-9\-\s]+)"""),
            Regex("""Teléfono:\s*([0-9\-\s]+)"""),
            Regex("""Número:\s*([0-9\-\s]+)"""),
            Regex("""([0-9]{3,4}[\-\s]?[0-9]{6,7})""") // General phone pattern
        )
        
        for (pattern in patterns) {
            val match = pattern.find(message)
            if (match != null) {
                AppLogger.d(TAG, "Phone extracted: ${match.groupValues[1].trim()}")
                return match.groupValues[1].trim()
            }
        }
        return null
    }
    
    /**
     * Extract cedula/CI from message using multiple patterns
     */
    fun extractCedula(message: String): String? {
        val patterns = listOf(
            Regex("""Cédula:\s*([0-9\.\-\s]+)"""),
            Regex("""C\.I\.:?\s*([0-9\.\-\s]+)"""),
            Regex("""CI:\s*([0-9\.\-\s]+)"""),
            Regex("""([0-9]{1,2}\.[0-9]{3}\.[0-9]{3})"""), // CI format x.xxx.xxx
            Regex("""([0-9]{7,8})""") // Simple number format
        )
        
        for (pattern in patterns) {
            val match = pattern.find(message)
            if (match != null) {
                AppLogger.d(TAG, "Cedula extracted: ${match.groupValues[1].trim()}")
                return match.groupValues[1].trim()
            }
        }
        return null
    }
    
    /**
     * Extract and format amount from message
     */
    fun extractAndFormatAmount(message: String): String? {
        val patterns = listOf(
            Regex("""Monto:\s*([0-9,\.]+)\s*Gs?\.?"""),
            Regex("""Importe:\s*([0-9,\.]+)\s*Gs?\.?"""),
            Regex("""Valor:\s*([0-9,\.]+)\s*Gs?\.?"""),
            Regex("""([0-9]{1,3}(?:[,\.]?[0-9]{3})*)\s*Gs?\.?""") // General amount pattern
        )
        
        for (pattern in patterns) {
            val match = pattern.find(message)
            if (match != null) {
                val amount = match.groupValues[1].replace(",", "").replace(".", "")
                val numericAmount = amount.toLongOrNull()
                if (numericAmount != null && numericAmount > 0) {
                    val formatted = formatAmount(numericAmount)
                    AppLogger.d(TAG, "Amount extracted and formatted: $formatted")
                    return formatted
                }
            }
        }
        return null
    }
    
    /**
     * Extract legacy reference code from message
     */
    fun extractLegacyReference(message: String): String? {
        val regex = Regex("""Ref:\s*(\w+)""")
        val match = regex.find(message)
        return match?.groupValues?.get(1)?.also {
            AppLogger.d(TAG, "Legacy reference extracted: $it")
        }
    }
    
    /**
     * Extract service-specific special data
     */
    fun extractSpecialData(message: String, service: String): Map<String, String> {
        val data = mutableMapOf<String, String>()
        
        when {
            service.contains("ANDE", ignoreCase = true) -> {
                // For ANDE, look for NIS or account number
                val nisPattern = Regex("""NIS:\s*([0-9]+)""")
                val nisMatch = nisPattern.find(message)
                if (nisMatch != null) {
                    data["NIS"] = nisMatch.groupValues[1]
                    AppLogger.d(TAG, "ANDE NIS extracted: ${nisMatch.groupValues[1]}")
                }
            }
            service.contains("ESSAP", ignoreCase = true) -> {
                // For ESSAP, look for account number
                val accountPattern = Regex("""Cuenta:\s*([0-9\-]+)""")
                val accountMatch = accountPattern.find(message)
                if (accountMatch != null) {
                    data["Cuenta"] = accountMatch.groupValues[1]
                    AppLogger.d(TAG, "ESSAP account extracted: ${accountMatch.groupValues[1]}")
                }
            }
            service.contains("Banco", ignoreCase = true) || service.contains("Financiera", ignoreCase = true) -> {
                // For banks/financieras, look for specific account info
                val accountPattern = Regex("""Número de cuenta:\s*([0-9\-]+)""")
                val namePattern = Regex("""Nombre:\s*([A-Za-z\s]+)""")
                
                val accountMatch = accountPattern.find(message)
                val nameMatch = namePattern.find(message)
                
                if (accountMatch != null) {
                    data["Número de cuenta"] = accountMatch.groupValues[1]
                    AppLogger.d(TAG, "Bank account extracted: ${accountMatch.groupValues[1]}")
                }
                if (nameMatch != null) {
                    data["Nombre"] = nameMatch.groupValues[1].trim()
                    AppLogger.d(TAG, "Account holder name extracted: ${nameMatch.groupValues[1].trim()}")
                }
            }
            service.contains("Cooperativa", ignoreCase = true) -> {
                // For cooperatives, look for member number
                val memberPattern = Regex("""Socio:\s*([0-9]+)""")
                val memberMatch = memberPattern.find(message)
                if (memberMatch != null) {
                    data["Número de Socio"] = memberMatch.groupValues[1]
                    AppLogger.d(TAG, "Cooperative member number extracted: ${memberMatch.groupValues[1]}")
                }
            }
        }
        
        return data
    }
    
    /**
     * Format amount string
     */
    fun formatAmountString(amount: String): String {
        return try {
            val numericAmount = amount.replace(",", "").replace(".", "").toLongOrNull()
            if (numericAmount != null && numericAmount > 0) {
                formatAmount(numericAmount)
            } else {
                "$amount Gs."
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error formatting amount string: $amount", e)
            "$amount Gs."
        }
    }
    
    /**
     * Format numeric amount
     */
    fun formatAmount(amount: Long): String {
        return "${DecimalFormat("#,###").format(amount)} Gs."
    }
    
    /**
     * Extract numeric amount from message for calculations
     */
    fun extractNumericAmount(message: String): Long {
        val regex = Regex("""Monto:\s*([0-9,]+)\s*Gs\.""")
        val match = regex.find(message)
        return match?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull() ?: 0L
    }
    
    /**
     * Calculate commission for an amount
     */
    fun calculateCommission(amount: Long, commissionRate: Double = 0.01): Long {
        return (amount * commissionRate).toLong()
    }
    
    /**
     * Validate phone number format
     */
    fun isValidPhoneNumber(phone: String?): Boolean {
        if (phone.isNullOrBlank()) return false
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")
        return cleanPhone.length >= 9 && cleanPhone.length <= 10
    }
    
    /**
     * Validate cedula format
     */
    fun isValidCedula(cedula: String?): Boolean {
        if (cedula.isNullOrBlank()) return false
        val cleanCedula = cedula.replace(Regex("[^0-9]"), "")
        return cleanCedula.length >= 6 && cleanCedula.length <= 8
    }
    
    /**
     * Clean and format phone number
     */
    fun formatPhoneNumber(phone: String?): String? {
        if (phone.isNullOrBlank()) return null
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")
        return when (cleanPhone.length) {
            9 -> "${cleanPhone.substring(0, 3)}-${cleanPhone.substring(3, 6)}-${cleanPhone.substring(6)}"
            10 -> "${cleanPhone.substring(0, 4)}-${cleanPhone.substring(4, 7)}-${cleanPhone.substring(7)}"
            else -> phone
        }
    }
    
    /**
     * Clean and format cedula
     */
    fun formatCedula(cedula: String?): String? {
        if (cedula.isNullOrBlank()) return null
        val cleanCedula = cedula.replace(Regex("[^0-9]"), "")
        return when (cleanCedula.length) {
            7 -> "${cleanCedula.substring(0, 1)}.${cleanCedula.substring(1, 4)}.${cleanCedula.substring(4)}"
            8 -> "${cleanCedula.substring(0, 2)}.${cleanCedula.substring(2, 5)}.${cleanCedula.substring(5)}"
            else -> cedula
        }
    }
}