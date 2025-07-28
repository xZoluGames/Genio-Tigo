package com.example.geniotecni.tigo.ui.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import java.util.UUID

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