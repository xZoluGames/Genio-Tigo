package com.example.geniotecni.tigo.ui.activities

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.geniotecni.tigo.R
import com.example.geniotecni.tigo.data.repository.OptimizedServiceRepository
import com.example.geniotecni.tigo.helpers.LoadingAnimationHelper
import com.example.geniotecni.tigo.ui.adapters.OptimizedServiceAdapter
import com.example.geniotecni.tigo.ui.adapters.OptimizedServiceAdapter.Companion.toServiceItems
import com.example.geniotecni.tigo.ui.adapters.ServiceItem
import com.example.geniotecni.tigo.ui.viewmodels.SearchServicesViewModel
import com.example.geniotecni.tigo.utils.AppLogger
import com.example.geniotecni.tigo.utils.Constants
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint

/**
 * 🔍 ACTIVIDAD DE BÚSQUEDA DE SERVICIOS - Hub Central de Navegación
 * 
 * PROPÓSITO PRINCIPAL:
 * - Actividad principal para búsqueda y selección de servicios disponibles
 * - Hub central de navegación hacia todas las funcionalidades de la app
 * - Interfaz optimizada con Material Design 3 y animaciones fluidas
 * - Punto de entrada principal para la experiencia de usuario
 * 
 * FUNCIONALIDADES PRINCIPALES:
 * - Búsqueda inteligente con autocompletado de servicios
 * - Lista adaptativa que muestra servicios populares primero
 * - Acciones rápidas para funciones frecuentes (historial, estadísticas, etc.)
 * - Configuración automática de permisos críticos
 * - Navegación optimizada con efectos de carga personalizados
 * 
 * ARQUITECTURA DE UI:
 * - CollapsingToolbarLayout para experiencia inmersiva
 * - RecyclerView optimizado con OptimizedServiceAdapter
 * - FloatingActionButton inteligente que se adapta al scroll
 * - MaterialCardViews con animaciones táctiles
 * - Búsqueda en tiempo real con filtrado instantáneo
 * 
 * GESTIÓN DE PERMISOS:
 * - Solicitud automática de permisos críticos (CALL_PHONE, READ_SMS, Bluetooth)
 * - Manejo inteligente de permisos denegados con explicaciones contextuales
 * - Navegación a configuración del sistema para permisos complejos
 * 
 * OPTIMIZACIONES DE RENDIMIENTO:
 * - Lazy loading de servicios con estrategia "Ver más"
 * - Animaciones suaves con timings optimizados
 * - Diagnóstico automático de recursos para debugging
 * - Memory management con LoadingAnimationHelper
 * 
 * CONEXIONES ARQUITECTÓNICAS:
 * - CONSUME: OptimizedServiceRepository para datos de servicios
 * - UTILIZA: OptimizedServiceAdapter para renderizado eficiente
 * - NAVEGA A: MainActivity, PrintHistoryActivity, SettingsActivity, etc.
 * - GESTIONA: LoadingAnimationHelper para transiciones suaves
 * - COORDINA: Sistema de permisos para funcionalidad completa
 */
@AndroidEntryPoint
class SearchServices : AppCompatActivity() {

    companion object {
        private const val TAG = "SearchServices"
        private const val PERMISSION_REQUEST_CODE = 100
    }

    private val servicios = Constants.SERVICE_NAMES
    private var showAllServices = false // Control para mostrar todos los servicios
    private val reseteoClienteIndex = 75 // Índice del servicio "Reseteo de Cliente"
    
    // Use new architecture components
    private val serviceRepository = OptimizedServiceRepository.getInstance()
    
    // ViewModel with dependency injection - ready for reactive UI when needed
    private val viewModel: SearchServicesViewModel by viewModels()

    // UI Components
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var collapsingToolbar: CollapsingToolbarLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchInputLayout: TextInputLayout
    private lateinit var searchEditText: MaterialAutoCompleteTextView
    private lateinit var serviceAdapter: OptimizedServiceAdapter
    private lateinit var loadingHelper: LoadingAnimationHelper
    private lateinit var fabQuickAdd: ExtendedFloatingActionButton

