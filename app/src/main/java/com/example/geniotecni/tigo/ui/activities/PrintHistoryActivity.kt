package com.example.geniotecni.tigo.ui.activities

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.geniotecni.tigo.R
import com.example.geniotecni.tigo.helpers.ExportHelper
import com.example.geniotecni.tigo.managers.PreferencesManager
import com.example.geniotecni.tigo.managers.PrintDataManager
import com.example.geniotecni.tigo.models.PrintData
import com.example.geniotecni.tigo.models.ReferenceData
import com.example.geniotecni.tigo.ui.adapters.OptimizedPrintHistoryAdapter
import com.example.geniotecni.tigo.ui.viewmodels.PrintHistoryViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

@AndroidEntryPoint
class PrintHistoryActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var emptyView: View
    private lateinit var emptyTextView: TextView
    private lateinit var adapter: OptimizedPrintHistoryAdapter
    private lateinit var printDataManager: PrintDataManager
    private lateinit var exportHelper: ExportHelper
    private lateinit var preferencesManager: PreferencesManager
    
    // ViewModel with dependency injection - ready for reactive UI when needed
    private val viewModel: PrintHistoryViewModel by viewModels()
    
    private lateinit var fabFilter: FloatingActionButton
    private lateinit var searchCard: MaterialCardView
    private lateinit var searchEditText: TextInputEditText
    private lateinit var clearSearchButton: MaterialButton

    private var printHistory = listOf<PrintData>()
    private var filteredHistory = listOf<PrintData>()
    private var currentFilter = FilterType.ALL
    private var currentSearchQuery = ""

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
        setupSearch()
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
        searchCard = findViewById(R.id.searchCard)
        searchEditText = findViewById(R.id.searchEditText)
        clearSearchButton = findViewById(R.id.clearSearchButton)

        printDataManager = PrintDataManager(this)
        exportHelper = ExportHelper(this)
        preferencesManager = PreferencesManager(this)
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

        adapter = OptimizedPrintHistoryAdapter(
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
            toggleSearchBar()
        }
    }
    
    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentSearchQuery = s?.toString() ?: ""
                performSearch()
            }
        })
        
        searchEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || 
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                performSearch()
                true
            } else {
                false
            }
        }
        
        clearSearchButton.setOnClickListener {
            searchEditText.text?.clear()
            currentSearchQuery = ""
            performSearch()
        }
    }
    
    private fun toggleSearchBar() {
        if (searchCard.visibility == View.VISIBLE) {
            searchCard.visibility = View.GONE
            fabFilter.setImageResource(R.drawable.ic_search)
            searchEditText.text?.clear()
            currentSearchQuery = ""
            performSearch()
        } else {
            searchCard.visibility = View.VISIBLE
            fabFilter.setImageResource(R.drawable.ic_search_off)
            searchEditText.requestFocus()
        }
    }
    
    private fun performSearch() {
        if (currentSearchQuery.isEmpty()) {
            filteredHistory = filterHistory(printHistory)
        } else {
            val searchLower = currentSearchQuery.lowercase()
            filteredHistory = filterHistory(printHistory).filter { printData ->
                searchInPrintData(printData, searchLower)
            }
        }
        
        adapter.updateData(filteredHistory)
        updateEmptyView()
    }
    
    private fun searchInPrintData(printData: PrintData, query: String): Boolean {
        // Search in basic fields
        if (printData.serviceName.lowercase().contains(query) ||
            printData.date.contains(query) ||
            printData.time.contains(query) ||
            printData.message.lowercase().contains(query)) {
            return true
        }
        
        // Search in reference data
        if (printData.referenceData.ref1.lowercase().contains(query) ||
            printData.referenceData.ref2.lowercase().contains(query)) {
            return true
        }
        
        // Search in structured transaction data (preferred method)
        val transactionData = printData.transactionData
        if (transactionData.phone.lowercase().contains(query) ||
            transactionData.cedula.lowercase().contains(query) ||
            transactionData.amount.contains(query) ||
            transactionData.date.contains(query)) {
            return true
        }
        
        // Fallback: Search in extracted data for legacy records
        if (transactionData.phone.isEmpty() || transactionData.cedula.isEmpty() || transactionData.amount.isEmpty()) {
            val phone = extractPhone(printData.message)
            val cedula = extractCedula(printData.message)
            val amount = extractAmount(printData.message)
            val reference = extractLegacyReference(printData.message)
            
            if (phone?.lowercase()?.contains(query) == true ||
                cedula?.lowercase()?.contains(query) == true ||
                amount.toString().contains(query) ||
                reference?.lowercase()?.contains(query) == true) {
                return true
            }
        }
        
        return false
    }
    
    // Helper functions for search (extracted from adapter)
    private fun extractPhone(message: String): String? {
        val patterns = listOf(
            Regex("""Tel[eé]fono:\s*([0-9\-\s]+)"""),
            Regex("""Tel:\s*([0-9\-\s]+)"""),
            Regex("""Teléfono:\s*([0-9\-\s]+)"""),
            Regex("""Número:\s*([0-9\-\s]+)"""),
            Regex("""([0-9]{3,4}[\-\s]?[0-9]{6,7})""") // General phone pattern
        )
        
        for (pattern in patterns) {
            val match = pattern.find(message)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }
        return null
    }
    
    private fun extractCedula(message: String): String? {
        val patterns = listOf(
            Regex("""Cédula:\s*([0-9\.\-\s]+)"""),
            Regex("""C\.I\.:?\s*([0-9\.\-\s]+)"""),
            Regex("""CI:\s*([0-9\.\-\s]+)"""),
            Regex("""([0-9]{1,2}\.[0-9]{3}\.[0-9]{3})"""), // CI format x.xxx.xxx
            Regex("""([0-9]{7,8})""") // Simple number format
        )
        
        for (pattern in patterns) {
            val match = pattern.find(message)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }
        return null
    }
    
    private fun extractLegacyReference(message: String): String? {
        val regex = Regex("""Ref:\s*(\w+)""")
        val match = regex.find(message)
        return match?.groupValues?.get(1)
    }
    
    private fun extractAmount(message: String): Long {
        val regex = Regex("""Monto:\s*([0-9,]+)\s*Gs\.?""")
        val match = regex.find(message)
        return match?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull() ?: 0L
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
                    performSearch() // Apply current search and filter

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
                val weekCalendar = java.util.Calendar.getInstance()
                weekCalendar.add(java.util.Calendar.DAY_OF_YEAR, -7)
                val weekAgo = weekCalendar.time
                history.filter { data ->
                    isAfterDate(data.date, weekAgo)
                }
            }
            FilterType.MONTH -> {
                val monthCalendar = java.util.Calendar.getInstance()
                monthCalendar.add(java.util.Calendar.MONTH, -1)
                val monthAgo = monthCalendar.time
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
            dataDate?.let { formatter.format(it) } == formatter.format(date)
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
        if (filteredHistory.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE

            emptyTextView.text = when {
                currentSearchQuery.isNotEmpty() -> "No se encontraron resultados para '\$currentSearchQuery'"
                currentFilter == FilterType.ALL -> "No hay transacciones registradas"
                currentFilter == FilterType.TODAY -> "No hay transacciones de hoy"
                currentFilter == FilterType.WEEK -> "No hay transacciones de esta semana"
                currentFilter == FilterType.MONTH -> "No hay transacciones de este mes"
                else -> "No hay transacciones registradas"
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
                performSearch() // Apply new filter with current search
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
            R.id.action_filter -> {
                showFilterDialog()
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
        filteredHistory = emptyList()
        adapter.updateData(filteredHistory)
        updateEmptyView()
        showSnackbar("Historial borrado")
    }

    private fun deletePrintData(printData: PrintData) {
        // Remove from list and update adapter
        val newList = printHistory.toMutableList()
        newList.removeIf {
            it.serviceName == printData.serviceName &&
                    it.date == printData.date &&
                    it.time == printData.time
        }

        // Save updated list
        // Note: This requires modifying PrintDataManager to support updating the entire list
        // For now, we'll just update the UI
        printHistory = newList
        performSearch() // Refresh filtered results
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

                    // Print commands based on size preference
                    val printSize = preferencesManager.printSize
                    val alignCenterCommand = byteArrayOf(0x1B, 0x61, 0x01)
                    val alignLeftCommand = byteArrayOf(0x1B, 0x61, 0x00)
                    
                    val (headerFontCommand, bodyFontCommand) = when (printSize) {
                        "small" -> {
                            // Small: Header normal, body small
                            Pair(
                                byteArrayOf(0x1B, 0x21, 0x00), // Normal font for header
                                byteArrayOf(0x1B, 0x21, 0x08)  // Small font for body
                            )
                        }
                        "large" -> {
                            // Large: Header large, body normal
                            Pair(
                                byteArrayOf(0x1D, 0x21, 0x11), // Large font for header
                                byteArrayOf(0x1B, 0x21, 0x10)  // Medium font for body
                            )
                        }
                        else -> { // "medium" (default)
                            // Medium: Header medium, body normal
                            Pair(
                                byteArrayOf(0x1D, 0x21, 0x11), // Large font for header
                                byteArrayOf(0x1B, 0x21, 0x00)  // Normal font for body
                            )
                        }
                    }

                    // Print header
                    outputStream.write(alignCenterCommand)
                    outputStream.write(headerFontCommand)
                    outputStream.write("GENIO TECNI\n".toByteArray())
                    
                    // Print body
                    outputStream.write(alignLeftCommand)
                    outputStream.write(bodyFontCommand)
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