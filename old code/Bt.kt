package com.example.geniotecni.tigo

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.UUID

class Bt : AppCompatActivity() {

    private val REQUEST_ENABLE_BT = 1
    private val PERMISSION_REQUEST_CODE = 101
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var selectedDeviceAddress: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bt)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "El dispositivo no soporta Bluetooth", Toast.LENGTH_LONG).show()
            return
        }

        // Botón para buscar dispositivos emparejados
        val searchButton: Button = findViewById(R.id.btnSearch)
        searchButton.setOnClickListener {
            if (!bluetoothAdapter!!.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return@setOnClickListener
                }
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            } else {
                if (checkBluetoothPermissions()) {
                    listPairedDevices()
                }
            }
        }
    }

    // Guardar la dirección MAC del dispositivo en SharedPreferences
    private fun saveDeviceAddress(deviceAddress: String?) {
        val sharedPreferences: SharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("BluetoothDeviceAddress", deviceAddress)
        editor.apply()
    }

    // Verificar y solicitar permisos
    private fun checkBluetoothPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
            return false
        }
        return true
    }

    // Listar los dispositivos emparejados y permitir la selección
    @SuppressLint("MissingPermission")
    private fun listPairedDevices() {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        val listView: ListView = findViewById(R.id.deviceList)

        if (pairedDevices != null && pairedDevices.isNotEmpty()) {
            val deviceList: ArrayList<String> = ArrayList()
            for (device in pairedDevices) {
                val deviceName = device.name
                val deviceAddress = device.address
                deviceList.add("$deviceName\n$deviceAddress")
            }

            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceList)
            listView.adapter = adapter

            listView.setOnItemClickListener { _, view, _, _ ->
                val deviceInfo = (view as TextView).text.toString()
                selectedDeviceAddress = deviceInfo.substring(deviceInfo.length - 17)
                saveDeviceAddress(selectedDeviceAddress)
                Toast.makeText(this, "Dispositivo seleccionado: $selectedDeviceAddress", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No hay dispositivos emparejados", Toast.LENGTH_SHORT).show()
        }
    }

    // Manejar resultados de habilitar Bluetooth
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_OK) {
            if (checkBluetoothPermissions()) {
                listPairedDevices()
            }
        } else {
            Toast.makeText(this, "Bluetooth no activado", Toast.LENGTH_SHORT).show()
        }
    }
}


