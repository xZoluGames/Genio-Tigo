package com.example.geniotecni.tigo.models

import com.example.geniotecni.tigo.data.repository.ServiceRepository
import com.example.geniotecni.tigo.utils.PrintMessageGenerator
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Modelo de datos para impresión 
 * FASE 8: Actualizado para usar plantillas de configuración centralizadas
 */
data class PrintData(
    val serviceId: Int,
    val serviceName: String,
    val date: String,
    val time: String,
    val fields: Map<String, String>,     // Campos dinámicos del servicio
    val amount: String?,
    val commission: String?,
    val referenceData: ReferenceData,
    val rawMessage: String,              // Mensaje formateado final para impresión e historial
    val transactionData: TransactionData = TransactionData()
) {
    
    // Constructor de compatibilidad con el sistema anterior
    constructor(
        service: String,
        date: String,
        time: String,
        message: String,
        referenceData: ReferenceData,
        transactionData: TransactionData = TransactionData()
    ) : this(
        serviceId = -1,
        serviceName = service,
        date = date,
        time = time,
        fields = emptyMap(),
        amount = null,
        commission = null,
        referenceData = referenceData,
        rawMessage = message,
        transactionData = transactionData
    )
    
    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        
        /**
         * FASE 8: Crea PrintData usando configuración dinámica del servicio
         */
        fun fromTransaction(
            serviceId: Int,
            serviceName: String,
            fields: Map<String, String>,
            amount: String?,
            referenceData: ReferenceData,
            serviceConfig: ServiceConfig? = null
        ): PrintData {
            val currentDateTime = LocalDateTime.now()
            val date = currentDateTime.format(dateFormatter)
            val time = currentDateTime.format(timeFormatter)
            
            // Generar mensaje usando PrintMessageGenerator
            val message = PrintMessageGenerator.generateMessage(
                serviceId = serviceId,
                serviceName = serviceName,
                fields = fields,
                amount = amount,
                references = PrintMessageGenerator.ReferenceData(
                    referenceData.ref1,
                    referenceData.ref2
                ),
                serviceConfig = serviceConfig
            )
            
            // Calcular comisión si aplica
            val commission = if (serviceConfig?.hasCommission == true && amount != null) {
                try {
                    val cleanAmount = amount.replace(",", "").replace(".", "").toLongOrNull() ?: 0L
                    val commissionAmount = serviceConfig.calculateCommission(cleanAmount)
                    commissionAmount.toString()
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
            
            return PrintData(
                serviceId = serviceId,
                serviceName = serviceName,
                date = date,
                time = time,
                fields = fields,
                amount = amount,
                commission = commission,
                referenceData = referenceData,
                rawMessage = message
            )
        }
        
        /**
         * FASE 8: Crea PrintData desde ID de servicio y parámetros
         */
        fun fromServiceTransaction(
            serviceId: Int,
            params: Map<String, String>,
            referenceData: ReferenceData
        ): PrintData {
            val serviceRepository = ServiceRepository.getInstance()
            val serviceConfig = serviceRepository.getServiceConfig(serviceId)
            val serviceName = serviceRepository.getServiceName(serviceId)
            
            return fromTransaction(
                serviceId = serviceId,
                serviceName = serviceName,
                fields = params,
                amount = params["monto"] ?: params["amount"],
                referenceData = referenceData,
                serviceConfig = serviceConfig
            )
        }
        
        /**
         * Método simple para compatibilidad hacia atrás
         */
        fun createSimple(
            serviceName: String,
            fields: Map<String, String>,
            amount: String?,
            ref1: String,
            ref2: String = ""
        ): PrintData {
            val referenceData = ReferenceData(ref1, ref2)
            val message = PrintMessageGenerator.generateSimpleMessage(
                serviceName = serviceName,
                fields = fields,
                amount = amount,
                ref1 = ref1,
                ref2 = ref2
            )
            
            val currentDateTime = LocalDateTime.now()
            val date = currentDateTime.format(dateFormatter)
            val time = currentDateTime.format(timeFormatter)
            
            return PrintData(
                serviceId = -1,
                serviceName = serviceName,
                date = date,
                time = time,
                fields = fields,
                amount = amount,
                commission = null,
                referenceData = referenceData,
                rawMessage = message
            )
        }
    }
    
    /**
     * FASE 8: Obtiene el mensaje para mostrar en el historial
     * Garantiza que PrintHistory muestre exactamente lo mismo que se imprimió
     */
    fun getDisplayMessage(): String = rawMessage
    
    /**
     * Obtiene el mensaje legado para compatibilidad
     */
    val message: String get() = rawMessage
}





