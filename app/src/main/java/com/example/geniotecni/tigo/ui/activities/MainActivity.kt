package com.example.geniotecni.tigo.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.telecom.TelecomManager
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.geniotecni.tigo.R
import com.example.geniotecni.tigo.managers.*
import com.example.geniotecni.tigo.models.PrintData
import com.example.geniotecni.tigo.models.ReferenceData
import com.example.geniotecni.tigo.models.ServiceConfig
import com.example.geniotecni.tigo.ui.adapters.ServiceItem
import com.example.geniotecni.tigo.utils.Constants
import com.example.geniotecni.tigo.utils.AppLogger
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.*
import java.io.IOException
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
        private const val BLUETOOTH_UUID = "00001101-0000-1000-8000-00805F9B34FB"
        private const val DECIMAL_FORMAT_PATTERN = "#,###"
        private const val DATE_FORMAT = "dd-MM-yyyy"
        private const val TIME_FORMAT = "HH:mm:ss"
        private const val COMMISSION_RATE = 0.01f // 1% commission
        private const val TAG = "MainActivity"
    }

    // Properties
    private lateinit var printDataManager: PrintDataManager
    private lateinit var printCooldownManager: PrintCooldownManager
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var editModeManager: EditModeManager

    // Layout components
    private lateinit var coordinatorLayout: CoordinatorLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var serviceContainer: MaterialCardView
    private lateinit var serviceImage: ImageView
    private lateinit var serviceTitle: TextView

    // Input layouts
    private lateinit var phoneInputLayout: TextInputLayout
    private lateinit var phoneInput: TextInputEditText
    private lateinit var phoneCounterText: TextView
    private lateinit var cedulaInputLayout: TextInputLayout
    private lateinit var cedulaInput: TextInputEditText
    private lateinit var amountInputLayout: TextInputLayout
    private lateinit var amountInput: TextInputEditText
    private lateinit var dateInputLayout: TextInputLayout
    private lateinit var dateInput: TextInputEditText

    // Amount controls container (removed - no longer needed)

    private var currentServiceType = 0
    private lateinit var currentService: ServiceItem
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var lastSmsTimestamp = 0L
    private var smsObserver: ContentObserver? = null
    private var searchJob: Job? = null
    private var isSearching = false
    private var referenceSearchType = ""

    // Service configurations map - matches Constants.SERVICE_NAMES
    private val serviceConfigs = mapOf(
        // Giros Tigo (0)
        0 to ServiceConfig(
            showPhone = true,
            showCedula = true,
            showAmount = true,
            phoneHint = "Número de teléfono",
            cedulaHint = "Número de cédula",
            amountHint = "Monto a enviar"
        ),
        // Retiros Tigo (1)
        1 to ServiceConfig(
            showPhone = true,
            showCedula = true,
            showAmount = true,
            phoneHint = "Número de teléfono",
            cedulaHint = "Número de cédula",
            amountHint = "Monto a retirar"
        ),
        // Carga Billetera Tigo (2)
        2 to ServiceConfig(
            showPhone = true,
            showCedula = true,
            showAmount = true,
            phoneHint = "Número de teléfono",
            cedulaHint = "Número de cédula",
            amountHint = "Monto a cargar"
        ),
        // Telefonia Tigo (3)
        3 to ServiceConfig(
            showPhone = true,
            showCedula = false,
            showAmount = false,
            phoneHint = "Número de teléfono"
        ),
        // Pago TV e Internet Hogar (4)
        4 to ServiceConfig(
            showPhone = false,
            showCedula = true,
            showAmount = false,
            cedulaHint = "Número de cuenta"
        ),
        // Antena (Wimax) (5)
        5 to ServiceConfig(
            showPhone = false,
            showCedula = true,
            showAmount = false,
            cedulaHint = "Número de cuenta"
        ),
        // Tigo TV anticipado (6)
        6 to ServiceConfig(
            showPhone = false,
            showCedula = true,
            showAmount = false,
            cedulaHint = "Número de cliente"
        ),
        // Reseteo de Cliente (7)
        7 to ServiceConfig(
            showPhone = true,
            showCedula = true,
            showAmount = false,
            showNacimiento = true,
            phoneHint = "Número de teléfono",
            cedulaHint = "Número de cédula",
            nacimientoHint = "Fecha de nacimiento"
        ),
        // ANDE (8)
        8 to ServiceConfig(
            showPhone = false,
            showCedula = true,
            showAmount = false,
            cedulaHint = "Número de NIS"
        ),
        // ESSAP (9)
        9 to ServiceConfig(
            showPhone = false,
            showCedula = true,
            showAmount = false,
            cedulaHint = "Número de ISSAN"
        ),
        // COPACO (10)
        10 to ServiceConfig(
            showPhone = false,
            showCedula = true,
            showAmount = false,
            cedulaHint = "Teléfono o Cuenta"
        ),
        // Retiros Personal (11)
        11 to ServiceConfig(
            showPhone = true,
            showCedula = true,
            showAmount = true,
            phoneHint = "Número de teléfono",
            cedulaHint = "Número de cédula",
            amountHint = "Monto a retirar"
        ),
        // Telefonia Personal (12)
        12 to ServiceConfig(
            showPhone = true,
            showCedula = false,
            showAmount = true,
            phoneHint = "Número de teléfono",
            amountHint = "Monto a pagar"
        )
        // Se pueden agregar más configuraciones según sea necesario
        // Para servicios no configurados, se usará la configuración por defecto
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.i(TAG, "=== INICIANDO MAIN ACTIVITY ===")
        AppLogger.logMemoryUsage(TAG, "MainActivity onCreate inicio")
        AppLogger.i(TAG, "Extras recibidos: ${intent.extras}")
        
        try {
        val startTime = System.currentTimeMillis()
        AppLogger.i(TAG, "Cargando layout service")
        setContentView(R.layout.service)
        AppLogger.i(TAG, "Layout cargado en ${System.currentTimeMillis() - startTime}ms")

        setupWindowInsets()
        initializeViews()
        initializeManagers()
        setupListeners()
        setupPermissions()

        // Load service from intent
        val serviceItem = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("selectedService", ServiceItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("selectedService")
        }

        serviceItem?.let {
            AppLogger.logServiceSelection(TAG, it.name, it.serviceType)
            AppLogger.i(TAG, "Icono del servicio: ${it.icon}")
            currentService = it
            currentServiceType = it.serviceType
            updateServiceConfiguration(it.serviceType)
        } ?: run {
            AppLogger.w(TAG, "No se recibió servicio, usando configuración por defecto")
            currentServiceType = 0
            updateServiceConfiguration(0)
        }
        
        AppLogger.i(TAG, "=== MAIN ACTIVITY INICIALIZADA CORRECTAMENTE ===")
        AppLogger.logMemoryUsage(TAG, "MainActivity onCreate final")
        
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error crítico en MainActivity onCreate", e)
            throw e
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.coordinatorLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        // Enable edge-to-edge display
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
    }

    private fun initializeViews() {
        AppLogger.i(TAG, "Inicializando vistas de MainActivity")
        val startTime = System.currentTimeMillis()
        
        // Main layout
        coordinatorLayout = findViewById(R.id.coordinatorLayout)
        AppLogger.d(TAG, "CoordinatorLayout inicializado")
        toolbar = findViewById(R.id.toolbar)
        AppLogger.d(TAG, "Toolbar inicializado")
        serviceContainer = findViewById(R.id.serviceContainer)

        // Service header
        serviceImage = findViewById(R.id.serviceImage)
        serviceTitle = findViewById(R.id.serviceTitle)

        // Input layouts
        phoneInputLayout = findViewById(R.id.phoneInputLayout)
        phoneInput = findViewById(R.id.phoneInput)
        phoneCounterText = findViewById(R.id.phoneCounterText)
        cedulaInputLayout = findViewById(R.id.cedulaInputLayout)
        cedulaInput = findViewById(R.id.cedulaInput)
        amountInputLayout = findViewById(R.id.amountInputLayout)
        amountInput = findViewById(R.id.amountInput)
        dateInputLayout = findViewById(R.id.dateInputLayout)
        dateInput = findViewById(R.id.dateInput)

        // Amount controls (removed - no longer needed)

        // Setup toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { 
            AppLogger.logUserAction(TAG, "Navegación atrás", "Desde MainActivity")
            onBackPressed() 
        }
        
        val initTime = System.currentTimeMillis() - startTime
        AppLogger.i(TAG, "Vistas inicializadas en ${initTime}ms")
    }

    private fun initializeManagers() {
        AppLogger.i(TAG, "Inicializando managers")
        val startTime = System.currentTimeMillis()
        
        preferencesManager = PreferencesManager(this)
        AppLogger.d(TAG, "PreferencesManager inicializado")
        printDataManager = PrintDataManager(this)
        AppLogger.d(TAG, "PrintDataManager inicializado")
        printCooldownManager = PrintCooldownManager(this)
        AppLogger.d(TAG, "PrintCooldownManager inicializado")
        editModeManager = EditModeManager(this, this, coordinatorLayout, preferencesManager)
        AppLogger.d(TAG, "EditModeManager inicializado")

        // Initialize edit mode manager
        editModeManager.initializeEditMode()
        AppLogger.d(TAG, "EditMode inicializado")
        
        val initTime = System.currentTimeMillis() - startTime
        AppLogger.i(TAG, "Managers inicializados en ${initTime}ms")
    }

    private fun setupListeners() {
        // Button listeners removed - buttons no longer exist in layout

        // Set up phone number formatting
        phoneInput.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return

                s?.let {
                    val unformatted = it.toString().replace("-", "")
                    if (unformatted.isNotEmpty() && !unformatted.startsWith("09")) {
                        isFormatting = true
                        phoneInput.setText("09")
                        phoneInput.setSelection(phoneInput.text?.length ?: 0)
                        isFormatting = false
                    } else if (unformatted.length >= 4) {
                        isFormatting = true
                        val formatted = formatPhoneNumber(unformatted)
                        phoneInput.setText(formatted)
                        phoneInput.setSelection(formatted.length)
                        isFormatting = false
                    }
                    
                    // Update phone counter
                    val currentLength = it.toString().replace("-", "").length
                    phoneCounterText.text = "$currentLength/12"
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Amount formatting
        amountInput.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return

                s?.let {
                    val unformatted = it.toString().replace(",", "")
                    if (unformatted.isNotEmpty()) {
                        try {
                            val amount = unformatted.toLong()
                            isFormatting = true
                            val formatted = DecimalFormat(DECIMAL_FORMAT_PATTERN).format(amount)
                            amountInput.setText(formatted)
                            amountInput.setSelection(formatted.length)
                            isFormatting = false
                        } catch (e: NumberFormatException) {
                            // Invalid input
                        }
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Date formatting for birth date
        dateInput.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            private var previousLength = 0

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return

                s?.let {
                    val text = it.toString()
                    val length = text.length

                    isFormatting = true

                    // Auto-add slashes
                    when {
                        length == 2 && previousLength < length -> {
                            dateInput.setText("$text/")
                            dateInput.setSelection(3)
                        }
                        length == 5 && previousLength < length -> {
                            dateInput.setText("$text/")
                            dateInput.setSelection(6)
                        }
                    }

                    isFormatting = false
                    previousLength = dateInput.text?.length ?: 0
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                previousLength = s?.length ?: 0
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun formatPhoneNumber(number: String): String {
        return when {
            number.length <= 4 -> number
            number.length <= 7 -> "${number.substring(0, 4)}-${number.substring(4)}"
            else -> "${number.substring(0, 4)}-${number.substring(4, 7)}-${number.substring(7, minOf(number.length, 10))}"
        }
    }

    private fun updateServiceConfiguration(serviceType: Int) {
        currentServiceType = serviceType
        
        // Get configuration or use default
        val config = serviceConfigs[serviceType] ?: ServiceConfig(
            showPhone = true,
            showCedula = true,
            showAmount = false,
            phoneHint = "Número de teléfono",
            cedulaHint = "Número de cédula"
        )

        // Update service title and image based on current service item if available
        if (::currentService.isInitialized) {
            serviceImage.setImageResource(currentService.icon)
            serviceTitle.text = currentService.name
            toolbar.title = currentService.name
        } else {
            // Fallback to service names from Constants
            val serviceName = if (serviceType < Constants.SERVICE_NAMES.size) {
                Constants.SERVICE_NAMES[serviceType]
            } else {
                "Servicio"
            }
            
            serviceTitle.text = serviceName
            toolbar.title = serviceName
            
            // Set default image based on service type
            serviceImage.setImageResource(when {
                serviceName.contains("Tigo", ignoreCase = true) -> R.drawable.tigo_cuadrado
                serviceName.contains("ANDE", ignoreCase = true) -> R.drawable.ande_icon
                serviceName.contains("ESSAP", ignoreCase = true) -> R.drawable.essap_icon
                serviceName.contains("COPACO", ignoreCase = true) -> R.drawable.cocapo_icon
                serviceName.contains("Personal", ignoreCase = true) -> R.drawable.personal_logo
                else -> R.drawable.ic_service_default
            })
        }

        // Update visibility of input fields
        phoneInputLayout.visibility = if (config.showPhone) View.VISIBLE else View.GONE
        cedulaInputLayout.visibility = if (config.showCedula) View.VISIBLE else View.GONE
        amountInputLayout.visibility = if (config.showAmount) View.VISIBLE else View.GONE
        // Amount controls removed - no longer needed

        // Handle date input for birth date
        dateInputLayout.visibility = if (config.showNacimiento) View.VISIBLE else View.GONE

        // Update hints
        phoneInputLayout.hint = config.phoneHint
        cedulaInputLayout.hint = config.cedulaHint
        amountInputLayout.hint = config.amountHint
        dateInputLayout.hint = config.nacimientoHint

        // Update edit mode manager with current service
        editModeManager.setCurrentServiceType(serviceType)

        // Clear inputs when switching services
        clearAllInputs()
    }

    private fun clearAllInputs() {
        phoneInput.setText("")
        cedulaInput.setText("")
        amountInput.setText("")
        dateInput.setText("")

        // Clear errors
        phoneInputLayout.error = null
        cedulaInputLayout.error = null
        amountInputLayout.error = null
        dateInputLayout.error = null
    }

    // adjustAmount method removed - no longer needed

    private fun processTransaction() {
        AppLogger.i(TAG, "Iniciando procesamiento de transacción")
        if (!validateInputs()) {
            AppLogger.w(TAG, "Transacción cancelada - Validación fallida")
            return
        }

        // Check print cooldown
        val printStatus = printCooldownManager.canPrint()
        AppLogger.d(TAG, "Estado cooldown impresión: ${printStatus.canPrint}")
        if (!printStatus.canPrint) {
            AppLogger.w(TAG, "Transacción bloqueada por cooldown: ${printStatus.message}")
            showSnackbar(printStatus.message)
            return
        }

        // Check Bluetooth device
        if (!checkBluetoothDevice()) {
            AppLogger.w(TAG, "Transacción cancelada - Sin dispositivo Bluetooth")
            return
        }

        // Show processing dialog
        AppLogger.i(TAG, "Mostrando diálogo de procesamiento")
        showProcessingDialog()

        // Process the transaction
        CoroutineScope(Dispatchers.Main).launch {
            try {
                AppLogger.i(TAG, "Simulando procesamiento de transacción...")
                val startTime = System.currentTimeMillis()
                delay(1500) // Simulate processing

                val printData = createPrintData()
                AppLogger.logDataProcessing(TAG, "Crear datos de impresión", "PrintData", 1, System.currentTimeMillis() - startTime)
                printDataManager.savePrintData(printData)
                AppLogger.logFileOperation(TAG, "Guardar", "PrintData", true)
                printCooldownManager.recordPrint()
                AppLogger.d(TAG, "Cooldown de impresión registrado")

                // Print if auto print is enabled
                if (preferencesManager.autoPrint) {
                    AppLogger.i(TAG, "Impresión automática habilitada - Iniciando impresión")
                    printTransaction(printData)
                } else {
                    AppLogger.d(TAG, "Impresión automática deshabilitada")
                }

                hideProcessingDialog()
                AppLogger.i(TAG, "Transacción procesada exitosamente")
                showSuccessDialog(printData)

                // Clear inputs after successful transaction
                clearAllInputs()
                AppLogger.d(TAG, "Campos limpiados después de transacción exitosa")

            } catch (e: Exception) {
                hideProcessingDialog()
                AppLogger.e(TAG, "Error crítico en procesamiento de transacción", e)
                showSnackbar("Error al procesar la transacción: ${e.message}")
            }
        }
    }

    private fun validateInputs(): Boolean {
        val config = serviceConfigs[currentServiceType] ?: return false
        var isValid = true

        // Validate phone
        if (config.showPhone) {
            val phone = phoneInput.text.toString().replace("-", "")
            when {
                phone.isEmpty() -> {
                    phoneInputLayout.error = "Ingrese el número de teléfono"
                    isValid = false
                }
                phone.length != 10 -> {
                    phoneInputLayout.error = "El número debe tener 10 dígitos"
                    isValid = false
                }
                !phone.startsWith("09") -> {
                    phoneInputLayout.error = "El número debe comenzar con 09"
                    isValid = false
                }
                else -> phoneInputLayout.error = null
            }
        }

        // Validate cedula
        if (config.showCedula) {
            val cedula = cedulaInput.text.toString()
            when {
                cedula.isEmpty() -> {
                    cedulaInputLayout.error = "Ingrese el número de cédula"
                    isValid = false
                }
                cedula.length < 5 -> {
                    cedulaInputLayout.error = "Número de cédula inválido"
                    isValid = false
                }
                else -> cedulaInputLayout.error = null
            }
        }

        // Validate amount
        if (config.showAmount) {
            val amountText = amountInput.text.toString().replace(",", "")
            val amount = amountText.toLongOrNull() ?: 0L
            when {
                amountText.isEmpty() -> {
                    amountInputLayout.error = "Ingrese el monto"
                    isValid = false
                }
                amount < 1000 -> {
                    amountInputLayout.error = "El monto mínimo es 1,000 Gs."
                    isValid = false
                }
                else -> amountInputLayout.error = null
            }
        }

        // Validate birth date
        if (config.showNacimiento) {
            val date = dateInput.text.toString()
            when {
                date.isEmpty() -> {
                    dateInputLayout.error = "Ingrese la fecha de nacimiento"
                    isValid = false
                }
                !isValidDate(date) -> {
                    dateInputLayout.error = "Formato de fecha inválido (dd/mm/yyyy)"
                    isValid = false
                }
                else -> dateInputLayout.error = null
            }
        }

        return isValid
    }

    private fun isValidDate(date: String): Boolean {
        return try {
            val parts = date.split("/")
            if (parts.size != 3) return false

            val day = parts[0].toInt()
            val month = parts[1].toInt()
            val year = parts[2].toInt()

            day in 1..31 && month in 1..12 && year in 1900..2100
        } catch (e: Exception) {
            false
        }
    }

    private fun checkBluetoothDevice(): Boolean {
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val deviceAddress = sharedPreferences.getString("BluetoothDeviceAddress", null)

        if (deviceAddress == null) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Dispositivo Bluetooth")
                .setMessage("No se ha configurado ningún dispositivo Bluetooth. ¿Desea configurar uno ahora?")
                .setPositiveButton("Configurar") { _, _ ->
                    startActivity(Intent(this, Bt::class.java))
                }
                .setNegativeButton("Cancelar", null)
                .show()
            return false
        }

        AppLogger.i(TAG, "Dispositivo Bluetooth configurado correctamente")
        return true
    }

    private var processingDialog: Dialog? = null

    private fun showProcessingDialog() {
        processingDialog = Dialog(this, R.style.Theme_Material3_DayNight_Dialog).apply {
            setContentView(R.layout.dialog_processing)
            setCancelable(false)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            show()
        }
    }

    private fun hideProcessingDialog() {
        processingDialog?.dismiss()
        processingDialog = null
    }

    private fun showSuccessDialog(printData: PrintData) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Transacción Exitosa")
            .setMessage("La transacción se ha procesado correctamente.\n\n${printData.message}")
            .setPositiveButton("Aceptar", null)
            .setNeutralButton("Imprimir") { _, _ ->
                printTransaction(printData)
            }
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun printTransaction(printData: PrintData) {
        AppLogger.logPrintEvent(TAG, "Iniciar impresión", "Preparando datos")
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val deviceAddress = sharedPreferences.getString("BluetoothDeviceAddress", null) 
        if (deviceAddress == null) {
            AppLogger.logPrintEvent(TAG, "Error", "No hay dispositivo configurado", false)
            return
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        AppLogger.logBluetoothEvent(TAG, "Obtener adaptador Bluetooth", deviceAddress)
        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
        if (device == null) {
            AppLogger.logBluetoothEvent(TAG, "Error obteniendo dispositivo", deviceAddress, false)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppLogger.logBluetoothEvent(TAG, "Intentando conectar", device.name ?: device.address)
                val uuid = UUID.fromString(BLUETOOTH_UUID)
                val socket = device.createRfcommSocketToServiceRecord(uuid)
                val connectStart = System.currentTimeMillis()
                socket.connect()
                AppLogger.logBluetoothEvent(TAG, "Conectado", device.name ?: device.address)
                AppLogger.i(TAG, "Conexión establecida en ${System.currentTimeMillis() - connectStart}ms")

                val outputStream = socket.outputStream
                AppLogger.logPrintEvent(TAG, "Enviando datos", "Comandos de impresión")

                // Print commands
                val alignCenterCommand = byteArrayOf(0x1B, 0x61, 0x01)
                val largeFontCommand = byteArrayOf(0x1D, 0x21, 0x11)
                val normalFontCommand = byteArrayOf(0x1B, 0x21, 0x00)

                outputStream.write(alignCenterCommand)
                outputStream.write(largeFontCommand)
                outputStream.write("GENIO TECNI\n".toByteArray())
                outputStream.write(normalFontCommand)
                outputStream.write("\n".toByteArray())
                outputStream.write(printData.message.toByteArray())
                outputStream.write("\n\n\n".toByteArray())
                outputStream.flush()
                AppLogger.logPrintEvent(TAG, "Datos enviados", "${printData.message.length} caracteres")

                outputStream.close()
                socket.close()
                AppLogger.logBluetoothEvent(TAG, "Conexión cerrada", device.name ?: device.address)

                withContext(Dispatchers.Main) {
                    AppLogger.logPrintEvent(TAG, "Completada", "Impresión exitosa")
                    showSnackbar("Impresión completada")
                }

            } catch (e: IOException) {
                AppLogger.logPrintEvent(TAG, "Error de E/S", e.message ?: "Error desconocido", false)
                AppLogger.logBluetoothEvent(TAG, "Error de conexión", device.name ?: device.address, false)
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showSnackbar("Error al imprimir: ${e.message}")
                }
            }
        }
    }

    private fun createPrintData(): PrintData {
        val config = serviceConfigs[currentServiceType] ?: throw IllegalStateException("Invalid service type")

        val phone = if (config.showPhone) phoneInput.text.toString().replace("-", "") else ""
        val cedula = if (config.showCedula) cedulaInput.text.toString() else ""
        val amountText = if (config.showAmount) amountInput.text.toString().replace(",", "") else "0"
        val amount = amountText.toLongOrNull() ?: 0L
        val birthDate = if (config.showNacimiento) dateInput.text.toString() else ""

        val serviceName = when (currentServiceType) {
            0 -> "Giros Tigo"
            1 -> "ANDE"
            2 -> "Reseteo de Cliente"
            3 -> "Telefonía Tigo"
            else -> "Servicio Desconocido"
        }

        val currentTime = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        val timeFormat = SimpleDateFormat(TIME_FORMAT, Locale.getDefault())
        val currentDate = Date(currentTime)

        val message = buildString {
            append("Servicio: $serviceName\n")
            append("Fecha: ${dateFormat.format(currentDate)}\n")
            append("Hora: ${timeFormat.format(currentDate)}\n")
            append("------------------------\n")
            if (phone.isNotEmpty()) append("Teléfono: ${formatPhoneNumber(phone)}\n")
            if (cedula.isNotEmpty()) append("Cédula: $cedula\n")
            if (amount > 0) {
                append("Monto: ${DecimalFormat(DECIMAL_FORMAT_PATTERN).format(amount)} Gs.\n")
                val commission = (amount * COMMISSION_RATE).toLong()
                append("Comisión: ${DecimalFormat(DECIMAL_FORMAT_PATTERN).format(commission)} Gs.\n")
            }
            if (birthDate.isNotEmpty()) append("Fecha Nacimiento: $birthDate\n")
            append("------------------------\n")
            append("Ref: ${generateReference()}")
        }

        return PrintData(
            service = serviceName,
            date = dateFormat.format(currentDate),
            time = timeFormat.format(currentDate),
            message = message,
            referenceData = ReferenceData(
                ref1 = if (phone.isNotEmpty()) phone else cedula,
                ref2 = if (amount > 0) amount.toString() else birthDate
            )
        )
    }

    private fun generateReference(): String {
        return "GT${System.currentTimeMillis().toString().takeLast(8)}"
    }

    private fun showServiceInfo() {
        val info = when (currentServiceType) {
            0 -> """
                |Giros Tigo
                |
                |Permite enviar dinero de forma rápida y segura a cualquier número Tigo.
                |
                |Requisitos:
                |• Número de teléfono del destinatario
                |• Cédula del destinatario
                |• Monto a enviar (mínimo 1,000 Gs.)
                |
                |Comisión: 1% del monto enviado
            """.trimMargin()

            1 -> """
                |ANDE - Pago de Energía Eléctrica
                |
                |Paga tu factura de ANDE de manera fácil y rápida.
                |
                |Requisitos:
                |• Número de NIS (Número de Identificación del Suministro)
                |
                |El monto será consultado automáticamente.
            """.trimMargin()

            2 -> """
                |Reseteo de Cliente
                |
                |Restablece la información del cliente en el sistema.
                |
                |Requisitos:
                |• Número de teléfono del cliente
                |• Cédula del cliente
                |• Fecha de nacimiento
                |
                |Este proceso no tiene costo.
            """.trimMargin()

            3 -> """
                |Telefonía Tigo
                |
                |Gestiona servicios de telefonía Tigo.
                |
                |Requisitos:
                |• Número de teléfono
                |
                |Servicios disponibles: consulta de saldo, recargas, paquetes.
            """.trimMargin()

            else -> "Información del servicio no disponible"
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Información del Servicio")
            .setMessage(info)
            .setPositiveButton("Entendido", null)
            .show()
    }

    private fun setupPermissions() {
        val permissions = arrayOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.plus(arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            ))
        }

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // Let edit mode manager handle touch events if in edit mode
        if (editModeManager.handleTouchEvent(event)) {
            return true
        }

        // Otherwise, let normal touch events proceed
        return super.dispatchTouchEvent(event)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                val deniedPermissions = permissions.filterIndexed { index, _ ->
                    grantResults[index] != PackageManager.PERMISSION_GRANTED
                }

                if (deniedPermissions.isNotEmpty()) {
                    showSnackbar("Algunos permisos fueron denegados. La aplicación puede no funcionar correctamente.")
                }
            }
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_SHORT).show()
    }

    // USSD and SMS Integration Methods
    private fun hasUSSDSupport(serviceType: Int): Boolean {
        return when (serviceType) {
            0, 1, 2, 3, 7, 8, 9, 10, 11, 12 -> true // Services with USSD support
            else -> false
        }
    }

    private fun makeCallWithSIM(phoneNumber: String, simSlot: Int = 0) {
        AppLogger.i(TAG, "Iniciando llamada USSD: $phoneNumber (SIM $simSlot)")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            AppLogger.w(TAG, "Permiso CALL_PHONE no concedido - Solicitando")
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), PERMISSION_REQUEST_CODE)
            return
        }

        try {
            val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
            val phoneAccountHandles = telecomManager.callCapablePhoneAccounts

            if (phoneAccountHandles.isNotEmpty()) {
                val selectedSim = if (simSlot < phoneAccountHandles.size) {
                    phoneAccountHandles[simSlot]
                } else {
                    phoneAccountHandles[0] // Default to first SIM
                }

                val callIntent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:${Uri.encode(phoneNumber)}")
                    putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, selectedSim)
                }

                startActivity(callIntent)
                AppLogger.logNetworkEvent(TAG, "Llamada USSD iniciada", phoneNumber)
                showSnackbar("Iniciando llamada USSD...")
                
                // Start SMS monitoring
                startSMSSearch()
                AppLogger.i(TAG, "Monitoreo SMS iniciado")
            } else {
                AppLogger.w(TAG, "No se encontraron SIMs disponibles")
                showSnackbar("No se encontraron SIMs disponibles")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error crítico en llamada USSD", e)
            showSnackbar("Error al realizar la llamada: ${e.message}")
        }
    }

    private fun processUSSDForService() {
        val phone = phoneInput.text.toString().replace("-", "")
        val cedula = cedulaInput.text.toString()
        val amount = amountInput.text.toString().replace(",", "")
        val birthDate = dateInput.text.toString()

        when (currentServiceType) {
            0 -> { // Giros Tigo
                if (phone.length == 10 && cedula.length >= 5 && amount.isNotEmpty()) {
                    val ussdCode = "*555*1*$phone*$cedula*1*$amount#"
                    makeCallWithSIM(ussdCode)
                    copyToClipboard(amount)
                    copyToClipboard(cedula)
                    copyToClipboard(phone)
                    startReferenceSearch("Giros")
                } else {
                    showSnackbar("Complete todos los datos requeridos")
                }
            }
            1 -> { // Retiros Tigo
                if (phone.length == 10 && cedula.length >= 5 && amount.isNotEmpty()) {
                    val ussdCode = "*555*2*$phone*$cedula*1*$amount#"
                    makeCallWithSIM(ussdCode)
                    copyToClipboard(amount)
                    copyToClipboard(cedula)
                    copyToClipboard(phone)
                    startReferenceSearch("Retiros")
                } else {
                    showSnackbar("Complete todos los datos requeridos")
                }
            }
            2 -> { // Carga Billetera Tigo
                if (phone.length == 10 && cedula.length >= 5 && amount.isNotEmpty()) {
                    val ussdCode = "*555*3*1*$cedula*1*$phone*$amount#"
                    makeCallWithSIM(ussdCode)
                    copyToClipboard(amount)
                    copyToClipboard(cedula)
                    copyToClipboard(phone)
                    startReferenceSearch("Billetera")
                } else {
                    showSnackbar("Complete todos los datos requeridos")
                }
            }
            3 -> { // Telefonia Tigo
                if (phone.length == 10) {
                    val ussdCode = "*555*5*1*1*1*$phone*$phone#"
                    makeCallWithSIM(ussdCode)
                    copyToClipboard(phone)
                    startReferenceSearch("Servicio")
                } else {
                    showSnackbar("Ingrese un número de teléfono válido")
                }
            }
            7 -> { // Reseteo de Cliente
                if (phone.length == 10 && cedula.length >= 5 && birthDate.length >= 8) {
                    val ussdCode = "*555*6*3*$phone*1*$cedula*$birthDate#"
                    makeCallWithSIM(ussdCode)
                    copyToClipboard(phone)
                    startReferenceSearch("Reseteo")
                } else {
                    showSnackbar("Complete todos los datos requeridos")
                }
            }
            8 -> { // ANDE
                if (cedula.isNotEmpty()) {
                    val ussdCode = "*222*1*2*$cedula#"
                    makeCallWithSIM(ussdCode)
                    copyToClipboard(cedula)
                    startReferenceSearch("ANDE")
                } else {
                    showSnackbar("Ingrese el número de NIS")
                }
            }
            9 -> { // ESSAP
                if (cedula.isNotEmpty()) {
                    val ussdCode = "*222*2*1*$cedula#"
                    makeCallWithSIM(ussdCode)
                    copyToClipboard(cedula)
                    startReferenceSearch("ESSAP")
                } else {
                    showSnackbar("Ingrese el número de ISSAN")
                }
            }
            10 -> { // COPACO
                if (cedula.isNotEmpty()) {
                    val ussdCode = "*222*3*1*$cedula#"
                    makeCallWithSIM(ussdCode)
                    copyToClipboard(cedula)
                    startReferenceSearch("COPACO")
                } else {
                    showSnackbar("Ingrese el teléfono o cuenta")
                }
            }
            11 -> { // Retiros Personal
                if (phone.length == 10 && amount.isNotEmpty()) {
                    val ussdCode = "*200*2*$phone*$amount#"
                    makeCallWithSIM(ussdCode, 1) // SIM 2 for Personal
                    copyToClipboard(amount)
                    copyToClipboard(phone)
                    startReferenceSearch("Retiros Personal")
                } else {
                    showSnackbar("Complete todos los datos requeridos")
                }
            }
            12 -> { // Telefonia Personal
                if (phone.length == 10 && amount.isNotEmpty()) {
                    val ussdCode = "*200*4*$phone*$amount#"
                    makeCallWithSIM(ussdCode, 1) // SIM 2 for Personal
                    copyToClipboard(amount)
                    copyToClipboard(phone)
                    startReferenceSearch("Telefonia Personal")
                } else {
                    showSnackbar("Complete todos los datos requeridos")
                }
            }
            else -> {
                showSnackbar("Servicio no configurado para USSD")
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Genio Tigo", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun startReferenceSearch(serviceType: String) {
        referenceSearchType = serviceType
        isSearching = true
        lastSmsTimestamp = System.currentTimeMillis()
        
        showSnackbar("Búsqueda de referencias iniciada")

        // Setup SMS observer
        smsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                searchInMessages(serviceType)
            }
        }

        contentResolver.registerContentObserver(
            Telephony.Sms.CONTENT_URI,
            true,
            smsObserver!!
        )

        // Initial search
        searchInMessages(serviceType)

        // Auto-cancel after 5 minutes
        Handler(Looper.getMainLooper()).postDelayed({
            if (isSearching) {
                cancelSMSSearch()
                showSnackbar("Búsqueda finalizada automáticamente")
            }
        }, 300000) // 5 minutes
    }

    private fun startSMSSearch() {
        // This is called when USSD is dialed to prepare for response
        lastSmsTimestamp = System.currentTimeMillis()
    }

    private fun searchInMessages(refType: String) {
        if (!isSearching) return

        searchJob?.cancel()
        searchJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val smsUri: Uri = Telephony.Sms.Inbox.CONTENT_URI
                val selection = "${Telephony.Sms.DATE} > ? AND ${Telephony.Sms.ADDRESS} IN (?, ?, ?, ?)"
                val selectionArgs = arrayOf(
                    lastSmsTimestamp.toString(),
                    "555", "55", "200", "222"
                )

                val cursor: Cursor? = contentResolver.query(
                    smsUri,
                    arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.ADDRESS),
                    selection,
                    selectionArgs,
                    "${Telephony.Sms.DATE} DESC"
                )

                cursor?.use {
                    while (it.moveToNext()) {
                        val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
                        val date = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                        val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))

                        Log.d(TAG, "SMS encontrado: $body de $address en $date")

                        val referenceData = extractReferenceData(body, refType)
                        if (referenceData != null) {
                            withContext(Dispatchers.Main) {
                                handleReferenceFound(referenceData, body)
                                cancelSMSSearch()
                            }
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching SMS", e)
                withContext(Dispatchers.Main) {
                    showSnackbar("Error al buscar SMS: ${e.message}")
                }
            }
        }
    }

    private fun extractReferenceData(smsBody: String, refType: String): ReferenceData? {
        return try {
            when (refType) {
                "Giros", "Retiros", "Billetera" -> {
                    val ref1Regex = Regex("""Ref 1: (\d+)""")
                    val ref2Regex = Regex("""Ref 2: (\d+)""")
                    
                    val ref1Match = ref1Regex.find(smsBody)
                    val ref2Match = ref2Regex.find(smsBody)
                    
                    if (ref1Match != null && ref2Match != null) {
                        ReferenceData(ref1Match.groupValues[1], ref2Match.groupValues[1])
                    } else {
                        // Try alternative patterns
                        val montoYRefRegex = Regex("""Monto PYG ([\d.,]+)[^R]*Ref[ .:]+(\d+)""")
                        val montoMatch = montoYRefRegex.find(smsBody)
                        if (montoMatch != null) {
                            ReferenceData("", montoMatch.groupValues[2])
                        } else null
                    }
                }
                "Retiros Personal", "Telefonia Personal" -> {
                    val comprobanteRegex = Regex("""Su comprobante es (\d+)""")
                    val match = comprobanteRegex.find(smsBody)
                    match?.let { ReferenceData(it.groupValues[1], "") }
                }
                "ANDE", "ESSAP", "COPACO" -> {
                    val codigoRegex = Regex("""Codigo de referencia: (\d+)""")
                    val match = codigoRegex.find(smsBody)
                    match?.let { ReferenceData(it.groupValues[1], "") }
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting reference data", e)
            null
        }
    }

    private fun handleReferenceFound(referenceData: ReferenceData, smsBody: String) {
        // Update the print data with actual references
        val printData = createPrintData().copy(
            referenceData = referenceData,
            message = "Transacción completada exitosamente"
        )

        // Save and potentially print
        printDataManager.savePrintData(printData)
        
        // Show success dialog with references
        showReferenceDialog(referenceData, smsBody)
        
        showSnackbar("Referencias encontradas: ${referenceData.ref1}")
    }

    private fun showReferenceDialog(referenceData: ReferenceData, smsBody: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Referencias Encontradas")
            .setMessage("""
                |Transacción completada exitosamente
                |
                |Referencia 1: ${referenceData.ref1}
                |${if (referenceData.ref2.isNotEmpty()) "Referencia 2: ${referenceData.ref2}" else ""}
                |
                |Mensaje completo:
                |$smsBody
            """.trimMargin())
            .setPositiveButton("Imprimir Recibo") { _, _ ->
                val printData = createPrintData().copy(referenceData = referenceData)
                printTransaction(printData)
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun cancelSMSSearch() {
        isSearching = false
        smsObserver?.let {
            contentResolver.unregisterContentObserver(it)
            smsObserver = null
        }
        searchJob?.cancel()
        searchJob = null
    }

    private fun formatPrintDataWithLargeFont(printData: PrintData): ByteArray {
        val output = mutableListOf<Byte>()
        
        // ESC @ - Initialize printer
        output.addAll(byteArrayOf(0x1B, 0x40).toList())
        
        // ESC a 1 - Center alignment
        output.addAll(byteArrayOf(0x1B, 0x61, 0x01).toList())
        
        // GS ! 0x11 - Large font (double width and height)
        output.addAll(byteArrayOf(0x1D, 0x21, 0x11).toList())
        
        // Print header with large font
        val header = "=====================\nGenio Tecni\n${printData.service}\n"
        output.addAll(header.toByteArray().toList())
        
        // Reset font size for details
        output.addAll(byteArrayOf(0x1B, 0x21, 0x00).toList())
        
        // Print details
        val details = buildString {
            append("Fecha: ${printData.date}\n")
            append("Hora: ${printData.time}\n")
            
            if (printData.referenceData.ref1.isNotEmpty()) {
                when (currentServiceType) {
                    0, 1, 2 -> { // Giros, Retiros, Billetera
                        append("Teléfono: ${phoneInput.text}\n")
                        append("CI: ${cedulaInput.text}\n")
                        append("Monto: ${amountInput.text} Gs.\n")
                        append("Ref1: ${printData.referenceData.ref1}\n")
                        if (printData.referenceData.ref2.isNotEmpty()) {
                            append("Ref2: ${printData.referenceData.ref2}\n")
                        }
                    }
                    3 -> { // Telefonía
                        append("Teléfono: ${phoneInput.text}\n")
                        append("Referencia: ${printData.referenceData.ref1}\n")
                    }
                    7 -> { // Reseteo
                        append("Teléfono: ${phoneInput.text}\n")
                        append("CI: ${cedulaInput.text}\n")
                        append("Referencia: ${printData.referenceData.ref1}\n")
                    }
                    8, 9, 10 -> { // ANDE, ESSAP, COPACO
                        append("Cuenta: ${cedulaInput.text}\n")
                        append("Código: ${printData.referenceData.ref1}\n")
                    }
                    11, 12 -> { // Personal
                        append("Teléfono: ${phoneInput.text}\n")
                        append("Monto: ${amountInput.text} Gs.\n")
                        append("Comprobante: ${printData.referenceData.ref1}\n")
                    }
                    else -> {
                        append("Referencia: ${printData.referenceData.ref1}\n")
                    }
                }
            } else {
                // Simulation mode
                append("MODO SIMULACIÓN\n")
                if (phoneInput.text.toString().isNotEmpty()) {
                    append("Teléfono: ${phoneInput.text}\n")
                }
                if (cedulaInput.text.toString().isNotEmpty()) {
                    append("CI/Cuenta: ${cedulaInput.text}\n")
                }
                if (amountInput.text.toString().isNotEmpty()) {
                    append("Monto: ${amountInput.text} Gs.\n")
                }
                append("Ref: ${generateReference()}\n")
            }
        }
        
        output.addAll(details.toByteArray().toList())
        
        // Large font for footer
        output.addAll(byteArrayOf(0x1D, 0x21, 0x11).toList())
        output.addAll("=====================\n".toByteArray().toList())
        
        // Reset font and add line feeds
        output.addAll(byteArrayOf(0x1B, 0x21, 0x00).toList())
        output.addAll("\n\n\n".toByteArray().toList())
        
        return output.toByteArray()
    }

    private fun showPrintErrorDialog(printData: PrintData) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Error de Impresión")
            .setMessage("No se pudo imprimir el recibo. ¿Qué desea hacer?")
            .setPositiveButton("Reintentar") { _, _ ->
                printTransaction(printData)
            }
            .setNegativeButton("Configurar Bluetooth") { _, _ ->
                startActivity(Intent(this, Bt::class.java))
            }
            .setNeutralButton("Cancelar", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelSMSSearch()
    }
}