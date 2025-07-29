package com.example.geniotecni.tigo.utils

/**
 * Application-wide constants
 */
object Constants {

    /**
     * Complete list of all service names in order.
     * This array matches the index positions used throughout the application.
     */
    val SERVICE_NAMES = arrayOf(
        "Giros Tigo",                                                         // 0
        "Retiros Tigo",                                                       // 1
        "Carga Billetera Tigo",                                              // 2
        "Telefonia Tigo",                                                     // 3
        "Pago TV e Internet Hogar",                                          // 4
        "Antena (Wimax)",                                                     // 5
        "Tigo TV anticipado",                                                 // 6
        "ANDE",                                                               // 7
        "ESSAP",                                                              // 8
        "COPACO",                                                             // 9
        "Carga Billetera Personal",                                           // 10
        "Retiros Personal",                                                   // 11
        "Telefonia Personal",                                                 // 12
        "Alex S.A",                                                           // 13
        "Electroban",                                                         // 14
        "Leopard",                                                            // 15
        "Chacomer",                                                           // 16
        "Inverfin",                                                           // 17
        "Che Duo-Carsa (Prestamos)",                                         // 18
        "Banco Familar (Prestamos)",                                          // 19
        "Financiera El Comercio",                                             // 20
        "Interfisa (Prestamos)",                                              // 21
        "Financiera Paraguayo Japonesa (Prestamos)",                         // 22
        "Credito Amigo (Prestamos)",                                          // 23
        "Tu Financiera (Prestamos)",                                          // 24
        "Funacion Industrial (Prestamos)",                                    // 25
        "Banco Vision Pago de Tarjetas",                                      // 26
        "Banco Vision Pago de Prestamos",                                     // 27
        "Fiado.Net (Prestamos)",                                              // 28
        "Financiera Solar Pago de Tarjetas",                                  // 29
        "Financiera Solar Pago de Prestamos",                                 // 30
        "Interfisa Pago de Tarjetas",                                         // 31
        "Banco Itau (Prestamos)",                                             // 32
        "Cooperativa Universitaria (Prestamos)",                              // 33
        "Cooperativa Universitaria (Tarjeta Mastercard)",                     // 34
        "Cooperativa Universitaria (Tarjeta Cabal)",                          // 35
        "Cooperativa Universitaria (Tarjeta Panal)",                          // 36
        "CopexSanJo (Tarjeta Credito Visa)",                                 // 37
        "CopexSanJo (Tarjeta Credito Cabal)",                                // 38
        "CopexSanJo (Solidaridad)",                                          // 39
        "CopexSanJo (Cuotas)",                                               // 40
        "CopexSanJo (Aportes)",                                              // 41
        "Caja Mutual De Cooperativistas Del Paraguay CMCP (Tarjeta Cabal)",   // 42
        "Caja Mutual De Cooperativistas Del Paraguay CMCP (Tarjeta Mastercard)", // 43
        "Caja Mutual De Cooperativistas Del Paraguay CMCP (Credito)",         // 44
        "Caja Mutual De Cooperativistas Del Paraguay CMCP (Aporte)",          // 45
        "Cooperativa Tupãrenda (Aporte y Solidaridad)",                      // 46
        "Cooperativa Tupãrenda (Prestamos)",                                  // 47
        "Cooperativa San Cristobal (Admision)",                               // 48
        "Cooperativa San Cristobal (Tarjeta Mastercard)",                     // 49
        "Cooperativa San Cristobal (Solidaridad)",                            // 50
        "Cooperativa San Cristobal (Aporte)",                                 // 51
        "Cooperativa San Cristobal (Prestamo)",                               // 52
        "Cooperativa San Cristobal (Tarjeta Unica)",                          // 53
        "Cooperativa San Cristobal (Tarjeta Visa)",                           // 54
        "Cooperativa San Cristobal (Tarjeta Credicard)",                      // 55
        "Cooperativa Yoayu (Sepelios)",                                       // 56
        "Cooperativa Yoayu (Tarjeta Cabal)",                                  // 57
        "Cooperativa Yoayu (Tarjeta Visa)",                                   // 58
        "Cooperativa Yoayu (Fondos)",                                         // 59
        "Cooperativa Yoayu (Solidaridad)",                                    // 60
        "Cooperativa Yoayu (Aporte)",                                         // 61
        "Cooperativa Yoayu",                                                  // 62
        "Cooperativa Coomecipar (Solidaridad)",                               // 63
        "Cooperativa Coomecipar (Prestamo)",                                  // 64
        "Cooperativa Coomecipar (Tarjeta Mastercard)",                        // 65
        "Cooperativa Coomecipar (Tarjeta Credicard)",                         // 66
        "Cooperativa Coomecipar (Tarjeta Cabal)",                             // 67
        "Cooperativa Coomecipar (Aportes)",                                   // 68
        "Cooperativa Medalla Milagrosa (Tarjeta Visa)",                       // 69
        "Cooperativa Medalla Milagrosa (Solidaridad)",                        // 70
        "Cooperativa Medalla Milagrosa (Tarjeta Mastercard)",                 // 71
        "Cooperativa Medalla Milagrosa (Tarjeta Credicard)",                  // 72
        "Cooperativa Medalla Milagrosa (Tarjeta Cabal)",                      // 73
        "Cooperativa Medalla Milagrosa (Creditos)",                           // 74
        "Reseteo de Pin (Cliente)"                                            // 75
    )

    // Constantes no utilizadas eliminadas - solo se mantienen SERVICE_NAMES y Formats (deprecado)

    // ServiceCategories y USSDPrefixes eliminados - no utilizados
    // Formats - DEPRECATED: Use AppConfig.Formats instead
    @Deprecated("Use AppConfig.Formats instead", ReplaceWith("AppConfig.Formats"))
    object Formats {
        const val DATE_FORMAT = AppConfig.Formats.DATE_FORMAT
        const val TIME_FORMAT = AppConfig.Formats.TIME_FORMAT
        const val DATETIME_FORMAT = AppConfig.Formats.DATETIME_FORMAT
        const val DECIMAL_FORMAT = AppConfig.Formats.DECIMAL_FORMAT
    }
    // SMS_SENDER_NUMBERS eliminado - no utilizado
}