    // Quick action cards
    private lateinit var cardPrintHistory: MaterialCardView
    private lateinit var cardStatistics: MaterialCardView
    private lateinit var cardSettings: MaterialCardView
    private lateinit var cardBluetooth: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.i(TAG, "=== INICIANDO SEARCH SERVICES ===")
        AppLogger.logMemoryUsage(TAG, "SearchServices onCreate inicio")

        try {
            val startTime = System.currentTimeMillis()
            AppLogger.i(TAG, "Cargando layout activity_main")
            setContentView(R.layout.activity_main)
            AppLogger.i(TAG, "Layout cargado en ${System.currentTimeMillis() - startTime}ms")

            setupWindowInsets()
            initializeViews()
            setupRecyclerView()
            setupSearchFunctionality()
            setupQuickActions()
            setupFAB()
            requestPermissions()

            AppLogger.i(TAG, "=== SEARCH SERVICES INICIALIZADO CORRECTAMENTE ===")
            AppLogger.logMemoryUsage(TAG, "SearchServices onCreate final")

        } catch (e: Exception) {
            AppLogger.e(TAG, "Error crítico en SearchServices onCreate", e)
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
        AppLogger.i(TAG, "Inicializando vistas de SearchServices")
        val startTime = System.currentTimeMillis()

        loadingHelper = LoadingAnimationHelper(this, this)

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

        val initTime = System.currentTimeMillis() - startTime
        AppLogger.i(TAG, "Vistas inicializadas correctamente en ${initTime}ms")
    }

