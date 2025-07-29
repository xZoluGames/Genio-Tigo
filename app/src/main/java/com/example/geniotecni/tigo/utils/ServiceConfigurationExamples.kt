package com.example.geniotecni.tigo.utils

import com.example.geniotecni.tigo.models.ServiceConfig

/**
 * Ejemplos específicos de configuración para servicios críticos
 * Fase 8: Estandarización de códigos USSD y formatos de impresión
 */
object ServiceConfigurationExamples {
    
    /**
     * Configuración correcta para ESSAP
     * IMPORTANTE: Usa campo "issan" (NO cedula), tipo TEXT, valor inicial "ZV"
     */
    fun createESSAPConfiguration(): ServiceConfig {
        return ServiceConfig(
            // Campos visibles
            showPhone = false,
            showCedula = true,  // Este será el campo ISSAN
            showAmount = false,
            showConsultButton = true,
            showNacimiento = false,
            
            // Textos e indicaciones
            cedulaHint = "Ingrese el nro de issan",  // Texto específico para ESSAP
            cedulaDefaultValue = "ZV",               // Valor inicial
            cedulaInputType = ServiceConfig.InputType.TEXT,  // Tipo TEXT (no NUMBER)
            
            // Configuración SIM
            useAlternativeSIM = false,  // SIM 1 (Tigo)
            
            // FASE 8: Configuración USSD y Print
            ussdTemplate = "*555*5*1*2*2*{issan}*{issan}*#",
            printTemplate = """=====================
Genio Tecni
ESSAP
Fecha: {fecha}
Hora: {hora}
ISSAN: {issan}
Monto: {ref1} Gs.
Ref1: {ref2}
=====================""",
            fieldLabels = mapOf(
                "cedula" to "ISSAN",  // Mapea el campo cedula a etiqueta ISSAN
                "issan" to "ISSAN"
            ),
            specialFieldMappings = mapOf(
                "cedula" to "issan"   // Campo cedula se mapea internamente a issan
            ),
            hasCommission = false,
            smsSearchType = "ESSAP"
        )
    }
    
    /**
     * Configuración correcta para ANDE
     * IMPORTANTE: Usa campo "nis" (NO cedula), tipo NUMBER
     */
    fun createANDEConfiguration(): ServiceConfig {
        return ServiceConfig(
            // Campos visibles
            showPhone = false,
            showCedula = true,  // Este será el campo NIS
            showAmount = false,
            showConsultButton = true,
            showNacimiento = false,
            
            // Textos e indicaciones
            cedulaHint = "Ingrese el nro de NIS",    // Texto específico para ANDE
            cedulaDefaultValue = "",
            cedulaInputType = ServiceConfig.InputType.NUMBER,  // Tipo NUMBER
            
            // Configuración SIM
            useAlternativeSIM = false,  // SIM 1 (Tigo)
            
            // FASE 8: Configuración USSD y Print
            ussdTemplate = "*555*5*1*2*1*{nis}*{nis}*#",
            printTemplate = """=====================
Genio Tecni
ANDE
Fecha: {fecha}
Hora: {hora}
NIS: {nis}
Monto: {ref1} Gs.
Ref1: {ref2}
=====================""",
            fieldLabels = mapOf(
                "cedula" to "NIS",    // Mapea el campo cedula a etiqueta NIS
                "nis" to "NIS"
            ),
            specialFieldMappings = mapOf(
                "cedula" to "nis"     // Campo cedula se mapea internamente a nis
            ),
            hasCommission = false,
            smsSearchType = "ANDE"
        )
    }
    
    /**
     * Configuración correcta para Giros Tigo
     * IMPORTANTE: Aplica comisión del 6%
     */
    fun createGirosTigoConfiguration(): ServiceConfig {
        return ServiceConfig(
            // Campos visibles
            showPhone = true,
            showCedula = true,
            showAmount = true,
            showConsultButton = false,
            showNacimiento = false,
            
            // Textos e indicaciones
            phoneHint = "Número de teléfono",
            cedulaHint = "Número de cédula",
            amountHint = "Monto",
            
            // Configuración SIM
            useAlternativeSIM = false,  // SIM 1 (Tigo)
            
            // FASE 8: Configuración USSD y Print
            ussdTemplate = "*555*1*{numero}*{cedula}*1*{monto}#",
            printTemplate = """=====================
Genio Tecni
Giros Tigo
Fecha: {fecha}
Hora: {hora}
Numero: {numero}
Cedula: {cedula}
Monto: {monto} Gs.
Comision: {comision} Gs.
Ref1: {ref1}
Ref2: {ref2}
=====================""",
            fieldLabels = mapOf(
                "numero" to "Numero",
                "cedula" to "Cedula",
                "monto" to "Monto"
            ),
            hasCommission = true,
            commissionRate = 0.06,  // 6% de comisión
            smsSearchType = "Giros Tigo"
        )
    }
    
