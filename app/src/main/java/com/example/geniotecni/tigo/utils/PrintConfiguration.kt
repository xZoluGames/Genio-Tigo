package com.example.geniotecni.tigo.utils

/**
 * Configuración centralizada de plantillas de impresión
 * Fase 8: Estandarización de códigos USSD y formatos de impresión
 */
object PrintConfiguration {
    
    /**
     * Plantilla base estándar para impresión
     */
    const val BASE_TEMPLATE = """=====================
Genio Tecni
{servicio}
Fecha: {fecha}
Hora: {hora}
{campos_especificos}
{monto_info}
{comision_info}
{referencias}
====================="""
    
    /**
     * Plantillas específicas por tipo de servicio
     */
    val PRINT_TEMPLATES = mapOf(
        
        // SERVICIOS TIGO CON COMISIÓN (Giros y Retiros)
        0 to PrintTemplate(
            template = """=====================
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
            hasCommission = true,
            commissionRate = 0.06
        ),
        
        1 to PrintTemplate(
            template = """=====================
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
            hasCommission = true,
            commissionRate = 0.06
        ),
        
        // SERVICIOS TIGO SIN COMISIÓN
        2 to PrintTemplate(
            template = """=====================
Genio Tecni
Carga Billetera Tigo
Fecha: {fecha}
Hora: {hora}
Numero: {numero}
Cedula: {cedula}
Monto: {monto} Gs.
Ref1: {ref1}
Ref2: {ref2}
=====================""",
            hasCommission = false
        ),
        
        3 to PrintTemplate(
            template = """=====================
Genio Tecni
Telefonia Tigo
Fecha: {fecha}
Hora: {hora}
Numero: {numero}
Monto: {ref1} Gs.
Ref1: {ref2}
=====================""",
            hasCommission = false,
            specialMounting = SpecialMounting.AMOUNT_IN_REF1
        ),
        
        4 to PrintTemplate(
            template = """=====================
Genio Tecni
Pago TV e Internet Hogar
Fecha: {fecha}
Hora: {hora}
Cedula: {cedula}
Monto: {ref1} Gs.
Ref1: {ref2}
=====================""",
            hasCommission = false,
            specialMounting = SpecialMounting.AMOUNT_IN_REF1
        ),
        
        5 to PrintTemplate(
            template = """=====================
Genio Tecni
Antena (Wimax)
Fecha: {fecha}
Hora: {hora}
Cuenta: {cuenta}
Monto: {ref1} Gs.
Ref1: {ref2}
=====================""",
            hasCommission = false,
            specialMounting = SpecialMounting.AMOUNT_IN_REF1
        ),
        
        6 to PrintTemplate(
            template = """=====================
Genio Tecni
Tigo TV anticipado
Fecha: {fecha}
Hora: {hora}
Cliente: {cliente}
Monto: {ref1} Gs.
Ref1: {ref2}
=====================""",
            hasCommission = false,
            specialMounting = SpecialMounting.AMOUNT_IN_REF1
        ),
        
        // SERVICIOS PÚBLICOS
        7 to PrintTemplate(
            template = """=====================
Genio Tecni
ANDE
Fecha: {fecha}
Hora: {hora}
NIS: {nis}
Monto: {ref1} Gs.
Ref1: {ref2}
=====================""",
            hasCommission = false,
            specialMounting = SpecialMounting.AMOUNT_IN_REF1
        ),
        
        8 to PrintTemplate(
            template = """=====================
Genio Tecni
ESSAP
Fecha: {fecha}
Hora: {hora}
ISSAN: {issan}
Monto: {ref1} Gs.
Ref1: {ref2}
=====================""",
            hasCommission = false,
            specialMounting = SpecialMounting.AMOUNT_IN_REF1
        ),
        
        9 to PrintTemplate(
            template = """=====================
Genio Tecni
COPACO
Fecha: {fecha}
Hora: {hora}
Telefono o Cuenta: {telefono_cuenta}
Monto: {ref1} Gs.
Ref1: {ref2}
=====================""",
            hasCommission = false,
            specialMounting = SpecialMounting.AMOUNT_IN_REF1
        ),
        
        // PERSONAL
        10 to PrintTemplate(
            template = """=====================
Genio Tecni
Carga Billetera Personal
Fecha: {fecha}
Hora: {hora}
Numero: {numero}
Monto: {monto} Gs.
Ref1: {ref1}
=====================""",
            hasCommission = false
        ),
        
        11 to PrintTemplate(
            template = """=====================
Genio Tecni
Retiros Personal
Fecha: {fecha}
Hora: {hora}
Numero: {numero}
Monto: {monto} Gs.
Ref1: {ref1}
=====================""",
            hasCommission = false
        ),
        
        12 to PrintTemplate(
            template = """=====================
Genio Tecni
Telefonia Personal
Fecha: {fecha}
Hora: {hora}
Numero: {numero}
Monto: {monto} Gs.
Ref1: {ref1}
=====================""",
            hasCommission = false
        ),
        
        // RESETEO PIN - Servicio especial
        75 to PrintTemplate(
            template = """=====================
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
            hasCommission = false
        )
    )
    
    /**
     * Plantilla genérica para servicios no específicamente configurados
     */
    val GENERIC_TEMPLATE = PrintTemplate(
        template = """=====================
Genio Tecni
{servicio}
Fecha: {fecha}
Hora: {hora}
CI: {ci}
Monto: {ref1} Gs.
Ref1: {ref2}
=====================""",
        hasCommission = false,
        specialMounting = SpecialMounting.AMOUNT_IN_REF1
    )
    
    /**
     * Plantilla genérica para servicios con tarjetas
     */
    val CARD_TEMPLATE = PrintTemplate(
        template = """=====================
Genio Tecni
{servicio}
Fecha: {fecha}
Hora: {hora}
Tarjeta: {tarjeta}
Monto: {ref1} Gs.
Ref1: {ref2}
=====================""",
        hasCommission = false,
        specialMounting = SpecialMounting.AMOUNT_IN_REF1
    )
    
    /**
     * Datos de configuración de plantilla de impresión
     */
    data class PrintTemplate(
        val template: String,
        val hasCommission: Boolean = false,
        val commissionRate: Double = 0.0,
        val specialMounting: SpecialMounting = SpecialMounting.NORMAL
    )
    
    /**
     * Enum para manejo especial de montos
     */
    enum class SpecialMounting {
        NORMAL,           // Monto viene en campo monto
        AMOUNT_IN_REF1    // Monto viene en ref1 (servicios públicos y consultas)
    }
    
    /**
     * Obtiene la plantilla de impresión para un servicio específico
     */
    fun getPrintTemplate(serviceId: Int): PrintTemplate {
        return PRINT_TEMPLATES[serviceId] ?: when {
            // Servicios con tarjetas (Mastercard, Visa, Cabal, etc.)
            serviceId in listOf(34, 35, 36, 37, 38, 42, 43, 49, 53, 54, 55, 57, 58, 65, 66, 67, 69, 71, 72, 73) -> CARD_TEMPLATE
            // Servicios genéricos (financieras y cooperativas)
            else -> GENERIC_TEMPLATE
        }
    }
    
    /**
     * Determina si un servicio es de tarjeta basado en su ID
     */
    fun isCardService(serviceId: Int): Boolean {
        return serviceId in listOf(34, 35, 36, 37, 38, 42, 43, 49, 53, 54, 55, 57, 58, 65, 66, 67, 69, 71, 72, 73)
    }
    
    /**
     * Obtiene las etiquetas específicas para campos de tarjetas
     */
    fun getCardFieldLabel(serviceId: Int): String {
        return when (serviceId) {
            34, 49, 43, 71, 65 -> "Primeros 8 digitos"      // Mastercard
            37, 54, 58, 69 -> "Primeros 8 digitos"          // Visa  
            35, 38, 42, 57, 67, 73 -> "Primeros 10 digitos" // Cabal
            55, 66, 72 -> "Primeros 8 digitos"              // Credicard
            36, 53 -> "Ingrese el Nro de Tarjeta"           // Otros
            else -> "Ingrese el Nro de CI"
        }
    }
}