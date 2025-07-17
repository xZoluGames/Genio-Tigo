package com.example.geniotecni.tigo.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.geniotecni.tigo.R
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.UUID

class Bt : AppCompatActivity() {

    companion object {
        private const val REQUEST_ENABLE_BT = 1
        private const val PERMISSION_REQUEST_CODE = 101
        private const val PREFS_NAME = "MyPrefs"
        private const val KEY_DEVICE_ADDRESS = "BluetoothDeviceAddress"
        private const val KEY_DEVICE_NAME = "BluetoothDeviceName"
    }

    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var listView: ListView
    private lateinit var searchButton: Button
    private lateinit var refreshButton: Button
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar

    private val deviceList = mutableListOf<BluetoothDevice>()
    private val deviceNames = mutableListOf<String>()
    private lateinit var arrayAdapter: ArrayAdapter<String>

    // BroadcastReceiver para descubrir dispositivos
    private val discoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    device?.let {
                        if (!deviceList.contains(it)) {
                            deviceList.add(it)
                            val deviceName = it.name ?: "Dispositivo desconocido"
                            val deviceInfo = "$deviceName\n${it.address}"
                            deviceNames.add(deviceInfo)
                            arrayAdapter.notifyDataSetChanged()
                        }
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    progressBar.visibility = View.VISIBLE
                    statusText.text = "Buscando dispositivos..."
                    deviceList.clear()
                    deviceNames.clear()
                    arrayAdapter.notifyDataSetChanged()
                    loadPairedDevices()
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    progressBar.visibility = View.GONE
                    statusText.text = "Búsqueda completada. ${deviceList.size} dispositivos encontrados"
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bt_enhanced)

