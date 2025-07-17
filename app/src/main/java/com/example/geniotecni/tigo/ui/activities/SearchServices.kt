package com.example.geniotecni.tigo.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
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
import com.example.geniotecni.tigo.R
import com.example.geniotecni.tigo.helpers.LoadingAnimationHelper
import com.example.geniotecni.tigo.ui.adapters.ServiceAdapter
import com.example.geniotecni.tigo.utils.Constants

class SearchServices : AppCompatActivity() {

    companion object {
        private const val TAG = "SearchServices"
    }

    private val servicios = Constants.SERVICE_NAMES
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchEditText: EditText
    private lateinit var serviceAdapter: ServiceAdapter
    private lateinit var loadingHelper: LoadingAnimationHelper
    
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "SearchServices onCreate iniciado")
        
        try {
            setContentView(R.layout.activity_main)
            Log.d(TAG, "Layout establecido correctamente")
            
            loadingHelper = LoadingAnimationHelper(this)
            
            requestCall()
            requestSms()
            requestBt()
            
            recyclerView = findViewById(R.id.recyclerView)
            recyclerView.layoutManager = LinearLayoutManager(this)
            Log.d(TAG, "RecyclerView configurado")
            
            serviceAdapter = ServiceAdapter(
                allServices = servicios.toList(),
                onItemClick = { service ->
                    navigateToMainActivity(service)
                }
            )
            recyclerView.adapter = serviceAdapter
            Log.d(TAG, "Adapter configurado con ${servicios.size} servicios")

            searchEditText = findViewById<AutoCompleteTextView>(R.id.autoCompleteTextView)
            searchEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    filterServices(s.toString())
                }

                override fun afterTextChanged(s: Editable?) {}
            })
            Log.d(TAG, "EditText configurado")

            val bt = findViewById<ImageButton>(R.id.bt)
            bt.setOnClickListener {
                loadingHelper.showLoadingAndNavigate(
                    targetActivity = Bt::class.java,
                    message = "Configurando Bluetooth...",
                    duration = 1000L
                )
            }
            
            val print = findViewById<ImageButton>(R.id.print)
            print.setOnClickListener {
                loadingHelper.showLoadingAndNavigate(
                    targetActivity = PrintHistoryActivity::class.java,
                    message = "Cargando historial...",
                    duration = 800L
                )
            }
            
            // Agregar botones de estadísticas y configuración
            val statsButton = findViewById<ImageButton>(R.id.stats)
            statsButton?.setOnClickListener {
                Log.d(TAG, "Navegando a estadísticas")
                loadingHelper.showLoadingAndNavigate(
                    targetActivity = StatisticsActivity::class.java,
                    message = "Calculando estadísticas...",
                    duration = 1500L
                )
            }

            val settingsButton = findViewById<ImageButton>(R.id.settings)
            settingsButton?.setOnClickListener {
                Log.d(TAG, "Navegando a configuración")
                loadingHelper.showLoadingAndNavigate(
                    targetActivity = SettingsActivity::class.java,
                    message = "Abriendo configuración...",
                    duration = 600L
                )
            }
            
            Log.d(TAG, "SearchServices onCreate completado exitosamente")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error en SearchServices onCreate", e)
            throw e
        }
    }

    private fun filterServices(query: String) {
        val filteredServices = servicios.filter { service ->
            service.contains(query, ignoreCase = true)
        }
        serviceAdapter.updateServices(filteredServices)
    }

    private fun navigateToMainActivity(service: String) {
        Log.d(TAG, "Navegando a MainActivity con servicio: $service")
        
        loadingHelper.showLoadingAndNavigate(
            targetActivity = MainActivity::class.java,
            message = "Cargando $service...",
            duration = 1200L
        ) {
            putExtra("selectedService", service)
        }
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













