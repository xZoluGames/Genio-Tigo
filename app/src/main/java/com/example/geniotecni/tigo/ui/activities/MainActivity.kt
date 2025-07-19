package com.example.geniotecni.tigo.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
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
import com.example.geniotecni.tigo.helpers.USSDIntegrationHelper
import com.example.geniotecni.tigo.models.PrintData
import com.example.geniotecni.tigo.models.ReferenceData
import com.example.geniotecni.tigo.models.ServiceConfig
import com.example.geniotecni.tigo.ui.adapters.ServiceItem
import com.example.geniotecni.tigo.utils.Constants
import com.example.geniotecni.tigo.utils.AppLogger
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.*
import java.io.IOException
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
class MainActivity : AppCompatActivity(), USSDIntegrationHelper.USSDCallback {

    companion object {
        private const val BLUETOOTH_UUID = "00001101-0000-1000-8000-00805F9B34FB"
        private const val DECIMAL_FORMAT_PATTERN = "#,###"
        private const val DATE_FORMAT = "dd-MM-yyyy"
        private const val TIME_FORMAT = "HH:mm:ss"
        private const val COMMISSION_RATE = 0.01f // 1% commission
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 101
        private const val REQUEST_ENABLE_BT = 1
    }

    // Properties
    private lateinit var printDataManager: PrintDataManager
    private lateinit var printCooldownManager: PrintCooldownManager
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var editModeManager: EditModeManager
    private lateinit var bluetoothManager: com.example.geniotecni.tigo.managers.BluetoothManager
    private lateinit var ussdIntegrationHelper: com.example.geniotecni.tigo.helpers.USSDIntegrationHelper
    private lateinit var amountUsageManager: AmountUsageManager
    private lateinit var executeUSSDButton: MaterialButton
    private lateinit var manualReferenceButton: MaterialButton
    private lateinit var viewMoreButton: MaterialButton
    private lateinit var quickAmountChipGroup: ChipGroup

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
        setContentView(R.layout.service)
        AppLogger.i(TAG, "=== INICIANDO MAIN ACTIVITY ===")
        AppLogger.logMemoryUsage(TAG, "MainActivity onCreate inicio")
        AppLogger.i(TAG, "Extras recibidos: ${intent.extras}")

