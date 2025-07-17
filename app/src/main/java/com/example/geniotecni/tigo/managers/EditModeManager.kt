package com.example.geniotecni.tigo.managers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream

class EditModeManager(
    private val context: Context,
    private val activity: AppCompatActivity,
    private val rootLayout: ConstraintLayout,
    private val preferencesManager: PreferencesManager
) {

    companion object {
        private const val COMPONENT_SCALE_KEY = "component_scale"
        private const val COMPONENT_TEXT_SIZE_KEY = "component_text_size"
        private const val COMPONENT_LETTER_SPACING_KEY = "component_letter_spacing"
        private const val COMPONENT_POSITION_X_KEY = "component_position_x"
        private const val COMPONENT_POSITION_Y_KEY = "component_position_y"
        private const val SERVICE_CONFIG_KEY = "service_config"
    }

    private var isEditMode = false
    private var selectedComponent: View? = null
    private var currentServiceType = 0
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var isDragging = false

    // Edit mode UI components
    private lateinit var editButton: Button
    private lateinit var cancelButton: Button
    private lateinit var saveButton: Button
    private lateinit var resetButton: Button
    private lateinit var exportButton: Button
    private lateinit var importButton: Button
    private lateinit var selectionIndicator: View
    private lateinit var editModeOverlay: View
    private lateinit var editControlsPanel: LinearLayout
    
    // Edit controls
    private lateinit var editControlsTitle: TextView
    private lateinit var scaleSeekBar: SeekBar
    private lateinit var scaleValue: TextView
    private lateinit var textSizeSeekBar: SeekBar
    private lateinit var textSizeValue: TextView
    private lateinit var letterSpacingSeekBar: SeekBar
    private lateinit var letterSpacingValue: TextView
    private lateinit var textSizeControl: LinearLayout
    private lateinit var letterSpacingControl: LinearLayout
    private lateinit var applyChangesButton: Button
    private lateinit var cancelChangesButton: Button

    // Activity result launchers
    private val createDocumentLauncher = activity.registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { saveConfigurationToUri(it) }
    }

    private val openDocumentLauncher = activity.registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { loadConfigurationFromUri(it) }
    }

    fun initializeEditMode() {
        // Find all UI components
        editButton = activity.findViewById(com.example.geniotecni.tigo.R.id.editButton)
        cancelButton = activity.findViewById(com.example.geniotecni.tigo.R.id.cancelButton)
        saveButton = activity.findViewById(com.example.geniotecni.tigo.R.id.saveButton)
        resetButton = activity.findViewById(com.example.geniotecni.tigo.R.id.resetButton)
        exportButton = activity.findViewById(com.example.geniotecni.tigo.R.id.exportButton)
        importButton = activity.findViewById(com.example.geniotecni.tigo.R.id.importButton)
        selectionIndicator = activity.findViewById(com.example.geniotecni.tigo.R.id.selectionIndicator)
        editModeOverlay = activity.findViewById(com.example.geniotecni.tigo.R.id.editModeOverlay)
        editControlsPanel = activity.findViewById(com.example.geniotecni.tigo.R.id.editControlsPanel)
        
        // Find edit controls
        editControlsTitle = activity.findViewById(com.example.geniotecni.tigo.R.id.editControlsTitle)
        scaleSeekBar = activity.findViewById(com.example.geniotecni.tigo.R.id.scaleSeekBar)
        scaleValue = activity.findViewById(com.example.geniotecni.tigo.R.id.scaleValue)
        textSizeSeekBar = activity.findViewById(com.example.geniotecni.tigo.R.id.textSizeSeekBar)
        textSizeValue = activity.findViewById(com.example.geniotecni.tigo.R.id.textSizeValue)
        letterSpacingSeekBar = activity.findViewById(com.example.geniotecni.tigo.R.id.letterSpacingSeekBar)
        letterSpacingValue = activity.findViewById(com.example.geniotecni.tigo.R.id.letterSpacingValue)
        textSizeControl = activity.findViewById(com.example.geniotecni.tigo.R.id.textSizeControl)
        letterSpacingControl = activity.findViewById(com.example.geniotecni.tigo.R.id.letterSpacingControl)
        applyChangesButton = activity.findViewById(com.example.geniotecni.tigo.R.id.applyChangesButton)
        cancelChangesButton = activity.findViewById(com.example.geniotecni.tigo.R.id.cancelChangesButton)

        setupEditModeListeners()
        setupComponentTouchListeners(rootLayout)
        loadCurrentConfiguration()
    }

    private fun setupEditModeListeners() {
        editButton.setOnClickListener {
            toggleEditMode()
        }

        cancelButton.setOnClickListener {
            exitEditMode()
        }

        saveButton.setOnClickListener {
            showSaveDialog()
        }

        resetButton.setOnClickListener {
            resetToDefault()
        }

        exportButton.setOnClickListener {
            exportConfiguration()
        }

        importButton.setOnClickListener {
            importConfiguration()
        }

        // Setup edit controls
        setupEditControls()
    }

    private fun setupEditControls() {
        scaleSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && selectedComponent != null) {
                    val scale = 0.5f + (progress / 100f) * 1.5f
                    selectedComponent?.scaleX = scale
                    selectedComponent?.scaleY = scale
                    scaleValue.text = String.format("%.1f", scale)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        textSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && selectedComponent is TextView) {
                    val textSize = 8f + progress
                    (selectedComponent as TextView).textSize = textSize
                    textSizeValue.text = "${textSize.toInt()}sp"
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        letterSpacingSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && selectedComponent is TextView) {
                    val letterSpacing = progress / 100f
                    (selectedComponent as TextView).letterSpacing = letterSpacing
                    letterSpacingValue.text = String.format("%.2f", letterSpacing)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        applyChangesButton.setOnClickListener {
            saveComponentConfiguration(selectedComponent)
            hideEditControls()
        }

        cancelChangesButton.setOnClickListener {
            loadComponentConfiguration(selectedComponent)
            hideEditControls()
        }
    }

    private fun setupComponentTouchListeners(viewGroup: ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            
            child.setOnTouchListener { view, event ->
                if (isEditMode && isEditableComponent(view)) {
                    handleComponentTouch(view, event)
                    true
                } else {
                    false
                }
            }

            if (child is ViewGroup) {
                setupComponentTouchListeners(child)
            }
        }
    }

    private fun isEditableComponent(view: View): Boolean {
        return when (view.id) {
            com.example.geniotecni.tigo.R.id.serviceImage,
            com.example.geniotecni.tigo.R.id.serviceTitle,
            com.example.geniotecni.tigo.R.id.phoneLabel,
            com.example.geniotecni.tigo.R.id.phoneInput,
            com.example.geniotecni.tigo.R.id.cedulaLabel,
            com.example.geniotecni.tigo.R.id.cedulaInput,
            com.example.geniotecni.tigo.R.id.amountLabel,
            com.example.geniotecni.tigo.R.id.amountInput,
            com.example.geniotecni.tigo.R.id.dateLabel,
            com.example.geniotecni.tigo.R.id.dateInput,
            com.example.geniotecni.tigo.R.id.confirmLabel,
            com.example.geniotecni.tigo.R.id.confirmButton,
            com.example.geniotecni.tigo.R.id.infoButton,
            com.example.geniotecni.tigo.R.id.increaseButton,
            com.example.geniotecni.tigo.R.id.decreaseButton,
            com.example.geniotecni.tigo.R.id.resetAmountButton -> true
            else -> false
        }
    }

    private fun handleComponentTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                selectComponent(view)
                dragStartX = event.rawX
                dragStartY = event.rawY
                isDragging = false
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - dragStartX
                val deltaY = event.rawY - dragStartY
                
                if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                    isDragging = true
                    if (isMovableComponent(view)) {
                        view.translationX += deltaX
                        view.translationY += deltaY
                        dragStartX = event.rawX
                        dragStartY = event.rawY
                    }
                }
                return true
            }
            
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    showEditControls(view)
                }
                return true
            }
        }
        return false
    }

    private fun isMovableComponent(view: View): Boolean {
        return when (view.id) {
            com.example.geniotecni.tigo.R.id.confirmButton,
            com.example.geniotecni.tigo.R.id.infoButton,
            com.example.geniotecni.tigo.R.id.increaseButton,
            com.example.geniotecni.tigo.R.id.decreaseButton,
            com.example.geniotecni.tigo.R.id.resetAmountButton -> true
            else -> false
        }
    }

    private fun selectComponent(view: View) {
        selectedComponent = view
        
        // Update selection indicator
        val location = IntArray(2)
        view.getLocationInWindow(location)
        
        val layoutParams = selectionIndicator.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.width = view.width
        layoutParams.height = view.height
        selectionIndicator.layoutParams = layoutParams
        
        selectionIndicator.x = view.x
        selectionIndicator.y = view.y
        selectionIndicator.visibility = View.VISIBLE
    }

    private fun showEditControls(component: View) {
        selectedComponent = component
        val componentName = getComponentDisplayName(component)
        editControlsTitle.text = "Editando: $componentName"

        // Load current values
        val componentKey = getComponentKey(component)
        val currentScale = preferencesManager.getFloat(
            getServiceComponentKey(COMPONENT_SCALE_KEY, componentKey), 1.0f
        )

        // Setup scale control
        scaleSeekBar.progress = ((currentScale - 0.5f) * 100f / 1.5f).toInt()
        scaleValue.text = String.format("%.1f", currentScale)

        // Show/hide text controls based on component type
        val isTextComponent = component is TextView || component is EditText
        textSizeControl.visibility = if (isTextComponent) View.VISIBLE else View.GONE
        letterSpacingControl.visibility = if (isTextComponent) View.VISIBLE else View.GONE

        if (isTextComponent) {
            val currentTextSize = preferencesManager.getFloat(
                getServiceComponentKey(COMPONENT_TEXT_SIZE_KEY, componentKey), 16.0f
            )
            val currentLetterSpacing = preferencesManager.getFloat(
                getServiceComponentKey(COMPONENT_LETTER_SPACING_KEY, componentKey), 0.0f
            )

            textSizeSeekBar.progress = (currentTextSize - 8f).toInt()
            textSizeValue.text = "${currentTextSize.toInt()}sp"
            
            letterSpacingSeekBar.progress = (currentLetterSpacing * 100f).toInt()
            letterSpacingValue.text = String.format("%.2f", currentLetterSpacing)
        }

        editControlsPanel.visibility = View.VISIBLE
    }

    private fun hideEditControls() {
        editControlsPanel.visibility = View.GONE
        selectionIndicator.visibility = View.GONE
        selectedComponent = null
    }

    private fun getComponentDisplayName(component: View): String {
        return when (component.id) {
            com.example.geniotecni.tigo.R.id.serviceImage -> "Imagen del Servicio"
            com.example.geniotecni.tigo.R.id.serviceTitle -> "Título del Servicio"
            com.example.geniotecni.tigo.R.id.phoneLabel -> "Etiqueta Teléfono"
            com.example.geniotecni.tigo.R.id.phoneInput -> "Campo Teléfono"
            com.example.geniotecni.tigo.R.id.cedulaLabel -> "Etiqueta Cédula"
            com.example.geniotecni.tigo.R.id.cedulaInput -> "Campo Cédula"
            com.example.geniotecni.tigo.R.id.amountLabel -> "Etiqueta Monto"
            com.example.geniotecni.tigo.R.id.amountInput -> "Campo Monto"
            com.example.geniotecni.tigo.R.id.dateLabel -> "Etiqueta Fecha"
            com.example.geniotecni.tigo.R.id.dateInput -> "Campo Fecha"
            com.example.geniotecni.tigo.R.id.confirmLabel -> "Etiqueta Confirmar"
            com.example.geniotecni.tigo.R.id.confirmButton -> "Botón Confirmar"
            com.example.geniotecni.tigo.R.id.infoButton -> "Botón Info"
            com.example.geniotecni.tigo.R.id.increaseButton -> "Botón Aumentar"
            com.example.geniotecni.tigo.R.id.decreaseButton -> "Botón Disminuir"
            com.example.geniotecni.tigo.R.id.resetAmountButton -> "Botón Reset"
            else -> "Componente"
        }
    }

    private fun saveComponentConfiguration(component: View?) {
        component?.let {
            val componentKey = getComponentKey(it)
            val scale = it.scaleX
            
            preferencesManager.putFloat(
                getServiceComponentKey(COMPONENT_SCALE_KEY, componentKey), scale
            )
            preferencesManager.putFloat(
                getServiceComponentKey(COMPONENT_POSITION_X_KEY, componentKey), it.translationX
            )
            preferencesManager.putFloat(
                getServiceComponentKey(COMPONENT_POSITION_Y_KEY, componentKey), it.translationY
            )

            if (it is TextView) {
                preferencesManager.putFloat(
                    getServiceComponentKey(COMPONENT_TEXT_SIZE_KEY, componentKey), it.textSize
                )
                preferencesManager.putFloat(
                    getServiceComponentKey(COMPONENT_LETTER_SPACING_KEY, componentKey), it.letterSpacing
                )
            }
            
            showToast("Configuración guardada")
        }
    }

    private fun loadComponentConfiguration(component: View?) {
        component?.let {
            val componentKey = getComponentKey(it)
            
            val scale = preferencesManager.getFloat(
                getServiceComponentKey(COMPONENT_SCALE_KEY, componentKey), 1.0f
            )
            val positionX = preferencesManager.getFloat(
                getServiceComponentKey(COMPONENT_POSITION_X_KEY, componentKey), 0f
            )
            val positionY = preferencesManager.getFloat(
                getServiceComponentKey(COMPONENT_POSITION_Y_KEY, componentKey), 0f
            )

            it.scaleX = scale
            it.scaleY = scale
            it.translationX = positionX
            it.translationY = positionY

            if (it is TextView) {
                val textSize = preferencesManager.getFloat(
                    getServiceComponentKey(COMPONENT_TEXT_SIZE_KEY, componentKey), 16.0f
                )
                val letterSpacing = preferencesManager.getFloat(
                    getServiceComponentKey(COMPONENT_LETTER_SPACING_KEY, componentKey), 0.0f
                )
                
                it.textSize = textSize
                it.letterSpacing = letterSpacing
            }
        }
    }

    private fun getComponentKey(component: View): String {
        return when (component.id) {
            com.example.geniotecni.tigo.R.id.serviceImage -> "serviceImage"
            com.example.geniotecni.tigo.R.id.serviceTitle -> "serviceTitle"
            com.example.geniotecni.tigo.R.id.phoneLabel -> "phoneLabel"
            com.example.geniotecni.tigo.R.id.phoneInput -> "phoneInput"
            com.example.geniotecni.tigo.R.id.cedulaLabel -> "cedulaLabel"
            com.example.geniotecni.tigo.R.id.cedulaInput -> "cedulaInput"
            com.example.geniotecni.tigo.R.id.amountLabel -> "amountLabel"
            com.example.geniotecni.tigo.R.id.amountInput -> "amountInput"
            com.example.geniotecni.tigo.R.id.dateLabel -> "dateLabel"
            com.example.geniotecni.tigo.R.id.dateInput -> "dateInput"
            com.example.geniotecni.tigo.R.id.confirmLabel -> "confirmLabel"
            com.example.geniotecni.tigo.R.id.confirmButton -> "confirmButton"
            com.example.geniotecni.tigo.R.id.infoButton -> "infoButton"
            com.example.geniotecni.tigo.R.id.increaseButton -> "increaseButton"
            com.example.geniotecni.tigo.R.id.decreaseButton -> "decreaseButton"
            com.example.geniotecni.tigo.R.id.resetAmountButton -> "resetAmountButton"
            else -> "unknown_${component.id}"
        }
    }

    private fun getServiceComponentKey(baseKey: String, componentKey: String): String {
        return "${baseKey}_service_${currentServiceType}_${componentKey}"
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode
        
        if (isEditMode) {
            enterEditMode()
        } else {
            exitEditMode()
        }
    }

    private fun enterEditMode() {
        editButton.visibility = View.GONE
        cancelButton.visibility = View.VISIBLE
        saveButton.visibility = View.VISIBLE
        resetButton.visibility = View.VISIBLE
        exportButton.visibility = View.VISIBLE
        importButton.visibility = View.VISIBLE
        editModeOverlay.visibility = View.VISIBLE
        
        showToast("Modo de edición activado. Toca componentes para editarlos.")
    }

    private fun exitEditMode() {
        editButton.visibility = View.VISIBLE
        cancelButton.visibility = View.GONE
        saveButton.visibility = View.GONE
        resetButton.visibility = View.GONE
        exportButton.visibility = View.GONE
        importButton.visibility = View.GONE
        editModeOverlay.visibility = View.GONE
        selectionIndicator.visibility = View.GONE
        editControlsPanel.visibility = View.GONE
        selectedComponent = null
        
        showToast("Modo de edición desactivado.")
    }

    private fun showSaveDialog() {
        val input = EditText(context)
        input.hint = "Nombre del archivo de configuración"
        
        AlertDialog.Builder(context)
            .setTitle("Guardar Configuración")
            .setMessage("Ingrese el nombre para el archivo de configuración:")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val fileName = input.text.toString().trim()
                if (fileName.isNotEmpty()) {
                    saveConfiguration(fileName)
                } else {
                    showToast("Por favor ingrese un nombre válido")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveConfiguration(fileName: String) {
        try {
            val configFileName = "${fileName}_service_${currentServiceType}_${System.currentTimeMillis()}.json"
            createDocumentLauncher.launch(configFileName)
        } catch (e: Exception) {
            showToast("Error al iniciar guardado: ${e.message}")
        }
    }

    private fun exportConfiguration() {
        showSaveDialog()
    }

    private fun saveConfigurationToUri(uri: Uri) {
        try {
            val config = JSONObject()
            config.put("serviceType", currentServiceType)
            config.put("version", "3.0")
            config.put("timestamp", System.currentTimeMillis())
            
            val componentsConfig = JSONObject()
            
            // Save configuration for all components
            val editableComponents = listOf(
                "serviceImage", "serviceTitle", "phoneLabel", "phoneInput",
                "cedulaLabel", "cedulaInput", "amountLabel", "amountInput",
                "dateLabel", "dateInput", "confirmLabel", "confirmButton", "infoButton",
                "increaseButton", "decreaseButton", "resetAmountButton"
            )
            
            editableComponents.forEach { componentKey ->
                val componentConfig = JSONObject()
                
                componentConfig.put("scale", preferencesManager.getFloat(
                    getServiceComponentKey(COMPONENT_SCALE_KEY, componentKey), 1.0f
                ))
                componentConfig.put("positionX", preferencesManager.getFloat(
                    getServiceComponentKey(COMPONENT_POSITION_X_KEY, componentKey), 0f
                ))
                componentConfig.put("positionY", preferencesManager.getFloat(
                    getServiceComponentKey(COMPONENT_POSITION_Y_KEY, componentKey), 0f
                ))
                componentConfig.put("textSize", preferencesManager.getFloat(
                    getServiceComponentKey(COMPONENT_TEXT_SIZE_KEY, componentKey), 16.0f
                ))
                componentConfig.put("letterSpacing", preferencesManager.getFloat(
                    getServiceComponentKey(COMPONENT_LETTER_SPACING_KEY, componentKey), 0.0f
                ))
                
                componentsConfig.put(componentKey, componentConfig)
            }
            
            config.put("components", componentsConfig)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(config.toString(2).toByteArray())
            }

            showToast("Configuración exportada exitosamente")
            exitEditMode()
        } catch (e: Exception) {
            showToast("Error al exportar: ${e.message}")
        }
    }

    private fun importConfiguration() {
        try {
            openDocumentLauncher.launch(arrayOf("application/json"))
        } catch (e: Exception) {
            showToast("Error al iniciar importación: ${e.message}")
        }
    }

    private fun loadConfigurationFromUri(uri: Uri) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val content = inputStream.bufferedReader().use { it.readText() }
                val config = JSONObject(content)
                
                val serviceType = config.optInt("serviceType", currentServiceType)
                val components = config.optJSONObject("components")
                
                components?.let { componentsConfig ->
                    val componentKeys = componentsConfig.keys()
                    while (componentKeys.hasNext()) {
                        val componentKey = componentKeys.next()
                        val componentConfig = componentsConfig.getJSONObject(componentKey)
                        
                        val scale = componentConfig.optDouble("scale", 1.0).toFloat()
                        val positionX = componentConfig.optDouble("positionX", 0.0).toFloat()
                        val positionY = componentConfig.optDouble("positionY", 0.0).toFloat()
                        val textSize = componentConfig.optDouble("textSize", 16.0).toFloat()
                        val letterSpacing = componentConfig.optDouble("letterSpacing", 0.0).toFloat()
                        
                        preferencesManager.putFloat(
                            getServiceComponentKey(COMPONENT_SCALE_KEY, componentKey), scale
                        )
                        preferencesManager.putFloat(
                            getServiceComponentKey(COMPONENT_POSITION_X_KEY, componentKey), positionX
                        )
                        preferencesManager.putFloat(
                            getServiceComponentKey(COMPONENT_POSITION_Y_KEY, componentKey), positionY
                        )
                        preferencesManager.putFloat(
                            getServiceComponentKey(COMPONENT_TEXT_SIZE_KEY, componentKey), textSize
                        )
                        preferencesManager.putFloat(
                            getServiceComponentKey(COMPONENT_LETTER_SPACING_KEY, componentKey), letterSpacing
                        )
                    }
                }
                
                loadCurrentConfiguration()
                showToast("Configuración importada exitosamente")
            }
        } catch (e: Exception) {
            showToast("Error al importar: ${e.message}")
        }
    }

    private fun resetToDefault() {
        AlertDialog.Builder(context)
            .setTitle("Resetear Configuración")
            .setMessage("¿Estás seguro de que quieres resetear todos los componentes a sus valores por defecto?")
            .setPositiveButton("Resetear") { _, _ ->
                performReset()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun performReset() {
        val editableComponents = listOf(
            "serviceImage", "serviceTitle", "phoneLabel", "phoneInput",
            "cedulaLabel", "cedulaInput", "amountLabel", "amountInput",
            "dateLabel", "dateInput", "confirmLabel", "confirmButton", "infoButton",
            "increaseButton", "decreaseButton", "resetAmountButton"
        )
        
        editableComponents.forEach { componentKey ->
            // Clear all saved preferences for this component
            preferencesManager.putFloat(getServiceComponentKey(COMPONENT_SCALE_KEY, componentKey), 1.0f)
            preferencesManager.putFloat(getServiceComponentKey(COMPONENT_POSITION_X_KEY, componentKey), 0f)
            preferencesManager.putFloat(getServiceComponentKey(COMPONENT_POSITION_Y_KEY, componentKey), 0f)
            preferencesManager.putFloat(getServiceComponentKey(COMPONENT_TEXT_SIZE_KEY, componentKey), 16.0f)
            preferencesManager.putFloat(getServiceComponentKey(COMPONENT_LETTER_SPACING_KEY, componentKey), 0.0f)
        }
        
        loadCurrentConfiguration()
        showToast("Configuración reseteada a valores por defecto")
    }

    private fun loadCurrentConfiguration() {
        val editableComponentIds = listOf(
            com.example.geniotecni.tigo.R.id.serviceImage,
            com.example.geniotecni.tigo.R.id.serviceTitle,
            com.example.geniotecni.tigo.R.id.phoneLabel,
            com.example.geniotecni.tigo.R.id.phoneInput,
            com.example.geniotecni.tigo.R.id.cedulaLabel,
            com.example.geniotecni.tigo.R.id.cedulaInput,
            com.example.geniotecni.tigo.R.id.amountLabel,
            com.example.geniotecni.tigo.R.id.amountInput,
            com.example.geniotecni.tigo.R.id.dateLabel,
            com.example.geniotecni.tigo.R.id.dateInput,
            com.example.geniotecni.tigo.R.id.confirmLabel,
            com.example.geniotecni.tigo.R.id.confirmButton,
            com.example.geniotecni.tigo.R.id.infoButton,
            com.example.geniotecni.tigo.R.id.increaseButton,
            com.example.geniotecni.tigo.R.id.decreaseButton,
            com.example.geniotecni.tigo.R.id.resetAmountButton
        )
        
        editableComponentIds.forEach { componentId ->
            val component = rootLayout.findViewById<View>(componentId)
            component?.let { loadComponentConfiguration(it) }
        }
    }

    fun setCurrentServiceType(serviceType: Int) {
        currentServiceType = serviceType
        loadCurrentConfiguration()
    }

    fun handleTouchEvent(event: MotionEvent): Boolean {
        return isEditMode
    }

    fun isInEditMode(): Boolean = isEditMode

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}