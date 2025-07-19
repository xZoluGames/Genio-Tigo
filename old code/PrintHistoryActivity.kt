package com.example.geniotecni.tigo
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.widget.Toast
import java.io.IOException
import java.util.UUID

class PrintHistoryActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PrintHistoryAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_print_history)

        recyclerView = findViewById(R.id.recyclerViewPrintHistory)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val printDataManager = PrintDataManager(this)
        val printHistory = printDataManager.getAllPrintData()

        adapter = PrintHistoryAdapter(printHistory) { printData ->
            showReprintConfirmationDialog(printData)
        }
        recyclerView.adapter = adapter
    }

    private fun showReprintConfirmationDialog(printData: PrintData) {
        AlertDialog.Builder(this)
            .setTitle("Reimprimir")
            .setMessage("¿Deseas reimprimir estos datos?")
            .setPositiveButton("Sí") { _, _ ->
                printData(printData.message, printData.referenceData)
            }
            .setNegativeButton("No", null)
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun printData(data: String, referenceData: MainActivity.ReferenceData?, retryCount: Int = 3) {
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val deviceAddress = sharedPreferences.getString("BluetoothDeviceAddress", null)
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val device = deviceAddress?.let { bluetoothAdapter?.getRemoteDevice(it) }
        if (deviceAddress == null || device == null) {
            Toast.makeText(this, "No se encontró un dispositivo Bluetooth seleccionado, configurelo!", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "Fallo al imprimir, reintentando (${retryCount - attempts}/$retryCount)", Toast.LENGTH_LONG).show()
                if (attempts == 0) {
                    showReprintConfirmationDialog(PrintData("", "", "", data, referenceData ?: MainActivity.ReferenceData(
                        "",
                        ""
                    )
                    ))
                    break
                }
            }
        }
    }
}