        try {
            // Initialize views first
            initializeViews()
            
            // Initialize managers
            preferencesManager = PreferencesManager(this)
            printDataManager = PrintDataManager(this)
            printCooldownManager = PrintCooldownManager(this)
            bluetoothManager = com.example.geniotecni.tigo.managers.BluetoothManager(this)
            ussdIntegrationHelper = USSDIntegrationHelper(this)
            editModeManager = EditModeManager(this, this, coordinatorLayout, preferencesManager)
            amountUsageManager = AmountUsageManager(this)
            setupWindowInsets()

            // Setup views
            setupTextWatchers()
            setupQuickAmountChips()
            setupActionButtons()

            // Load saved values
            loadSavedValues()

            // Get service info from intent
            currentServiceType = intent.getIntExtra("SERVICE_TYPE", 0)
            val serviceItem = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra("SERVICE_ITEM", ServiceItem::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra("SERVICE_ITEM")
            }

            serviceItem?.let {
                currentService = it
                AppLogger.i(TAG, "Servicio recibido: ${it.name} (ID: ${it.id})")
                currentServiceType = it.id
                updateServiceConfiguration(it.id)
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
    private fun showManualServiceDialog(service: String, message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Servicio $service")
            .setMessage("Este servicio no requiere USSD. Los datos han sido preparados para impresión.")
            .setPositiveButton("Imprimir") { _, _ ->
                if (bluetoothManager.isBluetoothEnabled() && bluetoothManager.hasSelectedDevice()) {
                    printData(message, null)
                    savePrintData(service, message, null)
                } else {
                    showSnackbar("Configure una impresora Bluetooth primero")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
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
        cedulaInputLayout = findViewById(R.id.cedulaInputLayout)
        cedulaInput = findViewById(R.id.cedulaInput)
        amountInputLayout = findViewById(R.id.amountInputLayout)
        amountInput = findViewById(R.id.amountInput)
        dateInputLayout = findViewById(R.id.dateInputLayout)
        dateInput = findViewById(R.id.dateInput)
        executeUSSDButton = findViewById(R.id.executeUSSDButton)
        manualReferenceButton = findViewById(R.id.manualReferenceButton)
        viewMoreButton = findViewById(R.id.viewMoreButton)
        quickAmountChipGroup = findViewById(R.id.quickAmountChipGroup)
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
    private fun setupQuickAmountChips() {
        // Limpiar chips anteriores
        quickAmountChipGroup.removeAllViews()
        
        // Obtener los montos más utilizados (ordenados por frecuencia de uso histórico)
        val topAmounts = amountUsageManager.getTopUsedAmounts()
        
        AppLogger.d(TAG, "Configurando chips con montos más utilizados: $topAmounts")
        
        // Solo mostrar chips si hay al menos 1 monto registrado
        if (topAmounts.isNotEmpty()) {
            topAmounts.forEach { amount ->
                val chip = Chip(this).apply {
                    text = amount
                    isClickable = true
                    setOnClickListener {
                        // Remover formato para insertar en el campo
                        val cleanAmount = amount.replace(",", "")
                        amountInput.setText(cleanAmount)
                        amountInput.setSelection(amountInput.text?.length ?: 0)
                        
                        // Registrar uso del monto
                        amountUsageManager.recordAmountUsage(cleanAmount)
                        
                        AppLogger.logUserAction(TAG, "Chip de monto seleccionado", amount)
                    }
                }
                quickAmountChipGroup.addView(chip)
            }
            
            // Mostrar el grupo de chips
            quickAmountChipGroup.visibility = View.VISIBLE
        } else {
            // No mostrar chips hasta que haya al menos 1 monto registrado
            quickAmountChipGroup.visibility = View.GONE
            AppLogger.d(TAG, "No hay montos registrados - ocultando chips")
        }
    }
    
    private fun setupActionButtons() {
        // Botón USSD
        executeUSSDButton.setOnClickListener {
            AppLogger.logButtonClick(TAG, "ExecuteUSSD", "Procesando servicio USSD")

            if (!validateInputs()) {
                showSnackbar("Por favor complete todos los campos requeridos")
                return@setOnClickListener
            }

            if (hasUSSDSupport(currentServiceType)) {
                processUSSDForService()
            } else {
                // Para servicios sin USSD, procesar manualmente
                processManualService()
            }
        }

        // Botón Referencia Manual
        manualReferenceButton.setOnClickListener {
            AppLogger.logButtonClick(TAG, "ManualReference", "Ingreso manual de referencia")
            showManualReferenceDialog()
        }

        // Botón Ver más (para reseteo de cliente)
        viewMoreButton.setOnClickListener {
            AppLogger.logButtonClick(TAG, "ViewMore", "Ver más información de reseteo")
            showResetClientMoreInfo()
        }
    }

    private fun validateInputs(): Boolean {
        val phone = phoneInput.text.toString().replace("-", "")
        val cedula = cedulaInput.text.toString()
        val amount = amountInput.text.toString().replace(",", "")

        return when (currentServiceType) {
            0, 1, 2 -> { // Giros, Retiros, Carga Billetera
                phone.length >= 10 && cedula.isNotEmpty() && amount.isNotEmpty()
            }
            3 -> { // Telefonía
                phone.length >= 10 && amount.isNotEmpty()
            }
            7 -> { // Reseteo de Cliente
                phone.length >= 10 && cedula.isNotEmpty() && dateInput.text.toString().length == 8
            }
            else -> {
                // Para otros servicios, validar según configuración
                val phoneValid = if (phoneInputLayout.visibility == View.VISIBLE)
                    phone.isNotEmpty() else true
                val cedulaValid = if (cedulaInputLayout.visibility == View.VISIBLE)
                    cedula.isNotEmpty() else true
                val amountValid = if (amountInputLayout.visibility == View.VISIBLE)
                    amount.isNotEmpty() else true

                phoneValid && cedulaValid && amountValid
            }
        }
    }

    private fun processManualService() {
        // Servicios como ANDE, ESSAP, COPACO que no usan USSD
        val cedula = cedulaInput.text.toString()

        when (currentServiceType) {
            8 -> { // ANDE
                if (cedula.isNotEmpty()) {
                    // Crear mensaje para impresión directa
                    val message = buildPrintMessage("ANDE", cedula, "", "")
                    showManualServiceDialog("ANDE", message)
                }
            }
            9 -> { // ESSAP
                if (cedula.isNotEmpty()) {
                    val message = buildPrintMessage("ESSAP", cedula, "", "")
                    showManualServiceDialog("ESSAP", message)
                }
            }
            10 -> { // COPACO
                if (cedula.isNotEmpty()) {
                    val message = buildPrintMessage("COPACO", cedula, "", "")
                    showManualServiceDialog("COPACO", message)
                }
            }
            else -> {
                showSnackbar("Servicio no configurado")
            }
        }
    }

    private fun showManualReferenceDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_manual_reference, null)
        val ref1Input = dialogView.findViewById<EditText>(R.id.reference1Input)
        val ref2Input = dialogView.findViewById<EditText>(R.id.reference2Input)

        MaterialAlertDialogBuilder(this)
            .setTitle("Ingreso Manual de Referencias")
            .setMessage("Ingrese las referencias de la transacción")
            .setView(dialogView)
            .setPositiveButton("Confirmar") { _, _ ->
                val ref1 = ref1Input.text.toString()
                val ref2 = ref2Input.text.toString()

                if (ref1.isNotEmpty() && ref2.isNotEmpty()) {
                    val referenceData = ReferenceData(ref1, ref2)
                    processManualReference(referenceData)
                } else {
                    showSnackbar("Por favor ingrese ambas referencias")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun processManualReference(referenceData: ReferenceData) {
        val phone = phoneInput.text.toString().replace("-", "")
        val cedula = cedulaInput.text.toString()
        val amount = amountInput.text.toString().replace(",", "")

        val serviceName = if (::currentService.isInitialized) {
            currentService.name
        } else {
            Constants.SERVICE_NAMES.getOrNull(currentServiceType) ?: "Servicio"
        }

        val message = buildPrintMessage(serviceName, phone, cedula, amount, referenceData)

        // Guardar en historial
        savePrintData(serviceName, message, referenceData)

        // Imprimir si está configurado
        if (bluetoothManager.isBluetoothEnabled() && bluetoothManager.hasSelectedDevice()) {
            printData(message, referenceData)
        } else {
            showPrintOptionsDialog(message, referenceData)
        }
    }

    private fun buildPrintMessage(
        service: String,
        phoneOrId: String,
        cedula: String,
        amount: String,
        referenceData: ReferenceData? = null
    ): String {
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val currentDate = Date()

        return buildString {
            appendLine("================================")
            appendLine("       GENIO TECNI S.A.")
            appendLine("================================")
            appendLine("SERVICIO: $service")
            appendLine("FECHA: ${dateFormatter.format(currentDate)}")
            appendLine("HORA: ${timeFormatter.format(currentDate)}")
            appendLine("--------------------------------")

            when (currentServiceType) {
                0, 1, 2 -> { // Giros, Retiros, Carga Billetera Tigo
                    appendLine("TELÉFONO: $phoneOrId")
                    appendLine("CÉDULA: $cedula")
                    if (amount.isNotEmpty()) {
                        appendLine("MONTO: Gs. ${formatAmount(amount)}")
                    }
                }
                3, 4, 5, 6 -> { // Servicios Tigo varios
                    appendLine("NÚMERO: $phoneOrId")
                    if (amount.isNotEmpty()) {
                        appendLine("MONTO: Gs. ${formatAmount(amount)}")
                    }
                }
                7 -> { // Reseteo de Cliente
                    appendLine("TELÉFONO: $phoneOrId")
                    appendLine("CÉDULA: $cedula")
                    appendLine("FECHA NAC: ${dateInput.text}")
                }
                8, 9, 10 -> { // ANDE, ESSAP, COPACO
                    appendLine("NIS/ISSAN/LÍNEA: $phoneOrId")
                }
                else -> {
                    if (phoneOrId.isNotEmpty()) appendLine("DATOS: $phoneOrId")
                    if (cedula.isNotEmpty()) appendLine("DOCUMENTO: $cedula")
                    if (amount.isNotEmpty()) appendLine("MONTO: Gs. ${formatAmount(amount)}")
                }
            }

            if (referenceData != null) {
                appendLine("--------------------------------")
                appendLine("REFERENCIAS:")
                appendLine("REF 1: ${referenceData.ref1}")
                appendLine("REF 2: ${referenceData.ref2}")
            }

            appendLine("================================")
            appendLine("    ¡GRACIAS POR SU PREFERENCIA!")
            appendLine("================================")
        }
    }

    private fun formatAmount(amount: String): String {
        return try {
            val number = amount.toLong()
            DecimalFormat("#,###").format(number)
        } catch (e: Exception) {
            amount
        }
    }

    private fun savePrintData(service: String, message: String, referenceData: ReferenceData?) {
        val printData = PrintData(
            service = service,
            date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
            time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
            message = message,
            referenceData = referenceData ?: ReferenceData("N/A", "N/A")
        )

        printDataManager.savePrintData(printData)
        AppLogger.i(TAG, "Datos guardados en historial: $service")
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
        
        // Hide/show quickAmountChipGroup based on amount input visibility
        quickAmountChipGroup.visibility = if (config.showAmount) View.VISIBLE else View.GONE
        
        // Handle date input for birth date
        dateInputLayout.visibility = if (config.showNacimiento) View.VISIBLE else View.GONE

        // Show "Ver más" button only for Reset Client service (ID 7)
        viewMoreButton.visibility = if (serviceType == 7) View.VISIBLE else View.GONE

        // Update hints
        phoneInputLayout.hint = config.phoneHint
        cedulaInputLayout.hint = config.cedulaHint
        amountInputLayout.hint = config.amountHint
        dateInputLayout.hint = config.nacimientoHint

        // Update edit mode manager with current service
        editModeManager.setCurrentServiceType(serviceType)

        // Clear inputs when switching services
        clearAllInputs()
        
        // Auto-load "09" for services that need phone input
        initializePhoneInput(serviceType)
        
        // Update chips with latest usage data
        setupQuickAmountChips()
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
    
    private fun initializePhoneInput(serviceType: Int) {
        val config = serviceConfigs[serviceType]
        
        // Auto-load "09" for services that require phone input
        if (config?.showPhone == true) {
            phoneInput.setText("09")
            phoneInput.setSelection(phoneInput.text?.length ?: 0) // Set cursor at end
            AppLogger.d(TAG, "Auto-cargado '09' para servicio tipo $serviceType")
        }
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
                
                // Registrar uso del monto si aplica
                val amountText = amountInput.text.toString().replace(",", "")
                if (amountText.isNotEmpty() && amountText.toLongOrNull() != null) {
                    amountUsageManager.recordAmountUsage(amountText)
                    AppLogger.d(TAG, "Uso de monto registrado: $amountText")
                }
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

    private fun printTransaction(printData: PrintData) {
        bluetoothManager.printData(printData.message) { success, error ->
            if (success) {
                AppLogger.i(TAG, "Impresión completada exitosamente")
                showSnackbar("Impresión completada")
            } else {
                AppLogger.e(TAG, "Error en impresión: $error")
                showSnackbar("Error de impresión: $error")
                showPrintErrorDialog(printData)
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

    private fun showResetClientMoreInfo() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Reseteo de Cliente - Información Adicional")
            .setMessage("""
                |El reseteo de cliente permite restaurar la información del usuario en el sistema Tigo Money.
                |
                |📋 Datos Requeridos:
                |• Número de teléfono (línea Tigo)
                |• Número de cédula de identidad
                |• Fecha de nacimiento (DD/MM/AAAA)
                |
                |⚠️ Importante:
                |• Verificar que todos los datos sean correctos
                |• El cliente debe estar presente durante el proceso
                |• Solo se puede realizar con líneas Tigo activas
                |
                |💡 Proceso:
                |1. Completar todos los campos requeridos
                |2. Presionar "Ejecutar USSD"
                |3. Seguir las instrucciones en pantalla
                |4. Confirmar el reseteo cuando se solicite
                |
                |📞 Soporte:
                |En caso de problemas, contactar al *611 desde línea Tigo.
            """.trimMargin())
            .setPositiveButton("Entendido", null)
            .setNeutralButton("Llamar Soporte") { _, _ ->
                // Intent to call support
                try {
                    val callIntent = Intent(Intent.ACTION_CALL)
                    callIntent.data = Uri.parse("tel:*611")
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                        startActivity(callIntent)
                    } else {
                        showSnackbar("Permiso de llamada requerido")
                    }
                } catch (e: Exception) {
                    showSnackbar("No se pudo realizar la llamada")
                }
            }
            .show()
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
                    val ussdCode = ussdIntegrationHelper.generateUSSDForTigoGiros(phone, cedula, amount)
                    ussdIntegrationHelper.executeUSSD(ussdCode, "Giros", this)
                    ussdIntegrationHelper.copyToClipboard(amount)
                    ussdIntegrationHelper.copyToClipboard(cedula)
                    ussdIntegrationHelper.copyToClipboard(phone)
                } else {
                    showSnackbar("Complete todos los datos requeridos")
                }
            }
            1 -> { // Retiros Tigo
                if (phone.length == 10 && cedula.length >= 5 && amount.isNotEmpty()) {
                    val ussdCode = ussdIntegrationHelper.generateUSSDForTigoRetiros(phone, cedula, amount)
                    ussdIntegrationHelper.executeUSSD(ussdCode, "Retiros", this)
                    ussdIntegrationHelper.copyToClipboard(amount)
                    ussdIntegrationHelper.copyToClipboard(cedula)
                    ussdIntegrationHelper.copyToClipboard(phone)
                } else {
                    showSnackbar("Complete todos los datos requeridos")
                }
            }
            2 -> { // Carga Billetera Tigo
                if (phone.length == 10 && cedula.length >= 5 && amount.isNotEmpty()) {
                    val ussdCode = ussdIntegrationHelper.generateUSSDForTigoBilletera(phone, cedula, amount)
                    ussdIntegrationHelper.executeUSSD(ussdCode, "Billetera", this)
                    ussdIntegrationHelper.copyToClipboard(amount)
                    ussdIntegrationHelper.copyToClipboard(cedula)
                    ussdIntegrationHelper.copyToClipboard(phone)
                } else {
                    showSnackbar("Complete todos los datos requeridos")
                }
            }
            3 -> { // Telefonia Tigo
                if (phone.length == 10) {
                    val ussdCode = ussdIntegrationHelper.generateUSSDForTigoTelefonia(phone)
                    ussdIntegrationHelper.executeUSSD(ussdCode, "Servicio", this)
                    ussdIntegrationHelper.copyToClipboard(phone)
                } else {
                    showSnackbar("Ingrese un número de teléfono válido")
                }
            }
            7 -> { // Reseteo de Cliente
                if (phone.length == 10 && cedula.length >= 5 && birthDate.length >= 8) {
                    val ussdCode = ussdIntegrationHelper.generateUSSDForReseteoCliente(phone, cedula, birthDate)
                    ussdIntegrationHelper.executeUSSD(ussdCode, "Reseteo", this)
                    ussdIntegrationHelper.copyToClipboard(phone)
                } else {
                    showSnackbar("Complete todos los datos requeridos")
                }
            }
            8 -> { // ANDE
                if (cedula.isNotEmpty()) {
                    val ussdCode = ussdIntegrationHelper.generateUSSDForANDE(cedula)
                    ussdIntegrationHelper.executeUSSD(ussdCode, "ANDE", this)
                    ussdIntegrationHelper.copyToClipboard(cedula)
                } else {
                    showSnackbar("Ingrese el número de NIS")
                }
            }
            9 -> { // ESSAP
                if (cedula.isNotEmpty()) {
                    val ussdCode = ussdIntegrationHelper.generateUSSDForESSAP(cedula)
                    ussdIntegrationHelper.executeUSSD(ussdCode, "ESSAP", this)
                    ussdIntegrationHelper.copyToClipboard(cedula)
                } else {
                    showSnackbar("Ingrese el número de ISSAN")
                }
            }
            10 -> { // COPACO
                if (cedula.isNotEmpty()) {
                    val ussdCode = ussdIntegrationHelper.generateUSSDForCOPACO(cedula)
                    ussdIntegrationHelper.executeUSSD(ussdCode, "COPACO", this)
                    ussdIntegrationHelper.copyToClipboard(cedula)
                } else {
                    showSnackbar("Ingrese el teléfono o cuenta")
                }
            }
            11 -> { // Retiros Personal
                if (phone.length == 10 && amount.isNotEmpty()) {
                    val ussdCode = ussdIntegrationHelper.generateUSSDForPersonalRetiros(phone, amount)
                    ussdIntegrationHelper.executeUSSD(ussdCode, "Retiros Personal", this)
                    ussdIntegrationHelper.copyToClipboard(amount)
                    ussdIntegrationHelper.copyToClipboard(phone)
                } else {
                    showSnackbar("Complete todos los datos requeridos")
                }
            }
            12 -> { // Telefonia Personal
                if (phone.length == 10 && amount.isNotEmpty()) {
                    val ussdCode = ussdIntegrationHelper.generateUSSDForPersonalTelefonia(phone, amount)
                    ussdIntegrationHelper.executeUSSD(ussdCode, "Telefonia Personal", this)
                    ussdIntegrationHelper.copyToClipboard(amount)
                    ussdIntegrationHelper.copyToClipboard(phone)
                } else {
                    showSnackbar("Complete todos los datos requeridos")
                }
            }
            else -> {
                showSnackbar("Servicio no configurado para USSD")
            }
        }
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
    
    // USSDCallback interface implementation
    override fun onReferenceFound(referenceData: ReferenceData, smsBody: String) {
        AppLogger.i(TAG, "Referencias encontradas via USSD: ${referenceData.ref1}")
        showReferenceDialog(referenceData, smsBody)
        
        // Save print data with references
        val serviceName = if (::currentService.isInitialized) {
            currentService.name
        } else {
            Constants.SERVICE_NAMES.getOrNull(currentServiceType) ?: "Servicio"
        }
        
        val message = buildPrintMessage(serviceName, 
            phoneInput.text.toString(), 
            cedulaInput.text.toString(), 
            amountInput.text.toString().replace(",", ""), 
            referenceData)
        savePrintData(serviceName, message, referenceData)
    }
    
    override fun onSearchTimeout() {
        AppLogger.w(TAG, "Timeout en búsqueda de referencias")
        showSnackbar("Tiempo de búsqueda agotado. Intente nuevamente.")
    }
    
    override fun onError(error: String) {
        AppLogger.e(TAG, "Error en integración USSD: $error")
        showSnackbar("Error: $error")
    }
    
    // Setup TextWatchers method
    private fun setupTextWatchers() {
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
    
    // Load saved values method
    private fun loadSavedValues() {
        // Load any saved values from preferences if needed
        AppLogger.d(TAG, "Cargando valores guardados")
    }
    
    // Print data with Bluetooth integration
    private fun printData(message: String, referenceData: ReferenceData?) {
        bluetoothManager.printData(message) { success, error ->
            if (success) {
                AppLogger.i(TAG, "Impresión completada exitosamente")
                showSnackbar("Impresión completada")
            } else {
                AppLogger.e(TAG, "Error en impresión: $error")
                showSnackbar("Error de impresión: $error")
                showPrintOptionsDialog(message, referenceData)
            }
        }
    }
    
    private fun showPrintOptionsDialog(message: String, referenceData: ReferenceData?) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Error de Impresión")
            .setMessage("No se pudo imprimir. ¿Qué desea hacer?")
            .setPositiveButton("Reintentar") { _, _ ->
                printData(message, referenceData)
            }
            .setNegativeButton("Configurar Bluetooth") { _, _ ->
                startActivity(Intent(this, Bt::class.java))
            }
            .setNeutralButton("Cancelar", null)
            .show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        ussdIntegrationHelper.onDestroy()
    }
}