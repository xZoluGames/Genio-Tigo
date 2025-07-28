package com.example.geniotecni.tigo.managers

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.geniotecni.tigo.utils.BaseManager
import com.example.geniotecni.tigo.utils.AppConfig
import com.example.geniotecni.tigo.utils.AppLogger
import java.io.IOException
import java.util.*

class BluetoothManager(context: Context) : BaseManager(context, "BluetoothManager") {
    
    companion object {
        private const val BLUETOOTH_UUID = AppConfig.Bluetooth.UUID
        private const val PREFS_NAME = AppConfig.BLUETOOTH_PREFS
        private const val KEY_DEVICE_ADDRESS = AppConfig.PreferenceKeys.KEY_DEVICE_ADDRESS
        private const val KEY_DEVICE_NAME = AppConfig.PreferenceKeys.KEY_DEVICE_NAME
        private const val TAG = "BluetoothManager"
    }
    
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    
    fun isBluetoothSupported(): Boolean {
        return bluetoothAdapter != null
    }
    
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
    
    fun hasSelectedDevice(): Boolean {
        val deviceAddress = getString(KEY_DEVICE_ADDRESS)
        return deviceAddress.isNotEmpty()
    }
    
    fun getSelectedDeviceAddress(): String? {
        val address = getString(KEY_DEVICE_ADDRESS)
        return if (address.isEmpty()) null else address
    }
    
    fun getSelectedDeviceName(): String? {
        val name = getString(KEY_DEVICE_NAME)
        return if (name.isEmpty()) null else name
    }
    
    fun saveSelectedDevice(address: String, name: String) {
        savePreference(KEY_DEVICE_ADDRESS, address)
        savePreference(KEY_DEVICE_NAME, name)
        AppLogger.i(tag, "Dispositivo Bluetooth guardado: $name ($address)")
    }
    
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): Set<BluetoothDevice>? {
        return if (hasBluetoothPermission()) {
            bluetoothAdapter?.bondedDevices
        } else {
            null
        }
    }
    
    @SuppressLint("MissingPermission")
    fun getSelectedDevice(): BluetoothDevice? {
        val address = getSelectedDeviceAddress()
        return if (address != null && hasBluetoothPermission()) {
            bluetoothAdapter?.getRemoteDevice(address)
        } else {
            null
        }
    }
    
    private fun hasBluetoothPermission(): Boolean {
        return when {
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S -> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            }
            else -> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    fun printData(data: String, onResult: (Boolean, String?) -> Unit) {
        if (!isBluetoothSupported()) {
            onResult(false, "Bluetooth no soportado")
            return
        }
        
        if (!isBluetoothEnabled()) {
            onResult(false, "Bluetooth no habilitado")
            return
        }
        
        if (!hasBluetoothPermission()) {
            onResult(false, "Permisos de Bluetooth no otorgados")
            return
        }
        
        val device = getSelectedDevice()
        if (device == null) {
            onResult(false, "No hay dispositivo seleccionado")
            return
        }
        
        Thread {
            try {
                AppLogger.i(TAG, "Iniciando impresión a ${device.name ?: device.address}")
                val uuid = UUID.fromString(BLUETOOTH_UUID)
                val socket = device.createRfcommSocketToServiceRecord(uuid)
                
                socket.connect()
                AppLogger.d(TAG, "Conexión Bluetooth establecida")
                
                val outputStream = socket.outputStream
                
                // Comandos de impresora ESC/POS
                val initCommand = byteArrayOf(0x1B, 0x40) // ESC @ - Initialize printer
                val alignCenterCommand = byteArrayOf(0x1B, 0x61, 0x01) // ESC a 1 - Center alignment
                val largeFontCommand = byteArrayOf(0x1D, 0x21, 0x11) // GS ! 0x11 - Large font
                val normalFontCommand = byteArrayOf(0x1B, 0x21, 0x00) // ESC ! 0 - Normal font
                val cutCommand = byteArrayOf(0x1D, 0x56, 0x00) // GS V 0 - Cut paper
                
                outputStream.apply {
                    write(initCommand)
                    write(alignCenterCommand)
                    write(largeFontCommand)
                    write("GENIO TECNI\n".toByteArray())
                    write(normalFontCommand)
                    write("================================\n".toByteArray())
                    write(data.toByteArray())
                    write("\n================================\n".toByteArray())
                    write("Gracias por su preferencia\n".toByteArray())
                    write("\n\n\n".toByteArray())
                    flush()
                    close()
                }
                
                socket.close()
                AppLogger.i(TAG, "Impresión completada exitosamente")
                onResult(true, null)
                
            } catch (e: IOException) {
                AppLogger.e(TAG, "Error durante la impresión", e)
                onResult(false, "Error de impresión: ${e.message}")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error inesperado durante la impresión", e)
                onResult(false, "Error inesperado: ${e.message}")
            }
        }.start()
    }
    
    fun clearSelectedDevice() {
        sharedPreferences.edit()
            .remove(KEY_DEVICE_ADDRESS)
            .remove(KEY_DEVICE_NAME)
            .apply()
        AppLogger.i(TAG, "Dispositivo Bluetooth eliminado")
    }
}