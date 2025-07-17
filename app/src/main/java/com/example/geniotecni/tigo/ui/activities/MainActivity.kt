package com.example.geniotecni.tigo.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
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
import android.provider.Telephony
import android.telecom.TelecomManager
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.geniotecni.tigo.R
import com.example.geniotecni.tigo.managers.PrintDataManager
import com.example.geniotecni.tigo.managers.PrintCooldownManager
import com.example.geniotecni.tigo.managers.PreferencesManager
import com.example.geniotecni.tigo.managers.EditModeManager
import com.example.geniotecni.tigo.models.PrintData
import com.example.geniotecni.tigo.models.ReferenceData
import kotlinx.coroutines.*
import java.io.IOException
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    
    data class ServiceConfig(
        val showPhone: Boolean = false,
        val showCedula: Boolean = false,
        val showAmount: Boolean = false,
        val showConsulta: Boolean = false,
        val showNacimiento: Boolean = false,
        val cedulaHint: String = "Ingrese el numero de cedula",
        val amountHint: String = "Ingrese el monto",
        val imageResource: Int = R.drawable.tigo_cuadrado,
        val inputType: Int = InputType.TYPE_CLASS_NUMBER,
        val defaultText: String? = null,
        val ussdCode: String? = null,
        val simIndex: Int = 0
    )

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
        private const val FILE_NAME = "user_data.txt"
        private const val BLUETOOTH_UUID = "00001101-0000-1000-8000-00805F9B34FB"
        private const val DECIMAL_FORMAT_PATTERN = "#,###"
        private const val DATE_FORMAT = "dd-MM-yyyy"
        private const val TIME_FORMAT = "HH:mm:ss"
        private const val COMMISSION_RATE = 0.06f
        private const val TAG = "MainActivity"
    }

    // Properties
    private lateinit var printDataManager: PrintDataManager
    private lateinit var printCooldownManager: PrintCooldownManager
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var editModeManager: EditModeManager

    // Service layout components
    private lateinit var serviceContainer: ConstraintLayout
    private lateinit var serviceImage: ImageView
    private lateinit var serviceTitle: TextView
    private lateinit var phoneLabel: TextView
    private lateinit var phoneInput: EditText
    private lateinit var cedulaLabel: TextView
    private lateinit var cedulaInput: EditText
    private lateinit var amountLabel: TextView
    private lateinit var amountInput: EditText
    private lateinit var dateLabel: TextView
    private lateinit var dateInput: EditText
    private lateinit var confirmButton: ImageButton
    private lateinit var infoButton: ImageButton
    private lateinit var increaseButton: ImageButton
    private lateinit var decreaseButton: ImageButton
    private lateinit var resetAmountButton: ImageButton

    private var currentServiceType = 0
    private var isFirstClick = true
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var isSearching = false
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
            imageResource = R.drawable.tigo_cuadrado,
            cedulaHint = "Ingrese el numero de cedula",
            amountHint = "Ingrese el monto"
        ),
        // ANDE
        1 to ServiceConfig(
            showPhone = false, 
            showCedula = true, 
            showAmount = false, 
            imageResource = R.drawable.ande_icon,
            cedulaHint = "Ingrese el numero de cedula"
        ),
        // Reseteo de Cliente
        2 to ServiceConfig(
            showPhone = true, 
            showCedula = true, 
            showAmount = false,
            showNacimiento = true,
            imageResource = R.drawable.tigo_cuadrado,
            cedulaHint = "Ingrese el numero de cedula"
        ),
        // Telefonia Tigo
        3 to ServiceConfig(
            showPhone = true, 
            showCedula = false, 
            showAmount = false, 
            imageResource = R.drawable.tigo_cuadrado
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.service)

        initializeViews()
        initializeManagers()
        setupListeners()
        setupPermissions()
        
        // Load default service configuration based on intent or default to 0
        val serviceType = intent.getIntExtra("SERVICE_TYPE", 0)
        updateServiceConfiguration(serviceType)
    }

    private fun initializeViews() {
        // Service layout components
        serviceContainer = findViewById(R.id.serviceContainer)
        serviceImage = findViewById(R.id.serviceImage)
        serviceTitle = findViewById(R.id.serviceTitle)
        phoneLabel = findViewById(R.id.phoneLabel)
        phoneInput = findViewById(R.id.phoneInput)
        cedulaLabel = findViewById(R.id.cedulaLabel)
        cedulaInput = findViewById(R.id.cedulaInput)
        amountLabel = findViewById(R.id.amountLabel)
        amountInput = findViewById(R.id.amountInput)
        dateLabel = findViewById(R.id.dateLabel)
        dateInput = findViewById(R.id.dateInput)
        confirmButton = findViewById(R.id.confirmButton)
        infoButton = findViewById(R.id.infoButton)
        increaseButton = findViewById(R.id.increaseButton)
        decreaseButton = findViewById(R.id.decreaseButton)
        resetAmountButton = findViewById(R.id.resetAmountButton)
    }

    private fun initializeManagers() {
        preferencesManager = PreferencesManager(this)
        printDataManager = PrintDataManager(this)
        printCooldownManager = PrintCooldownManager(this)
        editModeManager = EditModeManager(this, this, serviceContainer, preferencesManager)
        
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

        // Set up automatic phone number prefix
        phoneInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.isNotEmpty() && !s.toString().startsWith("09")) {
                    phoneInput.setText("09${s.toString()}")
                    phoneInput.setSelection(phoneInput.text.length)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun updateServiceConfiguration(serviceType: Int) {
        currentServiceType = serviceType
        val config = serviceConfigs[serviceType] ?: return
        
        // Update service image and title
        serviceImage.setImageResource(config.imageResource)
        serviceTitle.text = when (serviceType) {
            0 -> "Giros Tigo"
            1 -> "ANDE"
            2 -> "Reseteo de Cliente"
            3 -> "Telefonia Tigo"
            else -> "Servicio"
        }

        // Show/hide components based on service
        phoneLabel.visibility = if (config.showPhone) View.VISIBLE else View.GONE
        phoneInput.visibility = if (config.showPhone) View.VISIBLE else View.GONE
        
        cedulaLabel.visibility = if (config.showCedula) View.VISIBLE else View.GONE
        cedulaInput.visibility = if (config.showCedula) View.VISIBLE else View.GONE
        
        amountLabel.visibility = if (config.showAmount) View.VISIBLE else View.GONE
        amountInput.visibility = if (config.showAmount) View.VISIBLE else View.GONE
        
        // Show/hide amount control buttons based on amount visibility
        increaseButton.visibility = if (config.showAmount) View.VISIBLE else View.GONE
        decreaseButton.visibility = if (config.showAmount) View.VISIBLE else View.GONE
        resetAmountButton.visibility = if (config.showAmount) View.VISIBLE else View.GONE
        
        // Handle birth date for reseteo service
        if (config.showNacimiento) {
            dateLabel.visibility = View.VISIBLE
            dateInput.visibility = View.VISIBLE
            dateLabel.text = "Ingrese fecha de nacimiento"
            dateInput.hint = "dd/mm/yyyy"
        } else {
            dateLabel.visibility = View.GONE
            dateInput.visibility = View.GONE
        }

        // Update hints
        cedulaLabel.text = config.cedulaHint
        amountLabel.text = config.amountHint

        // Update edit mode manager with current service
        editModeManager.setCurrentServiceType(serviceType)
        
        // Clear inputs when switching services
        phoneInput.setText("")
        cedulaInput.setText("")
        amountInput.setText("")
        dateInput.setText("")
    }

    private fun adjustAmount(increment: Int) {
        val currentAmount = amountInput.text.toString().toIntOrNull() ?: 0
        val newAmount = (currentAmount + increment).coerceAtLeast(0)
        amountInput.setText(newAmount.toString())
    }

    private fun processTransaction() {
        val config = serviceConfigs[currentServiceType] ?: return
        
        // Validate required fields
        if (config.showPhone && phoneInput.text.isBlank()) {
            showToast("Por favor ingrese el número de teléfono")
            return
        }
        
        if (config.showCedula && cedulaInput.text.isBlank()) {
            showToast("Por favor ingrese el número de cédula")
            return
        }
        
        if (config.showAmount && amountInput.text.isBlank()) {
            showToast("Por favor ingrese el monto")
            return
        }
        
        if (config.showNacimiento && dateInput.text.isBlank()) {
            showToast("Por favor ingrese la fecha de nacimiento")
            return
        }

        // Check print cooldown
        val printStatus = printCooldownManager.canPrint()
        if (!printStatus.canPrint) {
            showToast(printStatus.message)
            return
        }

        // Process the transaction
        try {
            val printData = createPrintData()
            printDataManager.savePrintData(printData)
            printCooldownManager.recordPrint()
            
            showToast("Transacción procesada exitosamente")
            
            // Clear inputs after successful transaction
            phoneInput.setText("")
            cedulaInput.setText("")
            amountInput.setText("")
            dateInput.setText("")
            
        } catch (e: Exception) {
            showToast("Error al procesar la transacción: ${e.message}")
        }
    }

    private fun createPrintData(): PrintData {
        val config = serviceConfigs[currentServiceType] ?: throw IllegalStateException("Invalid service type")
        
        val phone = if (config.showPhone) phoneInput.text.toString() else ""
        val cedula = if (config.showCedula) cedulaInput.text.toString() else ""
        val amount = if (config.showAmount) amountInput.text.toString().toLongOrNull() ?: 0L else 0L
        val birthDate = if (config.showNacimiento) dateInput.text.toString() else ""
        
        val serviceName = when (currentServiceType) {
            0 -> "Giros Tigo"
            1 -> "ANDE"
            2 -> "Reseteo de Cliente"
            3 -> "Telefonia Tigo"
            else -> "Servicio Desconocido"
        }
        
        val currentTime = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        val timeFormat = SimpleDateFormat(TIME_FORMAT, Locale.getDefault())
        val currentDate = Date(currentTime)
        
        val message = buildString {
            append("Servicio: $serviceName\n")
            if (phone.isNotEmpty()) append("Teléfono: $phone\n")
            if (cedula.isNotEmpty()) append("Cédula: $cedula\n")
            if (amount > 0) append("Monto: ${DecimalFormat(DECIMAL_FORMAT_PATTERN).format(amount)} Gs.\n")
            if (birthDate.isNotEmpty()) append("Fecha Nacimiento: $birthDate\n")
            if (amount > 0) {
                val commission = (amount * COMMISSION_RATE).toLong()
                append("Comisión: ${DecimalFormat(DECIMAL_FORMAT_PATTERN).format(commission)} Gs.")
            }
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

    private fun showServiceInfo() {
        val config = serviceConfigs[currentServiceType] ?: return
        
        val infoMessage = when (currentServiceType) {
            0 -> "Giros Tigo: Permite enviar dinero usando teléfono, cédula y monto"
            1 -> "ANDE: Pago de facturas eléctricas usando solo el número de cédula"
            2 -> "Reseteo de Cliente: Resetea información del cliente usando teléfono, cédula y fecha de nacimiento"
            3 -> "Telefonía Tigo: Servicios de telefonía usando solo el número de teléfono"
            else -> "Información del servicio no disponible"
        }
        
        AlertDialog.Builder(this)
            .setTitle("Información del Servicio")
            .setMessage(infoMessage)
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
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissionsToRequest = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            
            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
            }
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
                    showToast("Algunos permisos fueron denegados. La aplicación puede no funcionar correctamente.")
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        searchJob?.cancel()
        smsObserver?.let { contentResolver.unregisterContentObserver(it) }
    }
}