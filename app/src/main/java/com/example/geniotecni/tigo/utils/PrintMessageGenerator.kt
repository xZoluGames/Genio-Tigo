package com.example.geniotecni.tigo.utils

import com.example.geniotecni.tigo.models.ServiceConfig
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Generador de mensajes de impresión consistentes
 * Fase 8: Estandarización de códigos USSD y formatos de impresión
 */
object PrintMessageGenerator {
    
    private val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val decimalFormat = DecimalFormat("#,###")
    
    /**
     * Genera mensaje de impresión usando configuración del servicio
     */
    fun generateMessage(
        serviceId: Int,
        serviceName: String,
        fields: Map<String, String>,
        amount: String?,
        references: ReferenceData,
        serviceConfig: ServiceConfig? = null
    ): String {
        val currentDateTime = LocalDateTime.now()
        val date = currentDateTime.format(dateFormatter)
        val time = currentDateTime.format(timeFormatter)
        
        // Obtener plantilla desde PrintConfiguration
        val printTemplate = PrintConfiguration.getPrintTemplate(serviceId)
        
        // Si el servicio tiene configuración personalizada, usar su plantilla
        val template = if (serviceConfig?.printTemplate?.isNotEmpty() == true) {
            serviceConfig.printTemplate
        } else {
            printTemplate.template
        }
        
        return fillTemplate(
            template = template,
            serviceId = serviceId,
            serviceName = serviceName,
            fields = fields,
            amount = amount,
            references = references,
            date = date,
            time = time,
            printTemplate = printTemplate,
            serviceConfig = serviceConfig
        )
    }
    
    /**
     * Rellena la plantilla con los valores correspondientes
     */
    private fun fillTemplate(
        template: String,
        serviceId: Int,
        serviceName: String,
        fields: Map<String, String>,
        amount: String?,
        references: ReferenceData,
        date: String,
        time: String,
        printTemplate: PrintConfiguration.PrintTemplate,
        serviceConfig: ServiceConfig?
    ): String {
        var result = template
            .replace("{servicio}", serviceName)
            .replace("{fecha}", date)
            .replace("{hora}", time)
        
        // Reemplazar campos específicos del servicio
        fields.forEach { (key, value) ->
            result = result.replace("{$key}", value)
        }
        
        // Manejar monto según el tipo de servicio
        result = handleAmountReplacement(
            result, amount, references, printTemplate, serviceConfig
        )
        
        // Manejar comisión si aplica
        result = handleCommissionReplacement(
            result, amount, printTemplate, serviceConfig
        )
        
        // Manejar referencias
        result = handleReferencesReplacement(
            result, references, serviceId, serviceName
        )
        
        // Limpiar placeholders no utilizados
        result = cleanUnusedPlaceholders(result)
        
        return result
    }
    
    /**
     * Maneja el reemplazo del monto según el tipo de servicio
     */
    private fun handleAmountReplacement(
        template: String,
        amount: String?,
        references: ReferenceData,
        printTemplate: PrintConfiguration.PrintTemplate,
        serviceConfig: ServiceConfig?
    ): String {
        var result = template
        
        when (printTemplate.specialMounting) {
            PrintConfiguration.SpecialMounting.AMOUNT_IN_REF1 -> {
                // Para servicios públicos, el monto viene en ref1
                if (references.ref1.isNotEmpty()) {
                    val formattedAmount = formatAmount(references.ref1)
                    result = result.replace("{monto}", formattedAmount)
                }
            }
            PrintConfiguration.SpecialMounting.NORMAL, null -> {
                // Para servicios normales, usar el monto proporcionado
                amount?.let {
                    val formattedAmount = formatAmount(it)
                    result = result.replace("{monto}", formattedAmount)
                }
            }
        }
        
        return result
    }
    
    /**
     * Maneja el reemplazo de la comisión
     */
    private fun handleCommissionReplacement(
        template: String,
        amount: String?,
        printTemplate: PrintConfiguration.PrintTemplate,
        serviceConfig: ServiceConfig?
    ): String {
        var result = template
        
        val hasCommission = printTemplate.hasCommission || (serviceConfig?.hasCommission == true)
        val commissionRate = if (serviceConfig?.hasCommission == true) {
            serviceConfig.commissionRate
        } else {
            printTemplate.commissionRate
        }
        
        if (hasCommission && amount != null) {
            try {
                val cleanAmount = amount.replace(",", "").replace(".", "")
                val amountValue = cleanAmount.toLongOrNull() ?: 0L
                val commission = (amountValue * commissionRate).toLong()
                val formattedCommission = decimalFormat.format(commission)
                
                result = result.replace("{comision}", formattedCommission)
            } catch (e: Exception) {
                result = result.replace("{comision}", "0")
            }
        }
        
        return result
    }
    
    /**
     * Maneja el reemplazo de las referencias
     */
    private fun handleReferencesReplacement(
        template: String,
        references: ReferenceData,
        serviceId: Int,
        serviceName: String
    ): String {
        var result = template
        
        // Reemplazo estándar de referencias
        result = result.replace("{ref1}", references.ref1)
        result = result.replace("{ref2}", references.ref2)
        result = result.replace("{ref}", references.ref1) // ref genérico usa ref1
        
        return result
    }
    
    /**
     * Formatea un monto con separadores de miles
     */
    private fun formatAmount(amount: String): String {
        return try {
            val cleanAmount = amount.replace(",", "").replace(".", "")
            val amountValue = cleanAmount.toLongOrNull() ?: 0L
            decimalFormat.format(amountValue)
        } catch (e: Exception) {
            amount
        }
    }
    
    /**
     * Limpia placeholders no utilizados de la plantilla
     */
    private fun cleanUnusedPlaceholders(template: String): String {
        return template.replace(Regex("\\{[^}]+\\}"), "")
    }
    
    /**
     * Data class para referencias
     */
    data class ReferenceData(
        val ref1: String,
        val ref2: String = ""
    )
    
    /**
     * Genera mensaje usando solo la configuración básica (para compatibilidad)
     */
    fun generateSimpleMessage(
        serviceName: String,
        fields: Map<String, String>,
        amount: String?,
        ref1: String,
        ref2: String = ""
    ): String {
        val references = ReferenceData(ref1, ref2)
        return generateMessage(
            serviceId = -1,
            serviceName = serviceName,
            fields = fields,
            amount = amount,
            references = references
        )
    }
}