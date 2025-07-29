package com.example.geniotecni.tigo.models

/**
 * Service configuration model that defines which fields should be shown
 * and their corresponding hints/labels for each service
 * 
 * FASE 8: Actualizado con soporte para códigos USSD y plantillas de impresión
 */
data class ServiceConfig(
    // Field visibility flags
    val showPhone: Boolean = true,
    val showCedula: Boolean = true,
    val showAmount: Boolean = true,
    val showConsultButton: Boolean = false,
    val showNacimiento: Boolean = false,

    val showConsulta: Boolean = false,
    val consultaHint: String = "Ingrese el numero de consulta",
    val minAmount: Long = 0L,
    val maxAmount: Long = Long.MAX_VALUE,

    val phoneHint: String = "Número de teléfono",
    val cedulaHint: String = "Número de cédula",
    val amountHint: String = "Monto",
    val nacimientoHint: String = "Fecha de nacimiento",

    // Default values
    val cedulaDefaultValue: String = "",
    val phoneDefaultValue: String = "",

    // Input types
    val cedulaInputType: InputType = InputType.NUMBER,
    val phoneInputType: InputType = InputType.PHONE,
    val amountInputType: InputType = InputType.NUMBER,
    val nacimientoInputType: InputType = InputType.NUMBER,

    // Additional configuration
    val requiresManualUSSD: Boolean = false,
    val useAlternativeSIM: Boolean = false, // true = SIM 2, false = SIM 1
    val smsSearchType: String = "Servicio",
    val maxFieldLength: Map<String, Int> = emptyMap(),
    
    // FASE 8: Nuevos campos para USSD y plantillas de impresión
    val ussdTemplate: String = "",              // Plantilla USSD dinámica
    val printTemplate: String = "",             // Plantilla de impresión específica
    val fieldLabels: Map<String, String> = emptyMap(), // Etiquetas específicas para impresión
    val hasCommission: Boolean = false,         // Si aplica comisión
    val commissionRate: Double = 0.0,          // Tasa de comisión (ej: 0.06 = 6%)
    val specialFieldMappings: Map<String, String> = emptyMap() // Mapeo de campos especiales (ej: "cedula" -> "issan")
) {
    /**
     * Input type enumeration for form fields
     */
    enum class InputType {
        TEXT,
        NUMBER,
        PHONE,
        DATE
    }

    companion object {
        /**
         * Creates a default configuration for Tigo services
         */
        fun createTigoDefault() = ServiceConfig(
            showPhone = true,
            showCedula = true,
            showAmount = true,
            phoneHint = "Número de teléfono",
            cedulaHint = "Número de cédula",
            amountHint = "Monto"
        )

        /**
         * Creates a default configuration for Personal services
         */
        fun createPersonalDefault() = ServiceConfig(
            showPhone = true,
            showCedula = false,
            showAmount = true,
            phoneHint = "Número de teléfono Personal",
            amountHint = "Monto",
            useAlternativeSIM = true
        )

        /**
         * Creates a default configuration for government services
         */
        fun createGovernmentDefault() = ServiceConfig(
            showPhone = false,
            showCedula = true,
            showAmount = false,
            showConsultButton = true,
            cedulaHint = "Número de cuenta"
        )

        /**
         * Creates a default configuration for cooperative services
         */
        fun createCooperativeDefault() = ServiceConfig(
            showPhone = false,
            showCedula = true,
            showAmount = false,
            showConsultButton = true,
            cedulaHint = "Número de CI"
        )

        /**
         * Creates a default configuration for bank/financial services
         */
        fun createBankDefault() = ServiceConfig(
            showPhone = false,
            showCedula = true,
            showAmount = false,
            showConsultButton = true,
            cedulaHint = "Número de CI"
        )
    }

    /**
     * Validates if all required fields have values
     */
    fun validateFields(phone: String?, cedula: String?, amount: String?, nacimiento: String?): Boolean {
        return (!showPhone || !phone.isNullOrBlank()) &&
                (!showCedula || !cedula.isNullOrBlank()) &&
                (!showAmount || !amount.isNullOrBlank()) &&
                (!showNacimiento || !nacimiento.isNullOrBlank())
    }

    /**
     * Gets the appropriate hint for a field
     */
    fun getHintForField(fieldName: String): String {
        return when (fieldName) {
            "phone" -> phoneHint
            "cedula" -> cedulaHint
            "amount" -> amountHint
            "nacimiento" -> nacimientoHint
            else -> ""
        }
    }
    
    /**
     * FASE 8: Genera código USSD usando la plantilla configurada
     */
    fun generateUSSDCode(params: Map<String, String>): String? {
        if (ussdTemplate.isEmpty()) return null
        
        var code = ussdTemplate
        params.forEach { (key, value) ->
            code = code.replace("{$key}", value)
        }
        return code
    }
    
    /**
     * FASE 8: Obtiene la etiqueta de impresión para un campo específico
     */
    fun getPrintLabel(fieldName: String): String {
        return fieldLabels[fieldName] ?: getHintForField(fieldName)
    }
    
    /**
     * FASE 8: Calcula la comisión si aplica
     */
    fun calculateCommission(amount: Long): Long {
        return if (hasCommission) {
            (amount * commissionRate).toLong()
        } else {
            0L
        }
    }
}