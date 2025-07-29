package com.example.geniotecni.tigo.utils

/**
 * Configuración centralizada de códigos USSD extraídos de MainActivity antigua
 * Fase 8: Estandarización de códigos USSD y formatos de impresión
 */
object USSDConfiguration {
    
    /**
     * Mapa completo de códigos USSD por ID de servicio
     * Todos los códigos verificados contra MainActivity antigua
     */
    val USSD_CODES = mapOf(
        // TIGO - SIM 1
        0 to USSDTemplate("*555*1*{numero}*{cedula}*1*{monto}#", SimCard.SIM1),        // Giros Tigo
        1 to USSDTemplate("*555*2*{numero}*{cedula}*1*{monto}#", SimCard.SIM1),        // Retiros Tigo
        2 to USSDTemplate("*555*3*1*{cedula}*1*{numero}*{monto}#", SimCard.SIM1),      // Carga Billetera Tigo
        3 to USSDTemplate("*555*5*1*1*1*{numero}*{numero}*#", SimCard.SIM1),           // Telefonia Tigo
        4 to USSDTemplate("*555*5*1*1*2*{cedula}*{cedula}*#", SimCard.SIM1),           // Pago TV e Internet Hogar
        5 to USSDTemplate("*555*5*1*1*3*{cuenta}*{cuenta}*#", SimCard.SIM1),           // Antena (Wimax)
        6 to USSDTemplate("*555*5*1*1*4*{cliente}*{cliente}*#", SimCard.SIM1),         // Tigo TV anticipado
        
        // Servicios Públicos - SIM 1
        7 to USSDTemplate("*555*5*1*2*1*{nis}*{nis}*#", SimCard.SIM1),                 // ANDE
        8 to USSDTemplate("*555*5*1*2*2*{issan}*{issan}*#", SimCard.SIM1),             // ESSAP
        9 to USSDTemplate("*555*5*1*2*4*{telefono_cuenta}*{telefono_cuenta}*#", SimCard.SIM1), // COPACO
        
        // Personal - SIM 2
        10 to USSDTemplate("*200*3*{numero}*{monto}#", SimCard.SIM2),                  // Carga Billetera Personal
        11 to USSDTemplate("*200*2*{numero}*{monto}#", SimCard.SIM2),                  // Retiros Personal
        12 to USSDTemplate("*200*4*{numero}*{monto}#", SimCard.SIM2),                  // Telefonia Personal
        
        // Financieras (Categoría 3) - SIM 1
        13 to USSDTemplate("*555*5*1*3*1*{ci}*{ci}*#", SimCard.SIM1),                  // Alex S.A
        14 to USSDTemplate("*555*5*1*3*2*{ci}*{ci}*#", SimCard.SIM1),                  // Electroban
        15 to USSDTemplate("*555*5*1*3*3*{ci}*{ci}*#", SimCard.SIM1),                  // Leopard
        16 to USSDTemplate("*555*5*1*3*4*{ci}*{ci}*#", SimCard.SIM1),                  // Chacomer
        17 to USSDTemplate("*555*5*1*3*5*{ci}*{ci}*#", SimCard.SIM1),                  // Inverfin
        
        // Financieras (Categoría 4) - SIM 1
        18 to USSDTemplate("*555*5*1*4*1*{ci}*{ci}*#", SimCard.SIM1),                  // Che Duo-Carsa (Prestamos)
        19 to USSDTemplate("*555*5*1*4*2*{ci}*{ci}*#", SimCard.SIM1),                  // Banco Familar (Prestamos)
        20 to USSDTemplate("*555*5*1*4*3*{ci}*{ci}*#", SimCard.SIM1),                  // Financiera El Comercio
        21 to USSDTemplate("*555*5*1*4*4*{ci}*{ci}*#", SimCard.SIM1),                  // Interfisa (Prestamos)
        22 to USSDTemplate("*555*5*1*4*5*{ci}*{ci}*#", SimCard.SIM1),                  // Financiera Paraguayo Japonesa (Prestamos)
        23 to USSDTemplate("*555*5*1*4*6*{ci}*{ci}*#", SimCard.SIM1),                  // Credito Amigo (Prestamos)
        24 to USSDTemplate("*555*5*1*4*7*{ci}*{ci}*#", SimCard.SIM1),                  // Tu Financiera (Prestamos)
        25 to USSDTemplate("*555*5*1*4*8*{ci}*{ci}*#", SimCard.SIM1),                  // Funacion Industrial (Prestamos)
        26 to USSDTemplate("*555*5*1*4*9*{ci}*{ci}*#", SimCard.SIM1),                  // Banco Vision Pago de Tarjetas
        27 to USSDTemplate("*555*5*1*4*10*{ci}*{ci}*#", SimCard.SIM1),                 // Banco Vision Pago de Prestamos
        28 to USSDTemplate("*555*5*1*4*11*{ci}*{ci}*#", SimCard.SIM1),                 // Fiado.Net (Prestamos)
        29 to USSDTemplate("*555*5*1*4*12*{ci}*{ci}*#", SimCard.SIM1),                 // Financiera Solar Pago de Tarjetas
        30 to USSDTemplate("*555*5*1*4*13*{ci}*{ci}*#", SimCard.SIM1),                 // Financiera Solar Pago de Prestamos
        31 to USSDTemplate("*555*5*1*4*14*{ci}*{ci}*#", SimCard.SIM1),                 // Interfisa Pago de Tarjetas
        32 to USSDTemplate("*555*5*1*4*15*{ci}*{ci}*#", SimCard.SIM1),                 // Banco Itau (Prestamos)
        
        // Cooperativas (Categoría 5) - SIM 1
        33 to USSDTemplate("*555*5*1*5*1*1*{ci}*{ci}*#", SimCard.SIM1),                // Cooperativa Universitaria (Prestamos)
        34 to USSDTemplate("*555*5*1*5*1*2*{tarjeta}*{tarjeta}*#", SimCard.SIM1),      // Cooperativa Universitaria (Tarjeta Mastercard)
        35 to USSDTemplate("*555*5*1*5*1*3*{tarjeta}*{tarjeta}*#", SimCard.SIM1),      // Cooperativa Universitaria (Tarjeta Cabal)
        36 to USSDTemplate("*555*5*1*5*1*4*{tarjeta}*{tarjeta}*#", SimCard.SIM1),      // Cooperativa Universitaria (Tarjeta Panal)
        37 to USSDTemplate("*555*5*1*5*2*1*{tarjeta}*{tarjeta}*#", SimCard.SIM1),      // CopexSanJo (Tarjeta Credito Visa)
        38 to USSDTemplate("*555*5*1*5*2*2*{tarjeta}*{tarjeta}*#", SimCard.SIM1),      // CopexSanJo (Tarjeta Credito Cabal)
        39 to USSDTemplate("*555*5*1*5*2*3*{ci}*{ci}*#", SimCard.SIM1),                // CopexSanJo (Solidaridad)
        40 to USSDTemplate("*555*5*1*5*2*4*{ci}*{ci}*#", SimCard.SIM1),                // CopexSanJo (Cuotas)
        41 to USSDTemplate("*555*5*1*5*2*5*{ci}*{ci}*#", SimCard.SIM1),                // CopexSanJo (Aportes)
        42 to USSDTemplate("*555*5*1*5*3*1*{tarjeta}*{tarjeta}*#", SimCard.SIM1),      // CMCP (Tarjeta Cabal)
        43 to USSDTemplate("*555*5*1*5*3*2*{tarjeta}*{tarjeta}*#", SimCard.SIM1),      // CMCP (Tarjeta Mastercard)
        44 to USSDTemplate("*555*5*1*5*3*3*{ci}*{ci}*#", SimCard.SIM1),                // CMCP (Credito)
        45 to USSDTemplate("*555*5*1*5*3*4*{ci}*{ci}*#", SimCard.SIM1),                // CMCP (Aporte)
        46 to USSDTemplate("*555*5*1*5*4*1*{ci}*{ci}*#", SimCard.SIM1),                // Cooperativa Tupãrenda (Aporte y Solidaridad)
        47 to USSDTemplate("*555*5*1*5*4*2*{ci}*{ci}*#", SimCard.SIM1),                // Cooperativa Tupãrenda (Prestamos)
        48 to USSDTemplate("*555*5*1*5*5*1*{ci}*{ci}*#", SimCard.SIM1),                // Cooperativa San Cristobal (Admision)
        49 to USSDTemplate("*555*5*1*5*5*2*{tarjeta}*{tarjeta}*#", SimCard.SIM1),      // Cooperativa San Cristobal (Tarjeta Mastercard)
        50 to USSDTemplate("*555*5*1*5*5*3*{ci}*{ci}*#", SimCard.SIM1),                // Cooperativa San Cristobal (Solidaridad)
        51 to USSDTemplate("*555*5*1*5*5*4*{ci}*{ci}*#", SimCard.SIM1),                // Cooperativa San Cristobal (Aporte)
        52 to USSDTemplate("*555*5*1*5*5*5*{ci}*{ci}*#", SimCard.SIM1),                // Cooperativa San Cristobal (Prestamo)
        53 to USSDTemplate("*555*5*1*5*5*6*{tarjeta}*{tarjeta}*#", SimCard.SIM1),      // Cooperativa San Cristobal (Tarjeta Unica)
        54 to USSDTemplate("*555*5*1*5*5*7*{tarjeta}*{tarjeta}*#", SimCard.SIM1),      // Cooperativa San Cristobal (Tarjeta Visa)
        55 to USSDTemplate("*555*5*1*5*5*8*{tarjeta}*{tarjeta}*#", SimCard.SIM1),      // Cooperativa San Cristobal (Tarjeta Credicard)
        56 to USSDTemplate("*555*5*1*5*6*1*{ci}*{ci}*#", SimCard.SIM1),                // Cooperativa Yoayu (Sepelios)
        57 to USSDTemplate("*555*5*1*5*6*2*{tarjeta}*{tarjeta}*#", SimCard.SIM1),      // Cooperativa Yoayu (Tarjeta Cabal)
        58 to USSDTemplate("*555*5*1*5*6*3*{tarjeta}*{tarjeta}*#", SimCard.SIM1),      // Cooperativa Yoayu (Tarjeta Visa)
        59 to USSDTemplate("*555*5*1*5*6*4*{ci}*{ci}*#", SimCard.SIM1),                // Cooperativa Yoayu (Fondos)
        60 to USSDTemplate("*555*5*1*5*6*5*{ci}*{ci}*#", SimCard.SIM1),                // Cooperativa Yoayu (Solidaridad)
        61 to USSDTemplate("*555*5*1*5*6*6*{ci}*{ci}*#", SimCard.SIM1),                // Cooperativa Yoayu (Aporte)
        62 to USSDTemplate("*555*5*1*5*6*7*{ci}*{ci}*#", SimCard.SIM1),                // Cooperativa Yoayu
        63 to USSDTemplate("*555*5*1*5*7*1*{ci}*{ci}*#", SimCard.SIM1),                // Cooperativa Coomecipar (Solidaridad)
        64 to USSDTemplate("*555*5*1*5*7*2*{ci}*{ci}*#", SimCard.SIM1),                // Cooperativa Coomecipar (Prestamo)
        65 to USSDTemplate("*555*5*1*5*7*3*{tarjeta}*{tarjeta}*#", SimCard.SIM1),      // Cooperativa Coomecipar (Tarjeta Mastercard)
        66 to USSDTemplate("*555*5*1*5*7*4*{tarjeta}*{tarjeta}*#", SimCard.SIM1),      // Cooperativa Coomecipar (Tarjeta Credicard)
        67 to USSDTemplate("*555*5*1*5*7*5*{tarjeta}*{tarjeta}*#", SimCard.SIM1),      // Cooperativa Coomecipar (Tarjeta Cabal)
        68 to USSDTemplate("*555*5*1*5*7*6*{ci}*{ci}*#", SimCard.SIM1),                // Cooperativa Coomecipar (Aportes)
        69 to USSDTemplate("*555*5*1*5*8*1*{tarjeta}*{tarjeta}*#", SimCard.SIM1),      // Cooperativa Medalla Milagrosa (Tarjeta Visa)
        70 to USSDTemplate("*555*5*1*5*8*2*{ci}*{ci}*#", SimCard.SIM1),                // Cooperativa Medalla Milagrosa (Solidaridad)
        71 to USSDTemplate("*555*5*1*5*8*3*{tarjeta}*{tarjeta}*#", SimCard.SIM1),      // Cooperativa Medalla Milagrosa (Tarjeta Mastercard)
        72 to USSDTemplate("*555*5*1*5*8*4*{tarjeta}*{tarjeta}*#", SimCard.SIM1),      // Cooperativa Medalla Milagrosa (Tarjeta Credicard)
        73 to USSDTemplate("*555*5*1*5*8*5*{tarjeta}*{tarjeta}*#", SimCard.SIM1),      // Cooperativa Medalla Milagrosa (Tarjeta Cabal)
        74 to USSDTemplate("*555*5*1*5*8*6*{ci}*{ci}*#", SimCard.SIM1),                // Cooperativa Medalla Milagrosa (Creditos)
        
        // Servicios Especiales - SIM 1
        75 to USSDTemplate("*555*6*3*{numero}*1*{cedula}*{nacimiento}*#", SimCard.SIM1) // Reseteo de Pin (Cliente)
    )
    
    /**
     * Plantilla USSD con SIM asociada
     */
    data class USSDTemplate(
        val template: String,
        val simCard: SimCard
    )
    
    /**
     * Enum para selección de SIM
     */
    enum class SimCard(val index: Int) {
        SIM1(0),    // Tigo y mayoría de servicios
        SIM2(1)     // Personal
    }
    
    /**
     * Obtiene la plantilla USSD para un servicio específico
     */
    fun getUSSDTemplate(serviceId: Int): USSDTemplate? {
        return USSD_CODES[serviceId]
    }
    
    /**
     * Genera código USSD reemplazando placeholders con valores reales
     */
    fun generateUSSDCode(serviceId: Int, params: Map<String, String>): String? {
        val template = getUSSDTemplate(serviceId) ?: return null
        var code = template.template
        
        // Reemplazar placeholders con valores reales
        params.forEach { (key, value) ->
            code = code.replace("{$key}", value)
        }
        
        return code
    }
    
    /**
     * Verifica si un servicio requiere SIM específica
     */
    fun getRequiredSIM(serviceId: Int): SimCard? {
        return getUSSDTemplate(serviceId)?.simCard
    }
}