    private fun setupRecyclerView() {
        AppLogger.i(TAG, "Configurando RecyclerView")
        val startTime = System.currentTimeMillis()

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

        // Create service items - initially show only up to "Reseteo de Cliente" + "Ver más"
        val serviceItems = getVisibleServices()
        AppLogger.logDataProcessing(TAG, "Crear elementos de servicio", "ServiceItems", serviceItems.size)

        serviceAdapter = OptimizedServiceAdapter(
            services = serviceItems,
            onItemClick = { service ->
                AppLogger.logUserAction(TAG, "Selección de servicio desde RecyclerView", service.name)
                navigateToMainActivity(service)
            },
            onViewMoreClick = {
                val currentCount = serviceAdapter.itemCount
                AppLogger.i(TAG, "Botón 'Ver más' presionado - expandiendo lista completa")
                AppLogger.d(TAG, "Servicios actuales: $currentCount, showAllServices: $showAllServices -> true")
                showAllServices = true
                updateVisibleServices()
                val newCount = serviceAdapter.itemCount
                AppLogger.i(TAG, "Lista expandida: $currentCount -> $newCount servicios (+${newCount - currentCount} nuevos)")
            }
        )
        AppLogger.d(TAG, "OptimizedServiceAdapter configurado")

        recyclerView.adapter = serviceAdapter

        // Add scroll listener for FAB behavior
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0 && fabQuickAdd.isExtended) {
                    AppLogger.d(TAG, "FAB contraido por scroll hacia abajo")
                    fabQuickAdd.shrink()
                } else if (dy < 0 && !fabQuickAdd.isExtended) {
                    AppLogger.d(TAG, "FAB extendido por scroll hacia arriba")
                    fabQuickAdd.extend()
                }
            }
        })
        AppLogger.d(TAG, "Scroll listener configurado")

        val setupTime = System.currentTimeMillis() - startTime
        AppLogger.i(TAG, "RecyclerView configurado con ${serviceItems.size} servicios en ${setupTime}ms")
    }

    private fun setupSearchFunctionality() {
        AppLogger.i(TAG, "Configurando funcionalidad de búsqueda")
        val startTime = System.currentTimeMillis()

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                AppLogger.d(TAG, "Texto de búsqueda cambiado: '$query'")
                filterServices(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        AppLogger.d(TAG, "TextWatcher configurado")

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
            AppLogger.logUserAction(TAG, "Selección desde autocompletado", "Posición: $position")
            selectedService?.let {
                AppLogger.d(TAG, "Servicio seleccionado desde autocompletado: $it")
                val serviceItem = servicios.toList().toServiceItems()
                    .find { item -> item.name == it }
                serviceItem?.let { item ->
                    navigateToMainActivity(item)
                }
            }
        }
        
        val setupTime = System.currentTimeMillis() - startTime
        AppLogger.i(TAG, "Funcionalidad de búsqueda configurada en ${setupTime}ms")
    }

    private fun setupQuickActions() {
        AppLogger.i(TAG, "Configurando acciones rápidas")
        val startTime = System.currentTimeMillis()

        // Diagnóstico detallado de iconos
        AppLogger.d(TAG, "Iniciando diagnóstico de iconos")
        diagnosticarIconos()

        // Aplicar animación de entrada a las tarjetas
        val slideIn = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        cardPrintHistory.startAnimation(slideIn)
        cardStatistics.startAnimation(slideIn)
        cardSettings.startAnimation(slideIn)
        cardBluetooth.startAnimation(slideIn)

        // Print History
        cardPrintHistory.setOnClickListener {
            AppLogger.logButtonClick(TAG, "CardPrintHistory", "Navegar a historial de impresión")
            it.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    AppLogger.logNavigationStart(TAG, "SearchServices", "PrintHistoryActivity")
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
            AppLogger.logButtonClick(TAG, "CardStatistics", "Navegar a estadísticas")
            it.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    AppLogger.logNavigationStart(TAG, "SearchServices", "StatisticsActivity")
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
            AppLogger.logButtonClick(TAG, "CardSettings", "Navegar a configuraciones")
            it.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    AppLogger.logNavigationStart(TAG, "SearchServices", "SettingsActivity")
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
            AppLogger.logButtonClick(TAG, "CardBluetooth", "Navegar a configuración Bluetooth")
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
        
        val setupTime = System.currentTimeMillis() - startTime
        AppLogger.i(TAG, "Acciones rápidas configuradas en ${setupTime}ms")
    }

    private fun setupFAB() {
        AppLogger.d(TAG, "Configurando FAB")
        fabQuickAdd.setOnClickListener {
            AppLogger.logButtonClick(TAG, "FABQuickAdd", "Acceso rápido al servicio más usado")
            // Navigate to the most used service
            val mostUsedService = servicios.toList().toServiceItems().firstOrNull()
            mostUsedService?.let {
                AppLogger.d(TAG, "Navegando al servicio más usado: ${it.name}")
                navigateToMainActivity(it)
            }
        }
        AppLogger.d(TAG, "FAB configurado")
    }

    private fun filterServices(query: String) {
        val startTime = System.currentTimeMillis()
        
        if (query.isBlank()) {
            // Si no hay búsqueda, mostrar servicios según estado actual
            serviceAdapter.updateServices(getVisibleServices())
        } else {
            // Si hay búsqueda, usar ServiceRepository para filtrar
            val filteredServices = serviceRepository.searchServices(query)
            serviceAdapter.updateServices(filteredServices)
        }
        
        val filterTime = System.currentTimeMillis() - startTime
        AppLogger.logSearchQuery(TAG, query, serviceAdapter.itemCount, filterTime)
    }
    
    private fun getVisibleServices(): List<ServiceItem> {
        return if (showAllServices) {
            // Mostrar todos los servicios (sin botón "Ver más")
            AppLogger.d(TAG, "Mostrando todos los ${servicios.size} servicios")
            serviceRepository.getAllServices()
        } else {
            // Mostrar solo hasta "Reseteo de Cliente" + botón "Ver más"
            AppLogger.d(TAG, "Mostrando servicios limitados hasta reseteo + Ver más")
            serviceRepository.getServicesWithViewMore(15) // Show first 16 services (0-15) + "Ver más"
        }
    }
    
    private fun updateVisibleServices() {
        serviceAdapter.updateServices(getVisibleServices())
    }

    private fun navigateToMainActivity(service: ServiceItem) {
        AppLogger.logNavigationStart(TAG, "SearchServices", "MainActivity", "Servicio: ${service.name}")
        AppLogger.logServiceSelection(TAG, service.name, service.id)

        loadingHelper.showLoadingAndNavigate(
            targetActivity = MainActivity::class.java,
            message = "Cargando ${service.name}...",
            duration = 1200L
        ) {
            putExtra("SERVICE_ITEM", service)
            putExtra("SERVICE_TYPE", service.id)
            AppLogger.d(TAG, "Extra 'SERVICE_ITEM' y 'SERVICE_TYPE' agregados al Intent")
        }
    }

    private fun checkBluetoothAndNavigate() {
        AppLogger.i(TAG, "Verificando estado Bluetooth")
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val deviceAddress = sharedPreferences.getString("BluetoothDeviceAddress", null)
        AppLogger.d(TAG, "Dirección Bluetooth guardada: ${deviceAddress ?: "ninguna"}")
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val device = deviceAddress?.let { bluetoothAdapter?.getRemoteDevice(it) }

        if (deviceAddress == null || device == null) {
            AppLogger.w(TAG, "No se encontró dispositivo Bluetooth configurado")
            showSnackbar("No se encontró dispositivo Bluetooth configurado")
        } else {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            AppLogger.i(TAG, "Dispositivo Bluetooth encontrado: ${device.name ?: device.address}")
        }

        AppLogger.logNavigationStart(TAG, "SearchServices", "Bt")
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

    @RequiresApi(Build.VERSION_CODES.S)
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
                val criticalPermissions = deniedPermissions.filter {
                    it == Manifest.permission.CALL_PHONE || 
                    it == Manifest.permission.READ_SMS ||
                    it == Manifest.permission.BLUETOOTH_CONNECT ||
                    it == Manifest.permission.BLUETOOTH_SCAN
                }

                if (criticalPermissions.isNotEmpty()) {
                    showPermissionExplanationDialog(criticalPermissions)
                } else {
                    showSnackbar("Algunos permisos opcionales fueron denegados")
                }
            } else {
                showSnackbar("Todos los permisos han sido otorgados")
            }
        }
    }

    private fun showPermissionExplanationDialog(deniedPermissions: List<String>) {
        val permissionNames = deniedPermissions.map { permission ->
            when (permission) {
                Manifest.permission.CALL_PHONE -> "Realizar llamadas telefónicas"
                Manifest.permission.READ_SMS -> "Leer mensajes SMS"
                Manifest.permission.BLUETOOTH_CONNECT -> "Conectar con dispositivos Bluetooth"
                Manifest.permission.BLUETOOTH_SCAN -> "Buscar dispositivos Bluetooth"
                else -> permission
            }
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Permisos Requeridos")
            .setMessage("La aplicación necesita los siguientes permisos para funcionar correctamente:\n\n${permissionNames.joinToString("\n• ", "• ")}\n\nPuedes habilitarlos en Configuración > Aplicaciones > Genio Tigo > Permisos")
            .setPositiveButton("Ir a Configuración") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Continuar") { _, _ ->
                showSnackbar("Algunas funciones pueden no estar disponibles")
            }
            .setCancelable(false)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", packageName, null)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            showSnackbar("No se pudo abrir la configuración")
        }
    }

    private fun diagnosticarIconos() {
        AppLogger.i(TAG, "=== DIAGNÓSTICO DE ICONOS ===")
        val startTime = System.currentTimeMillis()
        
        // Verificar existencia de vistas de iconos
        val printIcon = findViewById<android.widget.ImageView>(R.id.print)
        val statsIcon = findViewById<android.widget.ImageView>(R.id.stats)
        val settingsIcon = findViewById<android.widget.ImageView>(R.id.settings)
        val btIcon = findViewById<android.widget.ImageView>(R.id.bt)
        
        AppLogger.d(TAG, "Print Icon encontrado: ${printIcon != null}")
        AppLogger.d(TAG, "Stats Icon encontrado: ${statsIcon != null}")
        AppLogger.d(TAG, "Settings Icon encontrado: ${settingsIcon != null}")
        AppLogger.d(TAG, "BT Icon encontrado: ${btIcon != null}")
        
        // Verificar drawable resources
        printIcon?.let { icon ->
            AppLogger.debugViewState(TAG, "Print Icon", icon)
            
            val drawable = ContextCompat.getDrawable(this, R.drawable.ic_print)
            AppLogger.logImageLoad(TAG, "ic_print", R.drawable.ic_print, drawable != null)
            
            // Intentar aplicar el tint manualmente
            try {
                val color = ContextCompat.getColor(this, R.color.md_theme_light_primary)
                icon.setColorFilter(color)
                AppLogger.i(TAG, "ColorFilter aplicado a Print Icon - Color: $color")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error aplicando ColorFilter a Print Icon", e)
            }
        }
        
        statsIcon?.let { icon ->
            AppLogger.debugViewState(TAG, "Stats Icon", icon)
            
            val drawable = ContextCompat.getDrawable(this, R.drawable.ic_chart)
            AppLogger.logImageLoad(TAG, "ic_chart", R.drawable.ic_chart, drawable != null)
            
            // Intentar aplicar el tint manualmente
            try {
                val color = ContextCompat.getColor(this, R.color.md_theme_light_secondary)
                icon.setColorFilter(color)
                AppLogger.i(TAG, "ColorFilter aplicado a Stats Icon - Color: $color")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error aplicando ColorFilter a Stats Icon", e)
            }
        }
        
        settingsIcon?.let { icon ->
            AppLogger.debugViewState(TAG, "Settings Icon", icon)
            
            val drawable = ContextCompat.getDrawable(this, R.drawable.ic_settings)
            AppLogger.logImageLoad(TAG, "ic_settings", R.drawable.ic_settings, drawable != null)
            
            // Intentar aplicar el tint manualmente
            try {
                val color = ContextCompat.getColor(this, R.color.md_theme_light_tertiary)
                icon.setColorFilter(color)
                AppLogger.i(TAG, "ColorFilter aplicado a Settings Icon - Color: $color")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error aplicando ColorFilter a Settings Icon", e)
            }
        }
        
        btIcon?.let { icon ->
            AppLogger.debugViewState(TAG, "BT Icon", icon)
            
            val drawable = ContextCompat.getDrawable(this, R.drawable.ic_bluetooth)
            AppLogger.logImageLoad(TAG, "ic_bluetooth", R.drawable.ic_bluetooth, drawable != null)
            
            // Intentar aplicar el tint manualmente
            try {
                val color = ContextCompat.getColor(this, R.color.status_info)
                icon.setColorFilter(color)
                AppLogger.i(TAG, "ColorFilter aplicado a BT Icon - Color: $color")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error aplicando ColorFilter a BT Icon", e)
            }
        }
        
        // Verificar colores disponibles
        try {
            AppLogger.debugColorValue(TAG, "md_theme_light_primary", ContextCompat.getColor(this, R.color.md_theme_light_primary))
            AppLogger.debugColorValue(TAG, "md_theme_light_secondary", ContextCompat.getColor(this, R.color.md_theme_light_secondary))
            AppLogger.debugColorValue(TAG, "md_theme_light_tertiary", ContextCompat.getColor(this, R.color.md_theme_light_tertiary))
            AppLogger.debugColorValue(TAG, "status_info", ContextCompat.getColor(this, R.color.status_info))
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error obteniendo colores del tema", e)
        }
        
        val diagnosticTime = System.currentTimeMillis() - startTime
        AppLogger.i(TAG, "=== FIN DIAGNÓSTICO DE ICONOS (${diagnosticTime}ms) ===")
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(
            findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_SHORT
        ).show()
    }
}