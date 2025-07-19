package com.example.geniotecni.tigo

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class SearchServices : AppCompatActivity() {

    private val servicios = arrayOf("Giros Tigo",
/*1*/  "Retiros Tigo","Carga Billetera Tigo","Telefonia Tigo","Pago TV e Internet Hogar","Antena (Wimax)",
/*6*/  "Tigo TV anticipado","Reseteo de Pin (Cliente)"/*, "ANDE", "ESSAP","COPACO"*//*,"Carga Billetera Personal",*/
/*11*/ /*"Retiros Personal","Telefonia Personal",*//*"Alex S.A","Electroban","Leopard",
/*16*/ "Chacomer","Inverfin","Che Duo-Carsa (Prestamos)","Banco Familar (Prestamos)","Financiera El Comercio",
/*21*/ "Interfisa (Prestamos)","Financiera Paraguayo Japonesa (Prestamos)","Credito Amigo (Prestamos)","Tu Financiera (Prestamos)","Funacion Industrial (Prestamos)",
/*26*/ "Banco Vision Pago de Tarjetas","Banco Vision Pago de Prestamos","Fiado.Net (Prestamos)","Financiera Solar Pago de Tarjetas","Financiera Solar Pago de Prestamos",
/*31*/ "Interfisa Pago de Tarjetas","Banco Itau (Prestamos)","Cooperativa Universitaria (Prestamos)","Cooperativa Universitaria (Tarjeta Mastercard)","Cooperativa Universitaria (Tarjeta Cabal)",
/*36*/ "Cooperativa Universitaria (Tarjeta Panal)", "CopexSanJo (Tarjeta Credito Visa)","CopexSanJo (Tarjeta Credito Cabal)","CopexSanJo (Solidaridad)","CopexSanJo (Cuotas)",
/*41*/ "CopexSanJo (Aportes)","Caja Mutual De Cooperativistas Del Paraguay CMCP (Tarjeta Cabal)", "Caja Mutual De Cooperativistas Del Paraguay CMCP (Tarjeta Mastercard)","Caja Mutual De Cooperativistas Del Paraguay CMCP (Credito)","Caja Mutual De Cooperativistas Del Paraguay CMCP (Aporte)",
/*46*/ "Cooperativa Tupãrenda (Aporte y Solidaridad)","Cooperativa Tupãrenda (Prestamos)","Cooperativa San Cristobal (Admision)","Cooperativa San Cristobal (Tarjeta Mastercard)", "Cooperativa San Cristobal (Solidaridad)",
/*51*/ "Cooperativa San Cristobal (Aporte)", "Cooperativa San Cristobal (Prestamo)", "Cooperativa San Cristobal (Tarjeta Unica)","Cooperativa San Cristobal (Tarjeta Visa)", "Cooperativa San Cristobal (Tarjeta Credicard)",
/*56*/ "Cooperativa Yoayu (Sepelios)", "Cooperativa Yoayu (Tarjeta Cabal)", "Cooperativa Yoayu (Tarjeta Visa)", "Cooperativa Yoayu (Fondos)","Cooperativa Yoayu (Solidaridad)",
/*61*/ "Cooperativa Yoayu (Aporte)","Cooperativa Yoayu","Cooperativa Coomecipar (Solidaridad)","Cooperativa Coomecipar (Prestamo)","Cooperativa Coomecipar (Tarjeta Mastercard)",
/*66*/ "Cooperativa Coomecipar (Tarjeta Credicard)","Cooperativa Coomecipar (Tarjeta Cabal)","Cooperativa Coomecipar (Aportes)","Cooperativa Medalla Milagrosa (Tarjeta Visa)","Cooperativa Medalla Milagrosa (Solidaridad)",
/*71*/ "Cooperativa Medalla Milagrosa (Tarjeta Mastercard)","Cooperativa Medalla Milagrosa (Tarjeta Credicard)","Cooperativa Medalla Milagrosa (Tarjeta Cabal)","Cooperativa Medalla Milagrosa (Creditos)"*/)
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchEditText: EditText
    private lateinit var serviceAdapter: ServiceAdapter
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestCall()
        requestSms()
        requestBt()
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        serviceAdapter = ServiceAdapter(servicios.toList()) { service ->
            navigateToMainActivity(service)
        }
        recyclerView.adapter = serviceAdapter

        searchEditText = findViewById<AutoCompleteTextView>(R.id.autoCompleteTextView)
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterServices(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        val bt = findViewById<ImageButton>(R.id.bt)
        bt.setOnClickListener {
            startActivity(Intent(this@SearchServices, Bt::class.java))
        }
        val print = findViewById<ImageButton>(R.id.print)
        print.setOnClickListener {
            startActivity(Intent(this@SearchServices, PrintHistoryActivity::class.java))
        }
    }

    private fun filterServices(query: String) {
        val filteredServices = servicios.filter { service ->
            service.contains(query, ignoreCase = true)
        }
        serviceAdapter.updateServices(filteredServices)
    }

    private fun navigateToMainActivity(service: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("selectedService", service)
        startActivity(intent)
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

    private fun requestCall(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), 1)
        }
    }

    private fun requestSms(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_SMS), 1)
        }
    }
}













