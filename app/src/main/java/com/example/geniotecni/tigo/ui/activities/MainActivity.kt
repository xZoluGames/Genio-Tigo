package com.example.geniotecni.tigo.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
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
    private lateinit var cedulaInputLayout: TextInputLayout
    private lateinit var cedulaInput: TextInputEditText
    private lateinit var amountInputLayout: TextInputLayout
    private lateinit var amountInput: TextInputEditText
    private lateinit var dateInputLayout: TextInputLayout
    private lateinit var dateInput: TextInputEditText

    // Control buttons
    private lateinit var confirmButton: MaterialButton
    private lateinit var infoButton: MaterialButton
    private lateinit var increaseButton: MaterialButton
    private lateinit var decreaseButton: MaterialButton
    private lateinit var resetAmountButton: MaterialButton

    // Amount controls container
    private lateinit var amountControls: LinearLayout

    private var currentServiceType = 0
    private lateinit var currentService: ServiceItem
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var lastSmsTimestamp = 0L
    private var smsObserver: ContentObserver? = null
    private var searchJob: Job? = null

    // Service configurations map
    private val serviceConfigs = mapOf(
        // Giros Tigo
        0 to ServiceConfig(
            showPhone = true,
            showCedula = true,
            showAmount = true,
            phoneHint = "Número de teléfono",
            cedulaHint = "Número de cédula",
            amountHint = "Monto a enviar"
        ),
        // ANDE
        1 to ServiceConfig(
            showPhone = false,
            showCedula = true,
            showAmount = false,
            cedulaHint = "Número de NIS"
        ),
        // Reseteo de Cliente
        2 to ServiceConfig(
            showPhone = true,
            showCedula = true,
            showAmount = false,
            showNacimiento = true,
            phoneHint = "Número de teléfono",
            cedulaHint = "Número de cédula",
            nacimientoHint = "Fecha de nacimiento"
        ),
        // Telefonia Tigo
        3 to ServiceConfig(
            showPhone = true,
            showCedula = false,
            showAmount = false,
            phoneHint = "Número de teléfono"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.service)

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
            currentService = it
            currentServiceType = it.serviceType
            updateServiceConfiguration(it.serviceType)
        } ?: run {
            // Default to first service if no selection
            currentServiceType = 0
            updateServiceConfiguration(0)
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
        // Main layout
        coordinatorLayout = findViewById(R.id.coordinatorLayout)
        toolbar = findViewById(R.id.toolbar)
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

        // Buttons
        confirmButton = findViewById(R.id.confirmButton)
        infoButton = findViewById(R.id.infoButton)
        increaseButton = findViewById(R.id.increaseButton)
        decreaseButton = findViewById(R.id.decreaseButton)
        resetAmountButton = findViewById(R.id.resetAmountButton)

        // Amount controls
        amountControls = findViewById(R.id.amountControls)

        // Setup toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun initializeManagers() {
        preferencesManager = PreferencesManager(this)
        printDataManager = PrintDataManager(this)
        printCooldownManager = PrintCooldownManager(this)
        editModeManager = EditModeManager(this, this, coordinatorLayout, preferencesManager)

        // Initialize edit mode manager
        editModeManager.initializeEditMode()
    }

    private fun setupListeners() {
        // Amount control buttons
        increaseButton.setOnClickListener {
            if (!editModeManager.isInEditMode()) {
                adjustAmount(1000)
            }
        }

        decreaseButton.setOnClickListener {
            if (!editModeManager.isInEditMode()) {
                adjustAmount(-1000)
            }
        }

        resetAmountButton.setOnClickListener {
            if (!editModeManager.isInEditMode()) {
                amountInput.setText("")
            }
        }

        // Main action buttons
        confirmButton.setOnClickListener {
            if (!editModeManager.isInEditMode()) {
                processTransaction()
            }
        }

        infoButton.setOnClickListener {
            if (!editModeManager.isInEditMode()) {
                showServiceInfo()
            }
        }

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

    private fun formatPhoneNumber(number: String): String {
        return when {
            number.length <= 4 -> number
            number.length <= 7 -> "${number.substring(0, 4)}-${number.substring(4)}"
            else -> "${number.substring(0, 4)}-${number.substring(4, 7)}-${number.substring(7, minOf(number.length, 10))}"
        }
    }

    private fun updateServiceConfiguration(serviceType: Int) {
        currentServiceType = serviceType
        val config = serviceConfigs[serviceType] ?: return

        // Update toolbar title
        toolbar.title = when (serviceType) {
            0 -> "Giros Tigo"
            1 -> "ANDE"
            2 -> "Reseteo de Cliente"
            3 -> "Telefonía Tigo"
            else -> "Servicio"
        }

        // Update service title
        serviceTitle.text = toolbar.title

        // Update visibility of input fields
        phoneInputLayout.visibility = if (config.showPhone) View.VISIBLE else View.GONE
        cedulaInputLayout.visibility = if (config.showCedula) View.VISIBLE else View.GONE
        amountInputLayout.visibility = if (config.showAmount) View.VISIBLE else View.GONE
        amountControls.visibility = if (config.showAmount) View.VISIBLE else View.GONE

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

    private fun adjustAmount(increment: Int) {
        val currentText = amountInput.text.toString().replace(",", "")
        val currentAmount = currentText.toLongOrNull() ?: 0L
        val newAmount = (currentAmount + increment).coerceAtLeast(0)
        val formatted = DecimalFormat(DECIMAL_FORMAT_PATTERN).format(newAmount)
        amountInput.setText(formatted)
    }

    private fun processTransaction() {
        if (!validateInputs()) {
            return
        }

        // Check print cooldown
        val printStatus = printCooldownManager.canPrint()
        if (!printStatus.canPrint) {
            showSnackbar(printStatus.message)
            return
        }

        // Check Bluetooth device
        if (!checkBluetoothDevice()) {
            return
        }

        // Show processing dialog
        showProcessingDialog()

        // Process the transaction
        CoroutineScope(Dispatchers.Main).launch {
            try {
                delay(1500) // Simulate processing

                val printData = createPrintData()
                printDataManager.savePrintData(printData)
                printCooldownManager.recordPrint()

                // Print if auto print is enabled
                if (preferencesManager.autoPrint) {
                    printTransaction(printData)
                }

                hideProcessingDialog()
                showSuccessDialog(printData)

                // Clear inputs after successful transaction
                clearAllInputs()

            } catch (e: Exception) {
                hideProcessingDialog()
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
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val deviceAddress = sharedPreferences.getString("BluetoothDeviceAddress", null) ?: return

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val uuid = UUID.fromString(BLUETOOTH_UUID)
                val socket = device.createRfcommSocketToServiceRecord(uuid)
                socket.connect()

                val outputStream = socket.outputStream

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

                outputStream.close()
                socket.close()

                withContext(Dispatchers.Main) {
                    showSnackbar("Impresión completada")
                }

            } catch (e: IOException) {
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

    override fun onDestroy() {
        super.onDestroy()
        searchJob?.cancel()
        smsObserver?.let { contentResolver.unregisterContentObserver(it) }
    }
}