package com.example.geniotecni.tigo.ui.activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.geniotecni.tigo.R
import com.example.geniotecni.tigo.helpers.ExportHelper
import com.example.geniotecni.tigo.managers.PrintDataManager
import com.example.geniotecni.tigo.models.PrintData
import com.example.geniotecni.tigo.models.ReferenceData
import com.example.geniotecni.tigo.ui.adapters.PrintHistoryAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import java.io.IOException
import java.util.UUID

class PrintHistoryActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var emptyView: View
    private lateinit var emptyTextView: TextView
    private lateinit var adapter: PrintHistoryAdapter
    private lateinit var printDataManager: PrintDataManager
    private lateinit var exportHelper: ExportHelper
    private lateinit var fabFilter: FloatingActionButton

    private var printHistory = listOf<PrintData>()
    private var currentFilter = FilterType.ALL

    enum class FilterType {
        ALL, TODAY, WEEK, MONTH
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_print_history)

        setupWindowInsets()
        initializeViews()
        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        setupFAB()
        loadPrintHistory()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recyclerViewPrintHistory)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        emptyView = findViewById(R.id.emptyView)
        emptyTextView = findViewById(R.id.emptyTextView)
        fabFilter = findViewById(R.id.fabFilter)

        printDataManager = PrintDataManager(this)
        exportHelper = ExportHelper(this)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "Historial de Transacciones"
            setDisplayHomeAsUpEnabled(true)
        }
        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)

        // Add item decoration for spacing
        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: android.graphics.Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                outRect.bottom = resources.getDimensionPixelSize(R.dimen.margin_small)
            }
        })

        adapter = PrintHistoryAdapter(
            printHistory = emptyList(),
            onReprintClick = { printData ->
                showReprintConfirmationDialog(printData)
            },
            onShareClick = { printData ->
                // Already handled in adapter
            },
            onDeleteClick = { printData ->
                deletePrintData(printData)
            }
        )

        recyclerView.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
            R.color.md_theme_light_primary,
            R.color.md_theme_light_secondary,
            R.color.md_theme_light_tertiary
        )

        swipeRefreshLayout.setOnRefreshListener {
            loadPrintHistory()
        }
    }

    private fun setupFAB() {
        fabFilter.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun loadPrintHistory() {
        swipeRefreshLayout.isRefreshing = true

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Simulate network delay for better UX
                delay(500)

                val allHistory = printDataManager.getAllPrintData()
                val filteredHistory = filterHistory(allHistory)

                withContext(Dispatchers.Main) {
                    printHistory = filteredHistory
                    adapter.updateData(printHistory)

                    updateEmptyView()
                    swipeRefreshLayout.isRefreshing = false

                    if (filteredHistory.isEmpty() && currentFilter != FilterType.ALL) {
                        showSnackbar("No hay transacciones para el período seleccionado")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    swipeRefreshLayout.isRefreshing = false
                    showSnackbar("Error al cargar el historial")
                }
            }
        }
    }

    private fun filterHistory(history: List<PrintData>): List<PrintData> {
        val calendar = java.util.Calendar.getInstance()
        val today = calendar.time

        return when (currentFilter) {
            FilterType.ALL -> history
            FilterType.TODAY -> {
                history.filter { data ->
                    isSameDay(data.date, today)
                }
            }
            FilterType.WEEK -> {
                calendar.add(java.util.Calendar.DAY_OF_YEAR, -7)
                val weekAgo = calendar.time
                history.filter { data ->
                    isAfterDate(data.date, weekAgo)
                }
            }
            FilterType.MONTH -> {
                calendar.add(java.util.Calendar.MONTH, -1)
                val monthAgo = calendar.time
                history.filter { data ->
                    isAfterDate(data.date, monthAgo)
                }
            }
        }
    }

    private fun isSameDay(dateString: String, date: java.util.Date): Boolean {
        return try {
            val formatter = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault())
            val dataDate = formatter.parse(dateString)
            formatter.format(dataDate) == formatter.format(date)
        } catch (e: Exception) {
            false
        }
    }

    private fun isAfterDate(dateString: String, date: java.util.Date): Boolean {
        return try {
            val formatter = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault())
            val dataDate = formatter.parse(dateString)
            dataDate?.after(date) ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun updateEmptyView() {
        if (printHistory.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE

            emptyTextView.text = when (currentFilter) {
                FilterType.ALL -> "No hay transacciones registradas"
                FilterType.TODAY -> "No hay transacciones de hoy"
                FilterType.WEEK -> "No hay transacciones de esta semana"
                FilterType.MONTH -> "No hay transacciones de este mes"
            }
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        }
    }

    private fun showFilterDialog() {
        val options = arrayOf("Todas", "Hoy", "Esta semana", "Este mes")
        val currentIndex = currentFilter.ordinal

        MaterialAlertDialogBuilder(this)
            .setTitle("Filtrar por período")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                currentFilter = FilterType.values()[which]
                loadPrintHistory()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_print_history, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_export -> {
                showExportDialog()
                true
            }
            R.id.action_clear -> {
                showClearHistoryDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showExportDialog() {
        val options = arrayOf("Exportar como CSV", "Exportar como PDF", "Exportar ambos")

        MaterialAlertDialogBuilder(this)
            .setTitle("Exportar historial")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> exportHelper.exportToCSV(true)
                    1 -> exportHelper.exportToPDF(true)
                    2 -> {
                        exportHelper.exportToCSV(false)
                        exportHelper.exportToPDF(true)
                    }
                }
            }
            .show()
    }

    private fun showClearHistoryDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Borrar historial")
            .setMessage("¿Está seguro de que desea borrar todo el historial? Esta acción no se puede deshacer.")
            .setPositiveButton("Borrar") { _, _ ->
                clearHistory()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun clearHistory() {
        printDataManager.clearAllData()
        printHistory = emptyList()
        adapter.updateData(printHistory)
        updateEmptyView()
        showSnackbar("Historial borrado")
    }

    private fun deletePrintData(printData: PrintData) {
        // Remove from list and update adapter
        val newList = printHistory.toMutableList()
        newList.removeIf {
            it.service == printData.service &&
                    it.date == printData.date &&
                    it.time == printData.time
        }

        // Save updated list
        // Note: This requires modifying PrintDataManager to support updating the entire list
        // For now, we'll just update the UI
        printHistory = newList
        adapter.updateData(printHistory)
        updateEmptyView()
        showSnackbar("Transacción eliminada")
    }

    private fun showReprintConfirmationDialog(printData: PrintData) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Reimprimir")
            .setMessage("¿Desea reimprimir esta transacción?")
            .setPositiveButton("Sí") { _, _ ->
                printData(printData.message, printData.referenceData)
            }
            .setNegativeButton("No", null)
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun printData(data: String, referenceData: ReferenceData, retryCount: Int = 3) {
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val deviceAddress = sharedPreferences.getString("BluetoothDeviceAddress", null)
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val device = deviceAddress?.let { bluetoothAdapter?.getRemoteDevice(it) }

        if (deviceAddress == null || device == null) {
            showSnackbar("No se encontró dispositivo Bluetooth configurado")
            startActivity(Intent(this, Bt::class.java))
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            var attempts = retryCount

            while (attempts > 0) {
                try {
                    val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
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
                    outputStream.write(data.toByteArray())
                    outputStream.write("\n\n\n".toByteArray())
                    outputStream.flush()

                    outputStream.close()
                    socket.close()

                    withContext(Dispatchers.Main) {
                        showSnackbar("Impresión completada")
                    }
                    return@launch

                } catch (e: IOException) {
                    e.printStackTrace()
                    attempts--

                    if (attempts == 0) {
                        withContext(Dispatchers.Main) {
                            showSnackbar("Error al imprimir. Intente nuevamente.")
                        }
                    } else {
                        delay(1000) // Wait before retry
                    }
                }
            }
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(recyclerView, message, Snackbar.LENGTH_SHORT).show()
    }
}