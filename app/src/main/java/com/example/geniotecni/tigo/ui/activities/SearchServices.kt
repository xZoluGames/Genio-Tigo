package com.example.geniotecni.tigo.ui.activities

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.geniotecni.tigo.R
import com.example.geniotecni.tigo.helpers.LoadingAnimationHelper
import com.example.geniotecni.tigo.ui.adapters.ServiceAdapter
import com.example.geniotecni.tigo.ui.adapters.ServiceItem
import com.example.geniotecni.tigo.ui.adapters.toServiceItems
import com.example.geniotecni.tigo.utils.Constants
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout

class SearchServices : AppCompatActivity() {

    companion object {
        private const val TAG = "SearchServices"
        private const val PERMISSION_REQUEST_CODE = 100
    }

    private val servicios = Constants.SERVICE_NAMES

    // UI Components
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var collapsingToolbar: CollapsingToolbarLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchInputLayout: TextInputLayout
    private lateinit var searchEditText: MaterialAutoCompleteTextView
    private lateinit var serviceAdapter: ServiceAdapter
    private lateinit var loadingHelper: LoadingAnimationHelper
    private lateinit var fabQuickAdd: ExtendedFloatingActionButton

    // Quick action cards
    private lateinit var cardPrintHistory: MaterialCardView
    private lateinit var cardStatistics: MaterialCardView
    private lateinit var cardSettings: MaterialCardView
    private lateinit var cardBluetooth: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "SearchServices onCreate iniciado")

        try {
            setContentView(R.layout.activity_main)

            setupWindowInsets()
            initializeViews()
            setupRecyclerView()
            setupSearchFunctionality()
            setupQuickActions()
            setupFAB()
            requestPermissions()

            Log.d(TAG, "SearchServices onCreate completado exitosamente")

        } catch (e: Exception) {
            Log.e(TAG, "Error en SearchServices onCreate", e)
            throw e
        }
    }

    private fun setupWindowInsets() {
        // Enable edge-to-edge display
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }

        // Make status bar transparent
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        // Set up window insets listener
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
    }

    private fun initializeViews() {
        Log.d(TAG, "Inicializando vistas")

        loadingHelper = LoadingAnimationHelper(this)

        // App bar components
        appBarLayout = findViewById(R.id.appBarLayout)
        collapsingToolbar = findViewById(R.id.collapsingToolbar)
        toolbar = findViewById(R.id.toolbar)

        // Setup toolbar
        setSupportActionBar(toolbar)

        // Search components
        searchInputLayout = findViewById<MaterialCardView>(R.id.searchCard)
            .findViewById(R.id.searchInputLayout)
        searchEditText = findViewById<MaterialCardView>(R.id.searchCard)
            .findViewById(R.id.autoCompleteTextView)

        // RecyclerView
        recyclerView = findViewById(R.id.recyclerView)

        // Quick action cards
        cardPrintHistory = findViewById(R.id.cardPrintHistory)
        cardStatistics = findViewById(R.id.cardStatistics)
        cardSettings = findViewById(R.id.cardSettings)
        cardBluetooth = findViewById(R.id.cardBluetooth)

        // FAB
        fabQuickAdd = findViewById(R.id.fabQuickAdd)

        Log.d(TAG, "Vistas inicializadas correctamente")
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "Configurando RecyclerView")

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

        // Create service items
        val serviceItems = servicios.toList().toServiceItems()

        serviceAdapter = ServiceAdapter(
            services = serviceItems,
            onItemClick = { service ->
                navigateToMainActivity(service)
            }
        )

        recyclerView.adapter = serviceAdapter

        // Add scroll listener for FAB behavior
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0 && fabQuickAdd.isExtended) {
                    fabQuickAdd.shrink()
                } else if (dy < 0 && !fabQuickAdd.isExtended) {
                    fabQuickAdd.extend()
                }
            }
        })

        Log.d(TAG, "RecyclerView configurado con ${serviceItems.size} servicios")
    }

    private fun setupSearchFunctionality() {
        Log.d(TAG, "Configurando funcionalidad de búsqueda")

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterServices(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Setup autocomplete suggestions
        val suggestions = servicios.toList()
        val adapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            suggestions
        )
        searchEditText.setAdapter(adapter)

        // Handle item click from autocomplete
        searchEditText.setOnItemClickListener { _, _, position, _ ->
            val selectedService = adapter.getItem(position)
            selectedService?.let {
                val serviceItem = servicios.toList().toServiceItems()
                    .find { item -> item.name == it }
                serviceItem?.let { item ->
                    navigateToMainActivity(item)
                }
            }
        }
    }

    private fun setupQuickActions() {
        Log.d(TAG, "Configurando acciones rápidas")

        // Aplicar animación de entrada a las tarjetas
        val slideIn = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        cardPrintHistory.startAnimation(slideIn)
        cardStatistics.startAnimation(slideIn)
        cardSettings.startAnimation(slideIn)
        cardBluetooth.startAnimation(slideIn)

        // Print History
        cardPrintHistory.setOnClickListener {
            it.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    loadingHelper.showLoadingAndNavigate(
                        targetActivity = PrintHistoryActivity::class.java,
                        message = "Cargando historial...",
                        duration = 800L
                    )
                }
                .start()
        }

        // Statistics
        cardStatistics.setOnClickListener {
            it.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    loadingHelper.showLoadingAndNavigate(
                        targetActivity = StatisticsActivity::class.java,
                        message = "Calculando estadísticas...",
                        duration = 1000L
                    )
                }
                .start()
        }

        // Settings
        cardSettings.setOnClickListener {
            it.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    loadingHelper.showLoadingAndNavigate(
                        targetActivity = SettingsActivity::class.java,
                        message = "Abriendo configuración...",
                        duration = 600L
                    )
                }
                .start()
        }

        // Bluetooth
        cardBluetooth.setOnClickListener {
            it.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    checkBluetoothAndNavigate()
                }
                .start()
        }
    }

    private fun setupFAB() {
        fabQuickAdd.setOnClickListener {
            // Navigate to the most used service
            val mostUsedService = servicios.toList().toServiceItems().firstOrNull()
            mostUsedService?.let {
                navigateToMainActivity(it)
            }
        }
    }

    private fun filterServices(query: String) {
        val filteredServices = servicios.filter { service ->
            service.contains(query, ignoreCase = true)
        }
        serviceAdapter.updateServices(filteredServices)
    }

    private fun navigateToMainActivity(service: ServiceItem) {
        Log.d(TAG, "Navegando a MainActivity con servicio: ${service.name}")

        loadingHelper.showLoadingAndNavigate(
            targetActivity = MainActivity::class.java,
            message = "Cargando ${service.name}...",
            duration = 1200L
        ) {
            putExtra("selectedService", service)
        }
    }

    private fun checkBluetoothAndNavigate() {
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val deviceAddress = sharedPreferences.getString("BluetoothDeviceAddress", null)
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val device = deviceAddress?.let { bluetoothAdapter?.getRemoteDevice(it) }

        if (deviceAddress == null || device == null) {
            showSnackbar("No se encontró dispositivo Bluetooth configurado")
        }

        loadingHelper.showLoadingAndNavigate(
            targetActivity = Bt::class.java,
            message = "Configurando Bluetooth...",
            duration = 1000L
        )
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_SMS
        )

        // Add Bluetooth permissions for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            val deniedPermissions = permissions.filterIndexed { index, _ ->
                grantResults[index] != PackageManager.PERMISSION_GRANTED
            }

            if (deniedPermissions.isNotEmpty()) {
                showSnackbar("Algunos permisos fueron denegados")
            }
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(
            findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_SHORT
        ).show()
    }
}