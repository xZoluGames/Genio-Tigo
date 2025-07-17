package com.example.geniotecni.tigo.utils

object Constants {
    // Service names
    val SERVICE_NAMES = arrayOf(
        "Giros Tigo", "Retiros Tigo", "Carga Billetera Tigo", "Telefonia Tigo",
        "Pago TV e Internet Hogar", "Antena (Wimax)", "Tigo TV anticipado", "Reseteo de Cliente",
        "ANDE", "ESSAP", "COPACO", "Retiros Personal",
        "Telefonia Personal", "Alex S.A", "Electroban", "Leopard", "Chacomer",
        "Inverfin", "Che Duo-Carsa (Prestamos)", "Banco Familar (Prestamos)",
        "Financiera El Comercio", "Interfisa (Prestamos)",
        "Financiera Paraguayo Japonesa (Prestamos)", "Credito Amigo (Prestamos)",
        "Tu Financiera (Prestamos)", "Funacion Industrial (Prestamos)",
        "Banco Vision Pago de Tarjetas", "Banco Vision Pago de Prestamos",
        "Fiado.Net (Prestamos)", "Financiera Solar Pago de Tarjetas",
        "Financiera Solar Pago de Prestamos", "Interfisa Pago de Tarjetas",
        "Banco Itau (Prestamos)", "Cooperativa Universitaria (Prestamos)",
        "Cooperativa Universitaria (Tarjeta Mastercard)",
        "Cooperativa Universitaria (Tarjeta Cabal)",
        "Cooperativa Universitaria (Tarjeta Panal)",
        "CopexSanJo (Tarjeta Credito Visa)", "CopexSanJo (Tarjeta Credito Cabal)",
        "CopexSanJo (Solidaridad)", "CopexSanJo (Cuotas)", "CopexSanJo (Aportes)",
        "Caja Mutual De Cooperativistas Del Paraguay CMCP (Tarjeta Cabal)",
        "Caja Mutual De Cooperativistas Del Paraguay CMCP (Tarjeta Mastercard)",
        "Caja Mutual De Cooperativistas Del Paraguay CMCP (Credito)",
        "Caja Mutual De Cooperativistas Del Paraguay CMCP (Aporte)",
        "Cooperativa Tupãrenda (Aporte y Solidaridad)",
        "Cooperativa Tupãrenda (Prestamos)", "Cooperativa San Cristobal (Admision)",
        "Cooperativa San Cristobal (Tarjeta Mastercard)",
        "Cooperativa San Cristobal (Solidaridad)", "Cooperativa San Cristobal (Aporte)",
        "Cooperativa San Cristobal (Prestamo)", "Cooperativa San Cristobal (Tarjeta Unica)",
        "Cooperativa San Cristobal (Tarjeta Visa)", "Cooperativa San Cristobal (Tarjeta Credicard)",
        "Cooperativa Yoayu (Sepelios)", "Cooperativa Yoayu (Tarjeta Cabal)",
        "Cooperativa Yoayu (Tarjeta Visa)", "Cooperativa Yoayu (Fondos)",
        "Cooperativa Yoayu (Solidaridad)", "Cooperativa Yoayu (Aporte)",
        "Cooperativa Yoayu", "Cooperativa Coomecipar (Solidaridad)",
        "Cooperativa Coomecipar (Prestamo)", "Cooperativa Coomecipar (Tarjeta Mastercard)",
        "Cooperativa Coomecipar (Tarjeta Credicard)", "Cooperativa Coomecipar (Tarjeta Cabal)",
        "Cooperativa Coomecipar (Aportes)", "Cooperativa Medalla Milagrosa (Tarjeta Visa)",
        "Cooperativa Medalla Milagrosa (Solidaridad)",
        "Cooperativa Medalla Milagrosa (Tarjeta Mastercard)",
        "Cooperativa Medalla Milagrosa (Tarjeta Credicard)",
        "Cooperativa Medalla Milagrosa (Tarjeta Cabal)",
        "Cooperativa Medalla Milagrosa (Creditos)"
    )
    
    // File names
    const val USER_DATA_FILE = "user_data.txt"
    const val PRINT_HISTORY_FILE = "print_history.json"
    
    // Preferences
    const val PREFS_NAME = "GenioTecniPrefs"
    const val BLUETOOTH_PREFS = "MyPrefs"
    
    // Bluetooth
    const val BLUETOOTH_UUID = "00001101-0000-1000-8000-00805F9B34FB"
    
    // Formats
    const val DATE_FORMAT = "dd-MM-yyyy"
    const val TIME_FORMAT = "HH:mm:ss"
    const val DECIMAL_FORMAT = "#,###"
    
    // Business logic
    const val COMMISSION_RATE = 0.06f
    const val MIN_PHONE_LENGTH = 10
    const val MIN_CEDULA_LENGTH = 5
    const val MIN_AMOUNT_PERSONAL = 1000
}