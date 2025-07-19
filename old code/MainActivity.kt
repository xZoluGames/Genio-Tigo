package com.example.geniotecni.tigo

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
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.service.consulta_bt
import kotlinx.android.synthetic.main.service.devolver
import kotlinx.android.synthetic.main.service.down
import kotlinx.android.synthetic.main.service.fecha
import kotlinx.android.synthetic.main.service.hora
import kotlinx.android.synthetic.main.service.imageView
import kotlinx.android.synthetic.main.service.ingnumerodcelular
import kotlinx.android.synthetic.main.service.swipe
import kotlinx.android.synthetic.main.service.txtcedula_giros
import kotlinx.android.synthetic.main.service.txtmonto_giros
import kotlinx.android.synthetic.main.service.txtnumer_giros
import kotlinx.android.synthetic.main.service.up
import kotlinx.android.synthetic.main.service.view_cedula
import kotlinx.android.synthetic.main.service.view_monto
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import android.database.Cursor
import android.provider.Telephony
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import kotlinx.android.synthetic.main.service.txtnacimiento
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    data class ReferenceData(val ref1: String, val ref2: String)

    private var smsObserver: ContentObserver? = null
    private var searchJob: Job? = null
    lateinit var option: Spinner
    lateinit var result: TextView
    private var services = ""
    private var pin = ""
    private var currentIndex = 0
    private val fileName = "user_data.txt"
    private var isFirstClick = true
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var isSearching = false
    private var lastSmsTimestamp: Long = 0L
    private lateinit var printDataManager: PrintDataManager
    private val s = arrayOf(
        "Giros Tigo",
        /*1*/
        "Retiros Tigo",
        "Carga Billetera Tigo",
        "Telefonia Tigo",
        "Pago TV e Internet Hogar",
        "Antena (Wimax)",
        /*6*/
        "Tigo TV anticipado",
        "ANDE",
        "ESSAP",
        "COPACO",
        "Carga Billetera Personal",
        /*11*/
        "Retiros Personal",
        "Telefonia Personal",
        "Alex S.A",
        "Electroban",
        "Leopard",
        /*16*/
        "Chacomer",
        "Inverfin",
        "Che Duo-Carsa (Prestamos)",
        "Banco Familar (Prestamos)",
        "Financiera El Comercio",
        /*21*/
        "Interfisa (Prestamos)",
        "Financiera Paraguayo Japonesa (Prestamos)",
        "Credito Amigo (Prestamos)",
        "Tu Financiera (Prestamos)",
        "Funacion Industrial (Prestamos)",
        /*26*/
        "Banco Vision Pago de Tarjetas",
        "Banco Vision Pago de Prestamos",
        "Fiado.Net (Prestamos)",
        "Financiera Solar Pago de Tarjetas",
        "Financiera Solar Pago de Prestamos",
        /*31*/
        "Interfisa Pago de Tarjetas",
        "Banco Itau (Prestamos)",
        "Cooperativa Universitaria (Prestamos)",
        "Cooperativa Universitaria (Tarjeta Mastercard)",
        "Cooperativa Universitaria (Tarjeta Cabal)",
        /*36*/
        "Cooperativa Universitaria (Tarjeta Panal)",
        "CopexSanJo (Tarjeta Credito Visa)",
        "CopexSanJo (Tarjeta Credito Cabal)",
        "CopexSanJo (Solidaridad)",
        "CopexSanJo (Cuotas)",
        /*41*/
        "CopexSanJo (Aportes)",
        "Caja Mutual De Cooperativistas Del Paraguay CMCP (Tarjeta Cabal)",
        "Caja Mutual De Cooperativistas Del Paraguay CMCP (Tarjeta Mastercard)",
        "Caja Mutual De Cooperativistas Del Paraguay CMCP (Credito)",
        "Caja Mutual De Cooperativistas Del Paraguay CMCP (Aporte)",
        /*46*/
        "Cooperativa Tupãrenda (Aporte y Solidaridad)",
        "Cooperativa Tupãrenda (Prestamos)",
        "Cooperativa San Cristobal (Admision)",
        "Cooperativa San Cristobal (Tarjeta Mastercard)",
        "Cooperativa San Cristobal (Solidaridad)",
        /*51*/
        "Cooperativa San Cristobal (Aporte)",
        "Cooperativa San Cristobal (Prestamo)",
        "Cooperativa San Cristobal (Tarjeta Unica)",
        "Cooperativa San Cristobal (Tarjeta Visa)",
        "Cooperativa San Cristobal (Tarjeta Credicard)",
        /*56*/
        "Cooperativa Yoayu (Sepelios)",
        "Cooperativa Yoayu (Tarjeta Cabal)",
        "Cooperativa Yoayu (Tarjeta Visa)",
        "Cooperativa Yoayu (Fondos)",
        "Cooperativa Yoayu (Solidaridad)",
        /*61*/
        "Cooperativa Yoayu (Aporte)",
        "Cooperativa Yoayu",
        "Cooperativa Coomecipar (Solidaridad)",
        "Cooperativa Coomecipar (Prestamo)",
        "Cooperativa Coomecipar (Tarjeta Mastercard)",
        /*66*/
        "Cooperativa Coomecipar (Tarjeta Credicard)",
        "Cooperativa Coomecipar (Tarjeta Cabal)",
        "Cooperativa Coomecipar (Aportes)",
        "Cooperativa Medalla Milagrosa (Tarjeta Visa)",
        "Cooperativa Medalla Milagrosa (Solidaridad)",
        /*71*/
        "Cooperativa Medalla Milagrosa (Tarjeta Mastercard)",
        "Cooperativa Medalla Milagrosa (Tarjeta Credicard)",
        "Cooperativa Medalla Milagrosa (Tarjeta Cabal)",
        "Cooperativa Medalla Milagrosa (Creditos)",
        "Reseteo de Pin (Cliente)"
    )

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.service)
        val history = readFromFile()
        result = findViewById<TextView>(R.id.txView_servicio)
        option = findViewById<Spinner>(R.id.sp_option)
        currentIndex = history.size - 1
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        devolver.setOnClickListener {
            saveDataLogic()
            currentIndex = 0
            cancelSearch()
            startActivity(Intent(this@MainActivity, SearchServices::class.java))
        }
        requestCall()
        requestSms()
        requestBt()
        val editText = findViewById<EditText>(R.id.txtmonto_giros)
        down.visibility = View.INVISIBLE
        up.setOnClickListener {
            if (isFirstClick) {
                loadFromFile(history, currentIndex)
                isFirstClick = false
                Toast.makeText(this, "Cambiando...", Toast.LENGTH_SHORT).show()
                saveDataLogic()
            } else {
                if (currentIndex > 0) {
                    loadFromFile(history, currentIndex - 1)
                    currentIndex--
                    Toast.makeText(this, "Cambiando...", Toast.LENGTH_SHORT).show()
                }
                fecha.visibility = View.VISIBLE
            }
            if (currentIndex < history.size - 1) {
                down.visibility = View.VISIBLE
            } else {
                down.visibility = View.INVISIBLE
            }
        }
        down.setOnClickListener {
            if (currentIndex < history.size - 1) {
                currentIndex++
                loadFromFile(history, currentIndex)
                Toast.makeText(this, "Cambiando...", Toast.LENGTH_SHORT).show()
            } else {
                down.visibility = View.INVISIBLE
            }
            fecha.visibility = View.VISIBLE
        }
        printDataManager = PrintDataManager(this)
        editText.addTextChangedListener(object : TextWatcher {
            private var currentText = ""
            private val decimalFormat = DecimalFormat("#,###")
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                if (text != currentText) {
                    editText.removeTextChangedListener(this)

                    val cleanText = text.replace(".", "").replace(",", "")
                    val formattedText = if (cleanText.isEmpty()) {
                        ""
                    } else {
                        val number = cleanText.toLong()
                        decimalFormat.format(number)
                    }

                    editText.setText(formattedText)
                    editText.setSelection(formattedText.length)
                    currentText = formattedText

                    editText.addTextChangedListener(this)
                }
            }
        })
        option.adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, s)
        option.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                result.text = s[position]
                val currentDateTime = LocalDateTime.now()
                val f = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                val h = DateTimeFormatter.ofPattern("HH:mm:ss")
                val ft = currentDateTime.format(f)
                val ht = currentDateTime.format(h)
                val cl: ConstraintLayout = findViewById(R.id.im)
                val cS = ConstraintSet()
                val selectedService = intent.getStringExtra("selectedService")
                if (!selectedService.isNullOrEmpty()) {
                    result.text = intent.getStringExtra("selectedService")
                } else {
                    result.text = s[position]
                }
                if (result.text == s[0]) {
                    ingnumerodcelular.visibility = View.VISIBLE
                    txtnumer_giros.visibility = View.VISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    view_monto.visibility = View.VISIBLE
                    txtmonto_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.INVISIBLE
                    view_cedula.text = "Ingrese el numero de cedula"
                    imageView.setImageResource(R.drawable.tigo_cuadrado)
                    fecha.text = ft
                    hora.text = ht
                    services = s[0]
                }
                if (result.text == s[1]) {
                    ingnumerodcelular.visibility = View.VISIBLE
                    txtnumer_giros.visibility = View.VISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    view_monto.visibility = View.VISIBLE
                    txtmonto_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.INVISIBLE
                    view_cedula.text = "Ingrese el numero de cedula"
                    imageView.setImageResource(R.drawable.tigo_cuadrado)
                    fecha.text = ft
                    hora.text = ht
                    services = s[1]
                }
                if (result.text == s[2]) {
                    ingnumerodcelular.visibility = View.VISIBLE
                    txtnumer_giros.visibility = View.VISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    view_monto.visibility = View.VISIBLE
                    txtmonto_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.INVISIBLE
                    view_cedula.text = "Ingrese el numero de cedula"
                    imageView.setImageResource(R.drawable.tigo_cuadrado)
                    fecha.text = ft
                    hora.text = ht
                    services = s[2]
                }
                if (result.text == s[3]) {
                    ingnumerodcelular.visibility = View.VISIBLE
                    txtnumer_giros.visibility = View.VISIBLE
                    view_cedula.visibility = View.INVISIBLE
                    txtcedula_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    imageView.setImageResource(R.drawable.tigo_cuadrado)
                    fecha.text = ft
                    hora.text = ht
                    services = s[3]
                }
                if (result.text == s[4]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el numero de cedula"
                    imageView.setImageResource(R.drawable.tigo_cuadrado)
                    fecha.text = ft
                    hora.text = ht
                    services = s[4]
                }
                if (result.text == s[5]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de Cuenta"
                    imageView.setImageResource(R.drawable.tigo_cuadrado)
                    fecha.text = ft
                    hora.text = ht
                    services = s[5]
                }
                if (result.text == s[6]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    consulta_bt.visibility = View.INVISIBLE
                    view_cedula.text = "Ingrese el Nro de Cliente"
                    imageView.setImageResource(R.drawable.tigo_cuadrado)
                    fecha.text = ft
                    hora.text = ht
                    services = s[6]
                }
                if (result.text == s[7]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el nro de NIS"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.ande_icon)
                    fecha.text = ft
                    hora.text = ht
                    services = s[7]
                }
                if (result.text == s[8]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el nro de issan"
                    txtcedula_giros.setText("ZV")
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_TEXT
                    imageView.setImageResource(R.drawable.essap_icon)
                    fecha.text = ft
                    hora.text = ht
                    services = s[8]
                }
                if (result.text == s[9]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Telefono o Cuenta"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cocapo_icon)
                    fecha.text = ft
                    hora.text = ht
                    services = s[9]
                }
                if (result.text == s[10]) {
                    ingnumerodcelular.visibility = View.VISIBLE
                    txtnumer_giros.visibility = View.VISIBLE
                    view_cedula.visibility = View.INVISIBLE
                    txtcedula_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.VISIBLE
                    txtmonto_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.INVISIBLE
                    view_cedula.text = "Ingrese el numero de cedula"
                    imageView.setImageResource(R.drawable.personal_logo)
                    fecha.text = ft
                    hora.text = ht
                    services = s[10]
                }
                if (result.text == s[11]) {
                    ingnumerodcelular.visibility = View.VISIBLE
                    txtnumer_giros.visibility = View.VISIBLE
                    view_cedula.visibility = View.INVISIBLE
                    txtcedula_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.VISIBLE
                    txtmonto_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.INVISIBLE
                    imageView.setImageResource(R.drawable.personal_logo)
                    view_cedula.text = "Ingrese el numero de cedula"
                    fecha.text = ft
                    hora.text = ht
                    services = s[11]
                }
                if (result.text == s[12]) {
                    ingnumerodcelular.visibility = View.VISIBLE
                    txtnumer_giros.visibility = View.VISIBLE
                    view_cedula.visibility = View.INVISIBLE
                    txtcedula_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.VISIBLE
                    txtmonto_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    imageView.setImageResource(R.drawable.personal_logo)
                    view_cedula.text = "Ingrese el numero de cedula"
                    fecha.text = ft
                    hora.text = ht
                    services = s[12]
                }
                if (result.text == s[13]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.alex)
                    fecha.text = ft
                    hora.text = ht
                    services = s[13]
                }
                if (result.text == s[14]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.electroban)
                    fecha.text = ft
                    hora.text = ht
                    services = s[14]
                }
                if (result.text == s[15]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.leopard)
                    fecha.text = ft
                    hora.text = ht
                    services = s[15]
                }
                if (result.text == s[16]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.chacomer)
                    fecha.text = ft
                    hora.text = ht
                    services = s[16]
                }
                if (result.text == s[17]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.inverfin)
                    fecha.text = ft
                    hora.text = ht
                    services = s[17]
                }
                if (result.text == s[18]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.che_duo_carsa)
                    fecha.text = ft
                    hora.text = ht
                    services = s[18]
                }
                if (result.text == s[19]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.banco_familiar)
                    fecha.text = ft
                    hora.text = ht
                    services = s[19]
                }
                if (result.text == s[20]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.financiera_el_comercio)
                    fecha.text = ft
                    hora.text = ht
                    services = s[20]
                }
                if (result.text == s[21]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.interfisa_prestamo)
                    fecha.text = ft
                    hora.text = ht
                    services = s[21]
                }
                if (result.text == s[22]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.financiera_paraguayo_japonesa)
                    fecha.text = ft
                    hora.text = ht
                    services = s[22]
                }
                if (result.text == s[23]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.credito_amigo)
                    fecha.text = ft
                    hora.text = ht
                    services = s[23]
                }
                if (result.text == s[24]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.tu_financiera)
                    fecha.text = ft
                    hora.text = ht
                    services = s[24]
                }
                if (result.text == s[25]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.fundacion_industrial)
                    fecha.text = ft
                    hora.text = ht
                    services = s[25]
                }
                if (result.text == s[26]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.vision_banco)
                    fecha.text = ft
                    hora.text = ht
                    services = s[26]
                }
                if (result.text == s[27]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.vision_banco)
                    fecha.text = ft
                    hora.text = ht
                    services = s[27]
                }
                if (result.text == s[28]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.fiado_net)
                    fecha.text = ft
                    hora.text = ht
                    services = s[28]
                }
                if (result.text == s[29]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.financiera_solar)
                    fecha.text = ft
                    hora.text = ht
                    services = s[29]
                }
                if (result.text == s[30]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.financiera_solar)
                    fecha.text = ft
                    hora.text = ht
                    services = s[30]
                }
                if (result.text == s[31]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.interfisa_prestamo)
                    fecha.text = ft
                    hora.text = ht
                    services = s[31]
                }
                if (result.text == s[32]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.banco_itau)
                    fecha.text = ft
                    hora.text = ht
                    services = s[32]
                }
                if (result.text == s[33]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Prestamo"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.universitaria)
                    fecha.text = ft
                    hora.text = ht
                    services = s[33]
                }
                if (result.text == s[34]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Primeros 8 digitos"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.universitaria)
                    fecha.text = ft
                    hora.text = ht
                    services = s[34]
                }
                if (result.text == s[35]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Primeros 10 digitos"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.universitaria)
                    fecha.text = ft
                    hora.text = ht
                    services = s[35]
                }
                if (result.text == s[36]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Primeros 8 digitos"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.universitaria)
                    fecha.text = ft
                    hora.text = ht
                    services = s[36]
                }
                if (result.text == s[37]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.copexsanjo)
                    fecha.text = ft
                    hora.text = ht
                    services = s[37]
                }
                if (result.text == s[38]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.copexsanjo)
                    fecha.text = ft
                    hora.text = ht
                    services = s[38]
                }
                if (result.text == s[39]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.copexsanjo)
                    fecha.text = ft
                    hora.text = ht
                    services = s[39]
                }
                if (result.text == s[40]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.copexsanjo)
                    fecha.text = ft
                    hora.text = ht
                    services = s[40]
                }
                if (result.text == s[41]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.copexsanjo)
                    fecha.text = ft
                    hora.text = ht
                    services = s[41]
                }
                if (result.text == s[42]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Primeros 10 digitos"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cmcp)
                    fecha.text = ft
                    hora.text = ht
                    services = s[42]
                }
                if (result.text == s[43]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Primeros 10 digitos"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cmcp)
                    fecha.text = ft
                    hora.text = ht
                    services = s[43]
                }
                if (result.text == s[44]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cmcp)
                    fecha.text = ft
                    hora.text = ht
                    services = s[44]
                }
                if (result.text == s[45]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cmcp)
                    fecha.text = ft
                    hora.text = ht
                    services = s[45]
                }
                if (result.text == s[46]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cooperativa_tuparenda)
                    fecha.text = ft
                    hora.text = ht
                    services = s[46]
                }
                if (result.text == s[47]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cooperativa_tuparenda)
                    fecha.text = ft
                    hora.text = ht
                    services = s[47]
                }
                if (result.text == s[48]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cooperativa_san_cristobal)
                    fecha.text = ft
                    hora.text = ht
                    services = s[48]
                }
                if (result.text == s[49]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cooperativa_san_cristobal)
                    fecha.text = ft
                    hora.text = ht
                    services = s[49]
                }
                if (result.text == s[50]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cooperativa_san_cristobal)
                    fecha.text = ft
                    hora.text = ht
                    services = s[50]
                }
                if (result.text == s[51]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cooperativa_san_cristobal)
                    fecha.text = ft
                    hora.text = ht
                    services = s[51]
                }
                if (result.text == s[52]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cooperativa_san_cristobal)
                    fecha.text = ft
                    hora.text = ht
                    services = s[52]
                }
                if (result.text == s[53]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cooperativa_san_cristobal)
                    fecha.text = ft
                    hora.text = ht
                    services = s[53]
                }
                if (result.text == s[54]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cooperativa_san_cristobal)
                    fecha.text = ft
                    hora.text = ht
                    services = s[54]
                }
                if (result.text == s[55]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cooperativa_san_cristobal)
                    fecha.text = ft
                    hora.text = ht
                    services = s[55]
                }
                if (result.text == s[56]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cooperativa_yoayu)
                    fecha.text = ft
                    hora.text = ht
                    services = s[56]
                }
                if (result.text == s[57]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Primeros 10 digitos"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cooperativa_yoayu)
                    fecha.text = ft
                    hora.text = ht
                    services = s[57]
                }
                if (result.text == s[58]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cooperativa_yoayu)
                    fecha.text = ft
                    hora.text = ht
                    services = s[58]
                }
                if (result.text == s[59]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cooperativa_yoayu)
                    fecha.text = ft
                    hora.text = ht
                    services = s[59]
                }
                if (result.text == s[60]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cooperativa_yoayu)
                    fecha.text = ft
                    hora.text = ht
                    services = s[60]
                }
                if (result.text == s[61]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cooperativa_yoayu)
                    fecha.text = ft
                    hora.text = ht
                    services = s[61]
                }
                if (result.text == s[62]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cooperativa_yoayu)
                    fecha.text = ft
                    hora.text = ht
                    services = s[62]
                }
                if (result.text == s[63]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cooperativa_comecipar)
                    fecha.text = ft
                    hora.text = ht
                    services = s[63]
                }
                if (result.text == s[64]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cooperativa_comecipar)
                    fecha.text = ft
                    hora.text = ht
                    services = s[64]
                }
                if (result.text == s[65]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Primeros 8 digitos"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cooperativa_comecipar)
                    fecha.text = ft
                    hora.text = ht
                    services = s[65]
                }
                if (result.text == s[66]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Primeros 8 digitos"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cooperativa_comecipar)
                    fecha.text = ft
                    hora.text = ht
                    services = s[66]
                }
                if (result.text == s[67]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Primeros 10 digitos"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cooperativa_comecipar)
                    fecha.text = ft
                    hora.text = ht
                    services = s[67]
                }
                if (result.text == s[68]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cooperativa_comecipar)
                    fecha.text = ft
                    hora.text = ht
                    services = s[68]
                }
                if (result.text == s[69]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cooperativa_medalla_milagrosa)
                    fecha.text = ft
                    hora.text = ht
                    services = s[69]
                }
                if (result.text == s[70]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de Socio"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cooperativa_medalla_milagrosa)
                    fecha.text = ft
                    hora.text = ht
                    services = s[70]
                }
                if (result.text == s[71]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cooperativa_medalla_milagrosa)
                    fecha.text = ft
                    hora.text = ht
                    services = s[71]
                }
                if (result.text == s[72]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de CI"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cooperativa_medalla_milagrosa)
                    fecha.text = ft
                    hora.text = ht
                    services = s[72]
                }
                if (result.text == s[73]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Primeros 10 digitos"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cooperativa_medalla_milagrosa)
                    fecha.text = ft
                    hora.text = ht
                    services = s[73]
                }
                if (result.text == s[74]) {
                    ingnumerodcelular.visibility = View.INVISIBLE
                    txtnumer_giros.visibility = View.INVISIBLE
                    view_monto.visibility = View.INVISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    consulta_bt.visibility = View.VISIBLE
                    view_cedula.text = "Ingrese el Nro de Socio"
                    txtcedula_giros.inputType = InputType.TYPE_CLASS_NUMBER
                    imageView.setImageResource(R.drawable.cooperativa_medalla_milagrosa)
                    fecha.text = ft
                    hora.text = ht
                    services = s[74]
                }
                if (result.text == s[75]) {
                    ingnumerodcelular.visibility = View.VISIBLE
                    txtnumer_giros.visibility = View.VISIBLE
                    view_cedula.visibility = View.VISIBLE
                    txtcedula_giros.visibility = View.VISIBLE
                    view_monto.visibility = View.VISIBLE
                    txtmonto_giros.visibility = View.INVISIBLE
                    consulta_bt.visibility = View.INVISIBLE
                    view_cedula.text = "Ingrese el numero de cedula"
                    view_monto.text = "Ingrese fecha de nacimiento"
                    txtnacimiento.visibility = View.VISIBLE
                    cS.clone(cl)
                    cS.connect(view_cedula.id, ConstraintSet.TOP, txtnumer_giros.id, ConstraintSet.BOTTOM, 30)
                    cS.applyTo(cl)
                    imageView.setImageResource(R.drawable.tigo_cuadrado)
                    fecha.text = ft
                    hora.text = ht
                    services = s[75]
                }

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
        swipe.setOnRefreshListener {
            finish()
        }
    }

    private fun makeCallWithSIM(phoneNumber: String, SIM: Int) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE),
                1
            )
            return
        }

        try {
            val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
            val phoneAccountHandles = telecomManager.callCapablePhoneAccounts

            if (phoneAccountHandles.isNotEmpty()) {
                // Usar la SIM 1 (primer PhoneAccountHandle)
                val sim1 = phoneAccountHandles[SIM]

                val callIntent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                    putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, sim1)
                }

                startActivity(callIntent)
            } else {
                Toast.makeText(this, "No se encontraron SIMs disponibles", Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(
                this,
                "Permisos insuficientes para realizar la llamada",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("AutoCopiado", text)
        clipboardManager.setPrimaryClip(clip)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permiso otorgado
        } else {
            Toast.makeText(
                this,
                "No se otorgaron permisos para realizar la llamada",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @SuppressLint("SuspiciousIndentation", "CutPasteId")
    fun llamar(view: View) {
        val c = findViewById<EditText>(R.id.txtcedula_giros).text.toString()
        val n = findViewById<EditText>(R.id.txtnumer_giros).text.toString()
        val txtmonto = findViewById<EditText>(R.id.txtmonto_giros).text.toString()
        val m = findViewById<EditText>(R.id.txtmonto_giros).text.toString().replace(",", "").toIntOrNull()
        val y =findViewById<EditText>(R.id.txtnacimiento).text.toString()
        val servicesMap = mapOf(
            s[4] to "*555*5*1*1*2*",
            s[5] to "*555*5*1*1*3*",
            s[6] to "*555*5*1*1*4*",
            s[7] to "*555*5*1*2*1*",
            s[8] to "*555*5*1*2*2*",
            s[9] to "*555*5*1*2*4*",
            s[13] to "*555*5*1*3*1*",
            s[14] to "*555*5*1*3*2*",
            s[15] to "*555*5*1*3*3*",
            s[16] to "*555*5*1*3*4*",
            s[17] to "*555*5*1*3*5*",
            s[18] to "*555*5*1*4*1*",
            s[19] to "*555*5*1*4*2*",
            s[20] to "*555*5*1*4*3*",
            s[21] to "*555*5*1*4*4*",
            s[22] to "*555*5*1*4*5*",
            s[23] to "*555*5*1*4*6*",
            s[24] to "*555*5*1*4*7*",
            s[25] to "*555*5*1*4*8*",
            s[26] to "*555*5*1*4*9*",
            s[27] to "*555*5*1*4*10*",
            s[28] to "*555*5*1*4*11*",
            s[29] to "*555*5*1*4*12*",
            s[30] to "*555*5*1*4*13*",
            s[31] to "*555*5*1*4*14*",
            s[32] to "*555*5*1*4*15*",
            s[33] to "*555*5*1*5*1*1*",
            s[34] to "*555*5*1*5*1*2*",
            s[35] to "*555*5*1*5*1*3*",
            s[36] to "*555*5*1*5*1*4*",
            s[37] to "*555*5*1*5*2*1*",
            s[38] to "*555*5*1*5*2*2*",
            s[39] to "*555*5*1*5*2*3*",
            s[40] to "*555*5*1*5*2*4*",
            s[41] to "*555*5*1*5*2*5*",
            s[42] to "*555*5*1*5*3*1*",
            s[43] to "*555*5*1*5*3*2*",
            s[44] to "*555*5*1*5*3*3*",
            s[45] to "*555*5*1*5*3*4*",
            s[46] to "*555*5*1*5*4*1*",
            s[47] to "*555*5*1*5*4*2*",
            s[48] to "*555*5*1*5*5*1*",
            s[49] to "*555*5*1*5*5*2*",
            s[50] to "*555*5*1*5*5*3*",
            s[51] to "*555*5*1*5*5*4*",
            s[52] to "*555*5*1*5*5*5*",
            s[53] to "*555*5*1*5*5*6*",
            s[54] to "*555*5*1*5*5*7*",
            s[55] to "*555*5*1*5*5*8*",
            s[56] to "*555*5*1*5*6*1*",
            s[57] to "*555*5*1*5*6*2*",
            s[58] to "*555*5*1*5*6*3*",
            s[59] to "*555*5*1*5*6*4*",
            s[60] to "*555*5*1*5*6*5*",
            s[61] to "*555*5*1*5*6*6*",
            s[62] to "*555*5*1*5*6*7*",
            s[63] to "*555*5*1*5*7*1*",
            s[64] to "*555*5*1*5*7*2*",
            s[65] to "*555*5*1*5*7*3*",
            s[66] to "*555*5*1*5*7*4*",
            s[67] to "*555*5*1*5*7*5*",
            s[68] to "*555*5*1*5*7*6*",
            s[69] to "*555*5*1*5*8*1*",
            s[70] to "*555*5*1*5*8*2*",
            s[71] to "*555*5*1*5*8*3*",
            s[72] to "*555*5*1*5*8*4*",
            s[73] to "*555*5*1*5*8*5*",
            s[74] to "*555*5*1*5*8*6*",
        )
        val servicesCode = servicesMap[services]
        if (servicesCode != null) {
            // Llamar a la función handleServiceCall si el servicio fue encontrado
            callServices(services, servicesCode, c)
        } else {
            if (services == s[0]) {
                if (n.length < 10 || c.length < 5) {
                    Toast.makeText(this, "Complete los datos porfavor", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    val phoneNumber = "*555*1*$n*$c*1*${m}${Uri.encode("#")}"
                    makeCallWithSIM(phoneNumber, 0)
                    copyToClipboard("${m}")
                    copyToClipboard(c)
                    copyToClipboard(n)
                    startSearch("Giros")

                }
            } // Giros Tigo
            if (services == s[1]) {
                if (n.length < 10 || c.length < 5) {
                    Toast.makeText(this, "Complete los datos porfavor", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    val phoneNumber = "*555*2*$n*$c*1*${m}${Uri.encode("#")}"
                    makeCallWithSIM(phoneNumber, 0)

                    copyToClipboard("${m}")
                    copyToClipboard(c)
                    copyToClipboard(n)
                    startSearch("Retiros")
                }
            } // Retiros Tigo
            if (services == s[2]) {
                if (n.length < 10 || c.length < 5) {
                    Toast.makeText(this, "Complete los datos porfavor", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    val phoneNumber = "*555*3*1*$c*1*$n*" + txtmonto.replace(",", "")
                        .toIntOrNull() + Uri.encode("#")
                    makeCallWithSIM(phoneNumber, 0)

                    copyToClipboard("${m}")
                    copyToClipboard(c)
                    copyToClipboard(n)
                    startSearch("Billetera")
                }
            } // Carga Billetera Tigo
            if (services == s[3]) {
                if (n.length < 10) {
                    Toast.makeText(this, "Complete los datos porfavor", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    val ussdCode = "*555*5*1*1*1*"
                    val phoneNumber = "$ussdCode$n*$n*" + Uri.encode("#")
                    makeCallWithSIM(phoneNumber, 0)
                    copyToClipboard(n)
                    startSearch("Servicio")
                }
            } // Telefonia Tigo
            if (services == s[10]) {
                if (n.length < 10 || txtmonto.length < 4) {
                    Toast.makeText(this, "Complete los datos porfavor", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    val phoneNumber = "*200*3*$n*${m}${Uri.encode("#")}"
                    makeCallWithSIM(phoneNumber, 1)
                    copyToClipboard("${m}")
                    copyToClipboard(n)
                    startSearch("Billetera Personal")

                }
            } // Carga Billetera Personal
            if (services == s[11]) {
                if (n.length < 10 || txtmonto.length < 4) {
                    Toast.makeText(this, "Complete los datos porfavor", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    val phoneNumber = "*200*2*$n*${m}${Uri.encode("#")}"
                    makeCallWithSIM(phoneNumber, 1)
                    copyToClipboard("${m}")
                    copyToClipboard(n)
                    startSearch(s[11])
                }
            } // Retiros Personal
            if (services == s[12]) {
                if (n.length < 10 || txtmonto.length < 4) {
                    Toast.makeText(this, "Complete los datos porfavor", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    val phoneNumber = "*200*4*$n*${m}${Uri.encode("#")}"
                    makeCallWithSIM(phoneNumber, 1)
                    copyToClipboard("${m}")
                    copyToClipboard(n)
                    startSearch(s[12])
                }
            } // Telefonia Personal
            if (services == s[75]) {
                if (n.length < 10 || c.length < 5 || y.length < 8) {
                    Toast.makeText(this, "Complete los datos porfavor", Toast.LENGTH_SHORT)
                        .show()
                } else{
                    val ussdCode = "*555*6*3*"
                    val phoneNumber = "$ussdCode$n*1*$c*$y*" + Uri.encode("#")
                    makeCallWithSIM(phoneNumber, 0)
                    copyToClipboard(n)
                }

            } // Reseteo de Pin (Cliente)
        }
    }

    fun manual_bt(view: View) {
        val c = findViewById<EditText>(R.id.txtcedula_giros).text.toString()
        val n = findViewById<EditText>(R.id.txtnumer_giros).text.toString()
        val servicesMap = mapOf(
            s[4] to "*555*5*1*1*2*",
            s[5] to "*555*5*1*1*3*",
            s[6] to "*555*5*1*1*4*",
            s[7] to "*555*5*1*2*1*",
            s[8] to "*555*5*1*2*2*",
            s[9] to "*555*5*1*2*4*",
            s[13] to "*555*5*1*3*1*",
            s[14] to "*555*5*1*3*2*",
            s[15] to "*555*5*1*3*3*",
            s[16] to "*555*5*1*3*4*",
            s[17] to "*555*5*1*3*5*",
            s[18] to "*555*5*1*4*1*",
            s[19] to "*555*5*1*4*2*",
            s[20] to "*555*5*1*4*3*",
            s[21] to "*555*5*1*4*4*",
            s[22] to "*555*5*1*4*5*",
            s[23] to "*555*5*1*4*6*",
            s[24] to "*555*5*1*4*7*",
            s[25] to "*555*5*1*4*8*",
            s[26] to "*555*5*1*4*9*",
            s[27] to "*555*5*1*4*10*",
            s[28] to "*555*5*1*4*11*",
            s[29] to "*555*5*1*4*12*",
            s[30] to "*555*5*1*4*13*",
            s[31] to "*555*5*1*4*14*",
            s[32] to "*555*5*1*4*15*",
            s[33] to "*555*5*1*5*1*1*",
            s[34] to "*555*5*1*5*1*2*",
            s[35] to "*555*5*1*5*1*3*",
            s[36] to "*555*5*1*5*1*4*",
            s[37] to "*555*5*1*5*2*1*",
            s[38] to "*555*5*1*5*2*2*",
            s[39] to "*555*5*1*5*2*3*",
            s[40] to "*555*5*1*5*2*4*",
            s[41] to "*555*5*1*5*2*5*",
            s[42] to "*555*5*1*5*3*1*",
            s[43] to "*555*5*1*5*3*2*",
            s[44] to "*555*5*1*5*3*3*",
            s[45] to "*555*5*1*5*3*4*",
            s[46] to "*555*5*1*5*4*1*",
            s[47] to "*555*5*1*5*4*2*",
            s[48] to "*555*5*1*5*5*1*",
            s[49] to "*555*5*1*5*5*2*",
            s[50] to "*555*5*1*5*5*3*",
            s[51] to "*555*5*1*5*5*4*",
            s[52] to "*555*5*1*5*5*5*",
            s[53] to "*555*5*1*5*5*6*",
            s[54] to "*555*5*1*5*5*7*",
            s[55] to "*555*5*1*5*5*8*",
            s[56] to "*555*5*1*5*6*1*",
            s[57] to "*555*5*1*5*6*2*",
            s[58] to "*555*5*1*5*6*3*",
            s[59] to "*555*5*1*5*6*4*",
            s[60] to "*555*5*1*5*6*5*",
            s[61] to "*555*5*1*5*6*6*",
            s[62] to "*555*5*1*5*6*7*",
            s[63] to "*555*5*1*5*7*1*",
            s[64] to "*555*5*1*5*7*2*",
            s[65] to "*555*5*1*5*7*3*",
            s[66] to "*555*5*1*5*7*4*",
            s[67] to "*555*5*1*5*7*5*",
            s[68] to "*555*5*1*5*7*6*",
            s[69] to "*555*5*1*5*8*1*",
            s[70] to "*555*5*1*5*8*2*",
            s[71] to "*555*5*1*5*8*3*",
            s[72] to "*555*5*1*5*8*4*",
            s[73] to "*555*5*1*5*8*5*",
            s[74] to "*555*5*1*5*8*6*",
        )
        val servicesCode = servicesMap[services]
        if (servicesCode != null) {
            // Llamar a la función handleServiceCall si el servicio fue encontrado
            callServicesManual(services, servicesCode, c)
        } else {
            if (services == s[12]) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("smsto:131")
                intent.putExtra("sms_body", txtnumer_giros.text.toString())
                startActivity(intent)
            } // Telefonia Personal
            if (services == s[3]) {
                if (n.length < 10) {
                    Toast.makeText(this, "Complete los datos porfavor", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    val ussdCode = "*555*5*1*1*1*"
                    val phoneNumber = ussdCode + Uri.encode("#")
                    makeCallWithSIM(phoneNumber, 0)
                    copyToClipboard(n)
                    startSearch("Servicio")
                }
            } // Telefonia Tigo
        }
    }

    private fun readFromFile(): List<Map<String, String>> {
        val history = mutableListOf<Map<String, String>>()
        val fileInputStream: FileInputStream
        try {
            fileInputStream = openFileInput(fileName)
            val inputString = fileInputStream.bufferedReader().use { it.readText() }
            fileInputStream.close()

            // Procesar el contenido del archivo
            inputString.lines().forEach { line ->
                val parts = line.split(",")
                if (parts.size == 5) {
                    history.add(
                        mapOf(
                            "numero" to parts[0],
                            "cedula" to parts[1],
                            "monto" to parts[2],
                            "fecha" to parts[3],
                            "hora" to parts[4]
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace() // Manejo de errores
        }
        return history
    }

    private fun saveToFile(
        numero: String?,
        cedula: String?,
        monto: String?,
        fecha: String,
        hora: String
    ) {
        val visibleNumero =
            if (txtnumer_giros.visibility == View.VISIBLE) numero?.takeIf { it != "null" }
                ?: "" else ""
        val visibleCedula =
            if (txtcedula_giros.visibility == View.VISIBLE) cedula?.takeIf { it != "null" }
                ?: "" else ""
        val visibleMonto =
            if (txtmonto_giros.visibility == View.VISIBLE) monto?.takeIf { it != "null" }
                ?: "" else ""

        val data = "$visibleNumero,$visibleCedula,$visibleMonto,$fecha,$hora\n"

        if (data.trim().isNotEmpty()) {
            val fileOutputStream: FileOutputStream
            try {
                fileOutputStream = openFileOutput(fileName, Context.MODE_APPEND)
                fileOutputStream.write(data.toByteArray())
                fileOutputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadFromFile(history: List<Map<String, String>>, index: Int) {
        if (history.isNotEmpty() && index in history.indices) {
            val entry = history[index]
            txtnumer_giros.setText(entry["numero"]?.takeIf { it != "null" } ?: "")
            txtcedula_giros.setText(entry["cedula"]?.takeIf { it != "null" } ?: "")
            txtmonto_giros.setText(entry["monto"]?.takeIf { it != "null" } ?: "")
            fecha.text = entry["fecha"]?.takeIf { it != "null" } ?: ""
            hora.text = entry["hora"]?.takeIf { it != "null" } ?: ""
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        saveDataLogic()
        currentIndex = 0
        cancelSearch()
        startActivity(Intent(this@MainActivity, SearchServices::class.java))
    }

    override fun onDestroy() {
        saveDataLogic()
        currentIndex = 0
        startActivity(Intent(this@MainActivity, SearchServices::class.java))
        super.onDestroy()
        cancelSearch()
    }

    private fun startSearch(refType: String) {
        if (isSearching) return
        isSearching = true
        lastSmsTimestamp = getLastSmsTimestamp()
        Log.d("MainActivity", "Búsqueda iniciada")
        Toast.makeText(this, "Búsqueda iniciada", Toast.LENGTH_SHORT).show()

        // Iniciar la observación de nuevos mensajes
        smsObserver = object : ContentObserver(android.os.Handler()) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                searchInMessages(refType)
            }
        }
        contentResolver.registerContentObserver(
            Telephony.Sms.CONTENT_URI,
            true,
            smsObserver!!
        )

        // Iniciar una búsqueda inicial
        searchInMessages(refType)
    }

    private fun extractRequiredData(response: String, refType: String): ReferenceData? {
        val ref1Regex = Regex("""Ref 1: (\d+)""")
        val ref2Regex = Regex("""Ref 2: (\d+)""")
        val codigoReferenciaRegex = Regex("""Codigo de referencia: (\d+)""")
        val comprobantePersonalRegex = Regex("""Su comprobante es (\d+)""")
        val comprobanteRetirosRegex = Regex("""Comprobante (\d+)""")
        val depositoBilleteraRegex = Regex("""Deposito nro\. (\d+)""")
        val montoYRefRegex = Regex("""Monto PYG ([\d.]+)[^R]*Ref[ .:]+(\d+)""")
        val ref1Match = ref1Regex.find(response)
        val ref2Match = ref2Regex.find(response)
        val codigoRefMatch = codigoReferenciaRegex.find(response)
        val comprobantePersonalMatch = comprobantePersonalRegex.find(response)
        val comprobanteRetirosMatch = comprobanteRetirosRegex.find(response)
        val depositoBilleteraMatch = depositoBilleteraRegex.find(response)
        val montoYRefMatch = montoYRefRegex.find(response)
        return when (refType) {
            "Giros" -> {
                // Verificar si Ref 1 y Ref 2 están presentes
                if (ref1Match != null && ref2Match != null) {
                    val ref1 = ref1Match.groupValues[1]
                    val ref2 = ref2Match.groupValues[1]
                    ReferenceData(ref1, ref2)
                } else {
                    null // No se encontraron ambas referencias
                }
            }

            "Servicio" -> {
                // Buscar en el formato "Monto PYG 210.000. Ref. 1234"
                montoYRefMatch?.let {
                    val monto = it.groupValues[1].replace(".", "") // Eliminar los puntos del monto
                    val ref = it.groupValues[2]
                    ReferenceData(monto, ref) // Monto como ref1, Ref como ref2
                }
            }

            "Billetera" -> {
                // Permitir que Ref 1 esté vacío, pero Ref 2 debe estar presente
                val ref1 = ref1Match?.groupValues?.get(1) ?: ""
                ref2Match?.groupValues?.get(1)?.let { ref2 ->
                    ReferenceData(ref1, ref2)
                }
            }

            "Retiros" -> {
                // Buscar solo Codigo de referencia: 123
                codigoRefMatch?.groupValues?.get(1)?.let {
                    ReferenceData(it, "") // Solo una referencia
                }
            }

            s[12] -> {
                // Buscar la referencia después de "Su comprobante es"
                comprobantePersonalMatch?.groupValues?.get(1)?.let {
                    ReferenceData(it, "") // Solo una referencia
                }
            }

            s[11] -> {
                // Buscar la referencia después de "Comprobante"
                comprobanteRetirosMatch?.groupValues?.get(1)?.let {
                    ReferenceData(it, "") // Solo una referencia
                }
            }

            "Billetera Personal" -> {
                // Buscar la referencia después de "Deposito nro."
                depositoBilleteraMatch?.groupValues?.get(1)?.let {
                    ReferenceData(it, "") // Solo una referencia
                }
            }

            else -> null
        }
    }

    private fun showConfirmationDialog(data: ReferenceData?) {
        val n = txtnumer_giros.text.toString()
        val c = txtcedula_giros.text.toString()
        val format = txtmonto_giros.text.toString()
        val m = format.replace(",", "").toIntOrNull()
        val currentDateTime = LocalDateTime.now()
        val f = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        val h = DateTimeFormatter.ofPattern("HH:mm:ss")
        val fecha = currentDateTime.format(f)
        val hora = currentDateTime.format(h)
        val fees = m?.times(0.06)?.toFloat()
        var Mensaje = ""
        if (services == s[0] || services == s[1]) {
            Mensaje =
                "=====================\nGenio Tecni\n${services}\nFecha: ${fecha} \nHora: ${hora}\nNumero: ${n}\nCedula: ${c}\nMonto: ${m} Gs.\nComision: ${fees} Gs.\nRef1: ${data?.ref1 ?: ""}\nRef2: ${data?.ref2 ?: ""}\n=====================\n"
        }
        if (services == s[10] || services == s[11]) {
            Mensaje =
                "=====================\nGenio Tecni\n${services}\nFecha: ${fecha} \nHora: ${hora}\nNumero: ${n}\nMonto: ${m} Gs.\nRef1: ${data?.ref1 ?: ""}\n=====================\n"
        }
        if (services == s[12]) {
            Mensaje =
                "=====================\nGenio Tecni\n${services}\nFecha: ${fecha} \nHora: ${hora}\nNumero: ${n}\nMonto: ${m} Gs.\nRef1: ${data?.ref1 ?: ""}\n=====================\n"
        }
        if (services == s[2]) {
            Mensaje =
                "=====================\nGenio Tecni\n${services}\nFecha: ${fecha} \nHora: ${hora}\nNumero: ${n}\nCedula: ${c}\nMonto: ${m} Gs.\nRef1: ${data?.ref1 ?: ""}\nRef2: ${data?.ref2 ?: ""}\n=====================\n"
        }
        if (services == s[8]) {
            Mensaje =
                "=====================\nGenio Tecni\n${services}\nFecha: ${fecha} \nHora: ${hora}\nISSAN: ${c}\nMonto: ${data?.ref1 ?: ""} Gs.\nRef1: ${data?.ref2 ?: ""}\n=====================\n"
        }
        if (services == s[3]) {
            Mensaje =
                "=====================\nGenio Tecni\n${services}\nFecha: ${fecha} \nHora: ${hora}\nNumero: ${n}\nMonto: ${data?.ref1 ?: ""} Gs.\nRef1: ${data?.ref2 ?: ""}\n=====================\n"
        }
        if (services == s[4] || services == s[5] || services == s[6]) {
            Mensaje =
                "=====================\nGenio Tecni\n${services}\nFecha: ${fecha} \nHora: ${hora}\nCI o Contrato: ${c}\nMonto: ${data?.ref1 ?: ""} Gs.\nRef1: ${data?.ref2 ?: ""}\n=====================\n"
        }
        if (services == s[9]) {
            Mensaje =
                "=====================\nGenio Tecni\n${services}\nFecha: ${fecha} \nHora: ${hora}\nTelefono o Cuenta: ${c}\nMonto: ${data?.ref1 ?: ""} Gs.\nRef1: ${data?.ref2 ?: ""}\n=====================\n"
        }
        if (services == s[7]) {
            Mensaje =
                "=====================\nGenio Tecni\n${services}\nFecha: ${fecha} \nHora: ${hora}\nNIS: ${c}\nMonto: ${data?.ref1 ?: ""} Gs.\nRef1: ${data?.ref2 ?: ""}\n=====================\n"
        }
        if (services == s[14] || services == s[15] || services == s[17] || services == s[16]
            || services == s[13] || services == s[18] || services == s[19]
            || services == s[20] || services == s[21] || services == s[22]
            || services == s[23] || services == s[24] || services == s[25]
            || services == s[26] || services == s[27] || services == s[28]
            || services == s[29] || services == s[30] || services == s[31]
            || services == s[32] || services == s[33] || services == s[34] || services == s[35] || services == s[36] || services == s[37] || services == s[38] || services == s[39] || services == s[40] || services == s[41] || services == s[42] || services == s[43] || services == s[44] || services == s[45] || services == s[46] || services == s[47] || services == s[48] || services == s[49] || services == s[50] || services == s[51] || services == s[52] ||
            services == s[53] || services == s[54] || services == s[55] ||
            services == s[56] || services == s[57] || services == s[58] || services == s[59] || services == s[60] ||
            services == s[61] || services == s[62] || services == s[63] || services == s[64] || services == s[65] || services == s[66] || services == s[67] ||
            services == s[68] || services == s[69] || services == s[70] || services == s[71] || services == s[72] || services == s[73] ||
            services == s[74]
        ) {
            Mensaje =
                "=====================\nGenio Tecni\n${services}\nFecha: ${fecha} \nHora: ${hora}\nCI: ${c}\nMonto: ${data?.ref1 ?: ""} Gs.\nRef1: ${data?.ref2 ?: ""}\n=====================\n"
        }
        runOnUiThread {
            requestBt()
            saveDataLogic()
            val printData = PrintData(
                service = services,
                date = fecha,
                time = hora,
                message = Mensaje,
                referenceData = data ?: ReferenceData("", "")
            )
            PrintDataManager(this).savePrintData(printData)
            AlertDialog.Builder(this)
                .setTitle("Confirmar impresión")
                .setMessage("Se encontraron los siguientes datos:\n${Mensaje}¿Deseas continuar con la impresión?")
                .setPositiveButton("Sí") { _, _ ->
                    printData(Mensaje, data)
                    cancelSearch()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                    cancelSearch()
                }
                .show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun printData(data: String, referenceData: ReferenceData?, retryCount: Int = 3) {
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val deviceAddress = sharedPreferences.getString("BluetoothDeviceAddress", null)
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val device = deviceAddress?.let { bluetoothAdapter?.getRemoteDevice(it) }
        if (deviceAddress == null || device == null) {
            Toast.makeText(
                this,
                "No se encontró un dispositivo Bluetooth seleccionado, configurelo!",
                Toast.LENGTH_SHORT
            ).show()
            startActivity(Intent(this, Bt::class.java))
            return
        }
        var attempts = retryCount
        while (attempts > 0) {
            try {
                val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                val socket = device.createRfcommSocketToServiceRecord(uuid)
                socket.connect()
                val outputStream = socket.outputStream
                val alignCenterCommand = byteArrayOf(0x1B, 0x61, 0x01)
                outputStream.write(alignCenterCommand)
                val largeFontCommand = byteArrayOf(0x1D, 0x21, 0x11)
                outputStream.write(largeFontCommand)
                outputStream.write(data.toByteArray())
                outputStream.flush()
                val fontACommand = byteArrayOf(0x1B, 0x21, 0x00)
                outputStream.write(fontACommand)
                outputStream.write("\n,\n".toByteArray())
                outputStream.flush()
                outputStream.close()
                socket.close()
                Toast.makeText(this, "Impresión completada", Toast.LENGTH_SHORT).show()
                return
            } catch (e: IOException) {
                e.printStackTrace()
                attempts--
                Toast.makeText(
                    this,
                    "Fallo al imprimir, reintentando (${retryCount - attempts}/$retryCount)",
                    Toast.LENGTH_LONG
                ).show()
                if (attempts == 0) {
                    showConfirmationDialog(referenceData)
                    break
                }
            }
        }
    }

    private fun cancelSearch() {
        isSearching = false
        smsObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
        searchJob?.cancel()
        Log.d("MainActivity", "Búsqueda cancelada")
    }

    private fun requestBt() {
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val deviceAddress = sharedPreferences.getString("BluetoothDeviceAddress", null)
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val device = deviceAddress?.let { bluetoothAdapter?.getRemoteDevice(it) }
        if (deviceAddress == null || device == null) {
            Toast.makeText(
                this,
                "No se encontró un dispositivo Bluetooth seleccionado, configurelo!",
                Toast.LENGTH_SHORT
            ).show()
            startActivity(Intent(this, Bt::class.java))
        }
    }

    private fun getLastSmsTimestamp(): Long {
        val smsUri: Uri = Telephony.Sms.Inbox.CONTENT_URI
        val cursor: Cursor? =
            contentResolver.query(smsUri, null, null, null, Telephony.Sms.DEFAULT_SORT_ORDER)

        cursor?.use {
            if (it.moveToFirst()) {
                val dateColumnIndex = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
                return it.getLong(dateColumnIndex)
            }
        }
        return 0L
    }

    private fun searchInMessages(refType: String) {
        if (!isSearching) return
        searchJob = CoroutineScope(Dispatchers.Default).launch {
            val smsUri: Uri = Telephony.Sms.Inbox.CONTENT_URI
            val selection = "${Telephony.Sms.DATE} > ? AND ${Telephony.Sms.ADDRESS} IN (?, ?)"
            val selectionArgs = arrayOf(lastSmsTimestamp.toString(), "555", "55")
            val cursor: Cursor? = contentResolver.query(
                smsUri,
                null,
                selection,
                selectionArgs,
                "${Telephony.Sms.DATE} DESC"
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    do {
                        val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
                        val date = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                        if (date > lastSmsTimestamp) {
                            lastSmsTimestamp = date
                            val extractedData = extractRequiredData(body, refType)
                            if (extractedData != null) {
                                withContext(Dispatchers.Main) {
                                    showConfirmationDialog(extractedData)
                                }
                                cancelSearch()
                                return@use
                            }
                        }
                    } while (it.moveToNext() && isSearching)
                }
            }
        }
    }

    private fun requestCall() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), 1)
        }
    }

    private fun requestSms() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_SMS), 1)
        }
    }

    private fun saveDataLogic() {
        val n = txtnumer_giros.text.toString()
        val c = txtcedula_giros.text.toString()
        if ((services == s[0] || services == s[1] || services == s[2] || services == s[3]) || services == s[10] || services == s[11] || services == s[12] && n != "09" || n != "") {
            saveData()
        }
        if ((services == s[4] || services == s[5] || services == s[7] || services == s[9] || services == s[6] || services == s[14] || services == s[15] || services == s[17]
                    || services == s[16] || services == s[13] || services == s[18] || services == s[19]
                    || services == s[20] || services == s[21] || services == s[22]
                    || services == s[23] || services == s[24] || services == s[25]
                    || services == s[26] || services == s[27] || services == s[28]
                    || services == s[29] || services == s[30] || services == s[31]
                    || services == s[32] || services == s[33] || services == s[34] || services == s[35] || services == s[36] || services == s[37] || services == s[38] || services == s[39] || services == s[40] || services == s[41] || services == s[42] || services == s[43] || services == s[44] || services == s[45] || services == s[46] || services == s[47] || services == s[48] || services == s[49] || services == s[50] || services == s[51] || services == s[52] ||
                    services == s[53] || services == s[54] || services == s[55] ||
                    services == s[56] || services == s[57] || services == s[58] || services == s[59] || services == s[60] ||
                    services == s[61] || services == s[62] || services == s[63] || services == s[64] || services == s[65] || services == s[66] || services == s[67] ||
                    services == s[68] || services == s[69] || services == s[70] || services == s[71] || services == s[72] || services == s[73] ||
                    services == s[74]) && c != ""
        ) {
            saveData()
        }
        if (services == s[8] && c != "ZV" || c != "") {
            saveData()
        }
        if ((services == s[75])|| n != "09" && n != "") {
            saveData()
        }
    }

    private fun saveData() {
        val n = txtnumer_giros.text.toString()
        val c = txtcedula_giros.text.toString()
        val format = txtmonto_giros.text.toString()
        val m = format.replace(",", "").toIntOrNull()
        val currentDateTime = LocalDateTime.now()
        val f = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        val h = DateTimeFormatter.ofPattern("HH:mm:ss")
        val fecha = currentDateTime.format(f)
        val hora = currentDateTime.format(h)
        saveToFile(n, c, m.toString(), fecha, hora)
    }

    private fun callServices(serviceName: String, ussdCode: String, c: String) {
        if (c.length < 3) {
            Toast.makeText(this, "Complete los datos por favor", Toast.LENGTH_SHORT).show()
        } else {
            val phoneNumber = "$ussdCode$c*$c*" + Uri.encode("#")
            makeCallWithSIM(phoneNumber, 0)
            copyToClipboard(c)
            startSearch("Servicio")
        }
    }

    private fun callServicesManual(serviceName: String, ussdCode: String, c: String) {
        if (c.length < 3) {
            Toast.makeText(this, "Complete los datos por favor", Toast.LENGTH_SHORT).show()
        } else {
            val phoneNumber = ussdCode + Uri.encode("#")
            makeCallWithSIM(phoneNumber, 0)
            copyToClipboard(c)
            startSearch("Servicio")
        }
    }
}