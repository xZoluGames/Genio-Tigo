package com.example.geniotecni.tigo.utils

import com.example.geniotecni.tigo.models.ServiceConfig
import com.example.geniotecni.tigo.models.ReferenceData
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
        
        // Aplicar reemplazos directos - SOLUCION SIMPLIFICADA
        var result = template
            .replace("{servicio}", serviceName)
            .replace("{fecha}", date)
            .replace("{hora}", time)
        
        // Reemplazar todos los campos
        fields.forEach { (key, value) ->
            result = result.replace("{$key}", value)
        }
        
        // Manejar monto específicamente
        when (printTemplate.specialMounting) {
            PrintConfiguration.SpecialMounting.AMOUNT_IN_REF1 -> {
                // Para servicios públicos (ANDE, ESSAP, etc.) - monto viene de ref1
                if (references.ref1.isNotEmpty()) {
                    result = result.replace("{monto}", formatAmount(references.ref1))
                }
            }
            else -> {
                // Para servicios normales (Giros, Personal, etc.) - usar monto directo
                if (amount != null && amount.isNotEmpty()) {
                    result = result.replace("{monto}", formatAmount(amount))
                }
            }
        }
        
        // Manejar comisión
        if (printTemplate.hasCommission && amount != null && amount.isNotEmpty()) {
            try {
                val cleanAmount = amount.replace(",", "").replace(".", "")
                val amountValue = cleanAmount.toLongOrNull() ?: 0L
                val commission = (amountValue * printTemplate.commissionRate).toLong()
                result = result.replace("{comision}", decimalFormat.format(commission))
            } catch (e: Exception) {
                result = result.replace("{comision}", "0")
            }
        }
        
        // Manejar referencias
        result = result.replace("{ref1}", references.ref1)
        result = result.replace("{ref2}", references.ref2)
        result = result.replace("{ref}", references.ref1) // ref genérico
        
        // Limpiar placeholders restantes
        result = cleanUnusedPlaceholders(result)
        
        return result
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
                    result = result.replace("{monto}", "$formattedAmount")
                } else {
                    // Si ref1 está vacío, poner 0
                    result = result.replace("{monto}", "0")
                }
            }
            else -> { // NORMAL o null
                // Para servicios normales, usar el monto proporcionado
                if (amount != null && amount.isNotEmpty()) {
                    val formattedAmount = formatAmount(amount)
                    result = result.replace("{monto}", "$formattedAmount")
                } else {
                    // Si no hay monto, poner 0
                    result = result.replace("{monto}", "0")
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
        val currentDateTime = LocalDateTime.now()
        val date = currentDateTime.format(dateFormatter)
        val time = currentDateTime.format(timeFormatter)
        
        // Usar plantilla simple para generateSimpleMessage
        var template = """=====================
Genio Tecni
{servicio}
Fecha: {fecha}
Hora: {hora}
{campos_dinamicos}
Monto: {monto} Gs.
Ref1: {ref1}
{ref2_opcional}
====================="""
        
        // Reemplazar valores básicos
        template = template
            .replace("{servicio}", serviceName)
            .replace("{fecha}", date)
            .replace("{hora}", time)
        
        // Construir campos dinámicos
        val camposDinamicos = fields.map { (key, value) ->
            "${key.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}: $value"
        }.joinToString("\n")
        
        template = template.replace("{campos_dinamicos}", camposDinamicos)
        
        // Manejar monto
        amount?.let {
            val formattedAmount = formatAmount(it)
            template = template.replace("{monto}", formattedAmount)
        } ?: run {
            template = template.replace("Monto: {monto} Gs.\n", "")
        }
        
        // Manejar referencias
        template = template.replace("{ref1}", ref1)
        if (ref2.isNotEmpty()) {
            template = template.replace("{ref2_opcional}", "Ref2: $ref2")
        } else {
            template = template.replace("{ref2_opcional}", "")
        }
        
        // Limpiar placeholders no utilizados
        template = cleanUnusedPlaceholders(template)
        
        return template
    }
}