        initializeViews()
        setupBluetooth()
        checkSavedDevice()
    }

    private fun initializeViews() {
        listView = findViewById(R.id.deviceList)
        searchButton = findViewById(R.id.btnSearch)
        refreshButton = findViewById(R.id.btnRefresh)
        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)

        arrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceNames)
        listView.adapter = arrayAdapter

        listView.setOnItemClickListener { _, view, position, _ ->
            if (position < deviceList.size) {
                selectDevice(deviceList[position])
            }
        }

        searchButton.setOnClickListener {
            if (checkBluetoothPermissions()) {
                enableBluetoothAndSearch()
            }
        }

        refreshButton.setOnClickListener {
            if (checkBluetoothPermissions()) {
                startDiscovery()
            }
        }
    }

    private fun setupBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Este dispositivo no soporta Bluetooth", Toast.LENGTH_LONG).show()
            statusText.text = "Bluetooth no disponible"
            searchButton.isEnabled = false
            refreshButton.isEnabled = false
            return
        }

        // Registrar el receiver para discovery
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        registerReceiver(discoveryReceiver, filter)
    }

    private fun checkSavedDevice() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedAddress = sharedPreferences.getString(KEY_DEVICE_ADDRESS, null)
        val savedName = sharedPreferences.getString(KEY_DEVICE_NAME, null)

        if (savedAddress != null && savedName != null) {
            statusText.text = "Dispositivo guardado: $savedName ($savedAddress)"
        }
    }

    private fun checkBluetoothPermissions(): Boolean {
        val permissions = mutableListOf<String>()

        // Permisos base para todas las versiones
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.BLUETOOTH)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        // Permisos adicionales para Android 6.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        // Permisos adicionales para Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }

        return if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
            false
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableBluetoothAndSearch() {
        if (!bluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            loadPairedDevices()
            startDiscovery()
        }
    }

    @SuppressLint("MissingPermission")
    private fun loadPairedDevices() {
        val pairedDevices = bluetoothAdapter?.bondedDevices

        pairedDevices?.forEach { device ->
            if (!deviceList.contains(device)) {
                deviceList.add(device)
                val deviceName = device.name ?: "Dispositivo emparejado"
                val deviceInfo = "$deviceName (Emparejado)\n${device.address}"
                deviceNames.add(deviceInfo)
            }
        }

        arrayAdapter.notifyDataSetChanged()

        if (pairedDevices.isNullOrEmpty()) {
            statusText.text = "No hay dispositivos emparejados. Buscando nuevos dispositivos..."
        }
    }

    @SuppressLint("MissingPermission")
    private fun startDiscovery() {
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter?.cancelDiscovery()
        }

        val started = bluetoothAdapter?.startDiscovery() ?: false
        if (!started) {
            Toast.makeText(this, "No se pudo iniciar la búsqueda", Toast.LENGTH_SHORT).show()
            progressBar.visibility = View.GONE
        }
    }

    @SuppressLint("MissingPermission")
    private fun selectDevice(device: BluetoothDevice) {
        // Cancelar discovery si está activo
        bluetoothAdapter?.cancelDiscovery()

        // Guardar dispositivo seleccionado
        saveDevice(device.address, device.name ?: "Dispositivo sin nombre")

        // Mostrar diálogo de confirmación
        AlertDialog.Builder(this)
            .setTitle("Dispositivo seleccionado")
            .setMessage("Se ha seleccionado:\n${device.name ?: "Dispositivo sin nombre"}\n${device.address}\n\n¿Desea usar este dispositivo para imprimir?")
            .setPositiveButton("Sí") { _, _ ->
                setResult(Activity.RESULT_OK)
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveDevice(address: String, name: String) {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putString(KEY_DEVICE_ADDRESS, address)
            putString(KEY_DEVICE_NAME, name)
            apply()
        }

        Toast.makeText(this, "Dispositivo guardado: $name", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("MissingPermission")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                loadPairedDevices()
                startDiscovery()
            } else {
                Toast.makeText(this, "Bluetooth no activado", Toast.LENGTH_SHORT).show()
                statusText.text = "Bluetooth desactivado"
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (allGranted) {
                enableBluetoothAndSearch()
            } else {
                Toast.makeText(this, "Se requieren permisos de Bluetooth para continuar", Toast.LENGTH_LONG).show()

                // Mostrar qué permisos fueron denegados
                permissions.forEachIndexed { index, permission ->
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        val permissionName = when (permission) {
                            Manifest.permission.BLUETOOTH -> "Bluetooth"
                            Manifest.permission.BLUETOOTH_ADMIN -> "Administrador Bluetooth"
                            Manifest.permission.BLUETOOTH_SCAN -> "Escanear Bluetooth"
                            Manifest.permission.BLUETOOTH_CONNECT -> "Conectar Bluetooth"
                            Manifest.permission.ACCESS_FINE_LOCATION -> "Ubicación"
                            else -> permission
                        }
                        Log.w("Bt", "Permiso denegado: $permissionName")
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        bluetoothAdapter?.cancelDiscovery()
        try {
            unregisterReceiver(discoveryReceiver)
        } catch (e: Exception) {
            // Receiver ya fue desregistrado
        }
    }
}

// Clase helper para manejar la impresión Bluetooth
class BluetoothPrintHelper(private val context: Context) {

    companion object {
        private const val BLUETOOTH_UUID = "00001101-0000-1000-8000-00805F9B34FB"
    }

    @SuppressLint("MissingPermission")
    fun printData(data: String, onComplete: (Boolean, String?) -> Unit) {
        val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val deviceAddress = sharedPreferences.getString("BluetoothDeviceAddress", null)

        if (deviceAddress == null) {
            onComplete(false, "No hay dispositivo seleccionado")
            return
        }

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)

        if (device == null) {
            onComplete(false, "Dispositivo no encontrado")
            return
        }

        Thread {
            try {
                val uuid = UUID.fromString(BLUETOOTH_UUID)
                val socket = device.createRfcommSocketToServiceRecord(uuid)
                socket.connect()

                val outputStream = socket.outputStream

                // Comandos de impresora
                val alignCenterCommand = byteArrayOf(0x1B, 0x61, 0x01)
                val largeFontCommand = byteArrayOf(0x1D, 0x21, 0x11)
                val normalFontCommand = byteArrayOf(0x1B, 0x21, 0x00)

                outputStream.apply {
                    write(alignCenterCommand)
                    write(largeFontCommand)
                    write(data.toByteArray())
                    flush()
                    write(normalFontCommand)
                    write("\n\n\n".toByteArray())
                    flush()
                    close()
                }

                socket.close()

                (context as? Activity)?.runOnUiThread {
                    onComplete(true, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                (context as? Activity)?.runOnUiThread {
                    onComplete(false, e.message)
                }
            }
        }.start()
    }
}