    /**
     * Configuración correcta para Retiros Tigo
     * IMPORTANTE: Aplica comisión del 6%
     */
    fun createRetirosTigoConfiguration(): ServiceConfig {
        return ServiceConfig(
            // Campos visibles
            showPhone = true,
            showCedula = true,
            showAmount = true,
            showConsultButton = false,
            showNacimiento = false,
            
            // Textos e indicaciones
            phoneHint = "Número de teléfono",
            cedulaHint = "Número de cédula",
            amountHint = "Monto",
            
            // Configuración SIM
            useAlternativeSIM = false,  // SIM 1 (Tigo)
            
            // FASE 8: Configuración USSD y Print
            ussdTemplate = "*555*2*{numero}*{cedula}*1*{monto}#",
            printTemplate = """=====================
Genio Tecni
Retiros Tigo
Fecha: {fecha}
Hora: {hora}
Numero: {numero}
Cedula: {cedula}
Monto: {monto} Gs.
Comision: {comision} Gs.
Ref1: {ref1}
Ref2: {ref2}
=====================""",
            fieldLabels = mapOf(
                "numero" to "Numero",
                "cedula" to "Cedula",
                "monto" to "Monto"
            ),
            hasCommission = true,
            commissionRate = 0.06,  // 6% de comisión
            smsSearchType = "Retiros Tigo"
        )
    }
    
    /**
     * Configuración correcta para servicios Personal
     * IMPORTANTE: Usa SIM 2
     */
    fun createPersonalConfiguration(serviceName: String): ServiceConfig {
        val ussdTemplates = mapOf(
            "Carga Billetera Personal" to "*200*3*{numero}*{monto}#",
            "Retiros Personal" to "*200*2*{numero}*{monto}#",
            "Telefonia Personal" to "*200*4*{numero}*{monto}#"
        )
        
        return ServiceConfig(
            // Campos visibles
            showPhone = true,
            showCedula = false,  // Personal no usa cédula
            showAmount = true,
            showConsultButton = serviceName == "Telefonia Personal",
            showNacimiento = false,
            
            // Textos e indicaciones
            phoneHint = "Número Personal",
            amountHint = "Monto",
            
            // Configuración SIM
            useAlternativeSIM = true,  // SIM 2 (Personal) - MUY IMPORTANTE
            
            // FASE 8: Configuración USSD y Print
            ussdTemplate = ussdTemplates[serviceName] ?: "*200*3*{numero}*{monto}#",
            printTemplate = """=====================
Genio Tecni
$serviceName
Fecha: {fecha}
Hora: {hora}
Numero: {numero}
Monto: {monto} Gs.
Ref1: {ref1}
=====================""",
            fieldLabels = mapOf(
                "numero" to "Numero",
                "monto" to "Monto"
            ),
            hasCommission = false,
            smsSearchType = serviceName
        )
    }
    
    /**
     * Configuración para cooperativas con tarjetas
     * Con etiquetas específicas según el tipo de tarjeta
     */
    fun createCardServiceConfiguration(
        serviceName: String,
        serviceId: Int,
        cardType: String
    ): ServiceConfig {
        val digitLabel = when (cardType.lowercase()) {
            "mastercard" -> "Primeros 8 digitos"
            "visa" -> "Primeros 8 digitos"  
            "cabal" -> "Primeros 10 digitos"
            "credicard" -> "Primeros 8 digitos"
            else -> "Ingrese el Nro de Tarjeta"
        }
        
        return ServiceConfig(
            // Campos visibles
            showPhone = false,
            showCedula = true,  // Campo será para tarjeta
            showAmount = false,
            showConsultButton = true,
            showNacimiento = false,
            
            // Textos específicos para tarjetas
            cedulaHint = digitLabel,
            cedulaInputType = ServiceConfig.InputType.NUMBER,
            
            // Configuración SIM
            useAlternativeSIM = false,  // SIM 1 (Tigo)
            
            // FASE 8: Configuración USSD y Print
            ussdTemplate = USSDConfiguration.getUSSDTemplate(serviceId)?.template ?: "",
            printTemplate = """=====================
Genio Tecni
$serviceName
Fecha: {fecha}
Hora: {hora}
Tarjeta: {tarjeta}
Monto: {ref1} Gs.
Ref1: {ref2}
=====================""",
            fieldLabels = mapOf(
                "cedula" to "Tarjeta",
                "tarjeta" to "Tarjeta"
            ),
            specialFieldMappings = mapOf(
                "cedula" to "tarjeta"
            ),
            hasCommission = false,
            smsSearchType = serviceName
        )
    }
    
    /**
     * Configuración para Reseteo PIN
     * IMPORTANTE: Incluye campo fecha de nacimiento
     */
    fun createResetPinConfiguration(): ServiceConfig {
        return ServiceConfig(
            // Campos visibles
            showPhone = true,
            showCedula = true,
            showAmount = false,
            showConsultButton = false,
            showNacimiento = true,  // Campo adicional único
            
            // Textos e indicaciones
            phoneHint = "Número de teléfono",
            cedulaHint = "Número de cédula",
            nacimientoHint = "Fecha de nacimiento",
            nacimientoInputType = ServiceConfig.InputType.NUMBER,
            
            // Configuración SIM
            useAlternativeSIM = false,  // SIM 1 (Tigo)
            
            // FASE 8: Configuración USSD y Print
            ussdTemplate = "*555*6*3*{numero}*1*{cedula}*{nacimiento}*#",
            printTemplate = """=====================
Genio Tecni
Reseteo de Pin (Cliente)
Fecha: {fecha}
Hora: {hora}
Numero: {numero}
Cedula: {cedula}
Fecha Nacimiento: {nacimiento}
Ref1: {ref1}
Ref2: {ref2}
=====================""",
            fieldLabels = mapOf(
                "numero" to "Numero",
                "cedula" to "Cedula",
                "nacimiento" to "Fecha Nacimiento"
            ),
            hasCommission = false,
            smsSearchType = "Reseteo de Pin"
        )
    }
}