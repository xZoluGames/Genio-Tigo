package com.example.geniotecni.tigo.managers

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
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
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.example.geniotecni.tigo.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONObject
import java.io.File

class EditModeManager(
    private val context: Context,
    private val activity: AppCompatActivity,
    private val rootLayout: CoordinatorLayout,
    private val preferencesManager: PreferencesManager
) {

    companion object {
        private const val COMPONENT_SCALE_KEY = "component_scale"
        private const val COMPONENT_TEXT_SIZE_KEY = "component_text_size"
        private const val COMPONENT_LETTER_SPACING_KEY = "component_letter_spacing"
        private const val COMPONENT_POSITION_X_KEY = "component_position_x"
        private const val COMPONENT_POSITION_Y_KEY = "component_position_y"
        private const val SERVICE_CONFIG_KEY = "service_config"
        private const val CONFIG_VERSION = "3.1"
    }

    private var isEditMode = false
    private var selectedComponent: View? = null
    private var currentServiceType = 0
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var isDragging = false
    private var originalPositions = mutableMapOf<View, Pair<Float, Float>>()

    // Edit mode UI components
    private lateinit var editButton: MaterialButton
    private lateinit var editButtonsContainer: View
    private lateinit var cancelButton: MaterialButton
    private lateinit var saveButton: MaterialButton
    private lateinit var resetButton: MaterialButton
    private lateinit var exportButton: MaterialButton
    private lateinit var importButton: MaterialButton
    private lateinit var selectionIndicator: View
    private lateinit var editModeOverlay: View
    private lateinit var editControlsPanel: MaterialCardView

    // Edit controls
    private lateinit var editControlsTitle: TextView
    private lateinit var scaleSlider: Slider
    private lateinit var textSizeSlider: Slider
    private lateinit var letterSpacingSlider: Slider
    private lateinit var textSizeControl: LinearLayout
    private lateinit var letterSpacingControl: LinearLayout
    private lateinit var applyChangesButton: MaterialButton
    private lateinit var cancelChangesButton: MaterialButton

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
        try {
            findViews()
            setupListeners()
            setupComponentTouchListeners(rootLayout)
            loadCurrentConfiguration()
        } catch (e: Exception) {
            e.printStackTrace()
            showSnackbar("Error al inicializar modo edición: ${e.message}")
        }
    }

    private fun findViews() {
        // Find all UI components
        editButton = activity.findViewById(R.id.editButton)
        editButtonsContainer = activity.findViewById(R.id.editButtonsContainer)
        cancelButton = activity.findViewById(R.id.cancelButton)
        saveButton = activity.findViewById(R.id.saveButton)
        resetButton = activity.findViewById(R.id.resetButton)
        exportButton = activity.findViewById(R.id.exportButton)
        importButton = activity.findViewById(R.id.importButton)
        selectionIndicator = activity.findViewById(R.id.selectionIndicator)
        editModeOverlay = activity.findViewById(R.id.editModeOverlay)
        editControlsPanel = activity.findViewById(R.id.editControlsPanel)

        // Find edit controls
        editControlsTitle = activity.findViewById(R.id.editControlsTitle)
        scaleSlider = activity.findViewById(R.id.scaleSlider)
        textSizeSlider = activity.findViewById(R.id.textSizeSlider)
        letterSpacingSlider = activity.findViewById(R.id.letterSpacingSlider)
        textSizeControl = activity.findViewById(R.id.textSizeControl)
        letterSpacingControl = activity.findViewById(R.id.letterSpacingControl)
        applyChangesButton = activity.findViewById(R.id.applyChangesButton)
        cancelChangesButton = activity.findViewById(R.id.cancelChangesButton)
    }

    private fun setupListeners() {
        editButton.setOnClickListener {
            toggleEditMode()
        }

        cancelButton.setOnClickListener {
            exitEditMode(false)
        }

        saveButton.setOnClickListener {
            saveAllChanges()
        }

        resetButton.setOnClickListener {
            showResetConfirmation()
        }

        exportButton.setOnClickListener {
            exportConfiguration()
        }

        importButton.setOnClickListener {
            importConfiguration()
        }

        setupEditControls()
    }

    private fun setupEditControls() {
        scaleSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser && selectedComponent != null) {
                selectedComponent?.apply {
                    scaleX = value
                    scaleY = value
                    updateSelectionIndicator()
                }
            }
        }

        textSizeSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser && selectedComponent != null) {
                when (val component = selectedComponent) {
                    is TextView -> component.textSize = value
                    is TextInputEditText -> component.textSize = value
                }
            }
        }

        letterSpacingSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser && selectedComponent != null) {
                when (val component = selectedComponent) {
                    is TextView -> component.letterSpacing = value
                    is TextInputEditText -> component.letterSpacing = value
                }
            }
        }

        applyChangesButton.setOnClickListener {
            saveComponentConfiguration(selectedComponent)
            hideEditControls()
            showSnackbar("Cambios aplicados")
        }

        cancelChangesButton.setOnClickListener {
            loadComponentConfiguration(selectedComponent)
            hideEditControls()
        }
    }

    private fun setupComponentTouchListeners(viewGroup: ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)

            if (isEditableComponent(child)) {
                child.setOnTouchListener { view, event ->
                    if (isEditMode) {
                        handleComponentTouch(view, event)
                        true
                    } else {
                        false
                    }
                }
            }

            if (child is ViewGroup && child !is TextInputLayout) {
                setupComponentTouchListeners(child)
            }
        }
    }

    private fun isEditableComponent(view: View): Boolean {
        return when (view.id) {
            R.id.serviceImage,
            R.id.serviceTitle,
            R.id.phoneInputLayout,
            R.id.cedulaInputLayout,
            R.id.amountInputLayout,
            R.id.dateInputLayout,
            R.id.confirmButton,
            R.id.infoButton,
            R.id.increaseButton,
            R.id.decreaseButton,
            R.id.resetAmountButton -> true
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
                        updateSelectionIndicator()
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
            R.id.confirmButton,
            R.id.infoButton,
            R.id.increaseButton,
            R.id.decreaseButton,
            R.id.resetAmountButton -> true
            else -> false
        }
    }

    private fun selectComponent(view: View) {
        selectedComponent = view
        updateSelectionIndicator()
        animateSelection()
    }

    private fun updateSelectionIndicator() {
        selectedComponent?.let { view ->
            val location = IntArray(2)
            view.getLocationInWindow(location)

            selectionIndicator.apply {
                layoutParams = (layoutParams as CoordinatorLayout.LayoutParams).apply {
                    width = (view.width * view.scaleX).toInt() + 20
                    height = (view.height * view.scaleY).toInt() + 20
                }
                x = view.x + view.translationX - 10
                y = view.y + view.translationY - 10
                visibility = View.VISIBLE
            }
        }
    }

    private fun animateSelection() {
        val scaleX = ObjectAnimator.ofFloat(selectionIndicator, "scaleX", 0.8f, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(selectionIndicator, "scaleY", 0.8f, 1.0f)
        val alpha = ObjectAnimator.ofFloat(selectionIndicator, "alpha", 0f, 1f)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 200
            start()
        }
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

        scaleSlider.value = currentScale

        // Show/hide text controls based on component type
        val isTextComponent = component is TextView || component is TextInputEditText ||
                (component is TextInputLayout && component.editText != null)

        textSizeControl.visibility = if (isTextComponent) View.VISIBLE else View.GONE
        letterSpacingControl.visibility = if (isTextComponent) View.VISIBLE else View.GONE

        if (isTextComponent) {
            val textView = when (component) {
                is TextView -> component
                is TextInputEditText -> component
                is TextInputLayout -> component.editText
                else -> null
            } as? TextView

            textView?.let {
                val currentTextSize = preferencesManager.getFloat(
                    getServiceComponentKey(COMPONENT_TEXT_SIZE_KEY, componentKey), it.textSize
                )
                val currentLetterSpacing = preferencesManager.getFloat(
                    getServiceComponentKey(COMPONENT_LETTER_SPACING_KEY, componentKey), it.letterSpacing
                )

                textSizeSlider.value = currentTextSize
                letterSpacingSlider.value = currentLetterSpacing
            }
        }

        // Animate panel appearance
        editControlsPanel.apply {
            visibility = View.VISIBLE
            alpha = 0f
            translationY = 100f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .start()
        }
    }

    private fun hideEditControls() {
        editControlsPanel.animate()
            .alpha(0f)
            .translationY(100f)
            .setDuration(200)
            .withEndAction {
                editControlsPanel.visibility = View.GONE
            }
            .start()

        selectionIndicator.visibility = View.GONE
        selectedComponent = null
    }

    private fun getComponentDisplayName(component: View): String {
        return when (component.id) {
            R.id.serviceImage -> "Imagen del Servicio"
            R.id.serviceTitle -> "Título del Servicio"
            R.id.phoneInputLayout -> "Campo Teléfono"
            R.id.cedulaInputLayout -> "Campo Cédula"
            R.id.amountInputLayout -> "Campo Monto"
            R.id.dateInputLayout -> "Campo Fecha"
            R.id.confirmButton -> "Botón Confirmar"
            R.id.infoButton -> "Botón Info"
            R.id.increaseButton -> "Botón Aumentar"
            R.id.decreaseButton -> "Botón Disminuir"
            R.id.resetAmountButton -> "Botón Reset"
            else -> "Componente"
        }
    }

    private fun saveComponentConfiguration(component: View?) {
        component?.let {
            val componentKey = getComponentKey(it)

            preferencesManager.putFloat(
                getServiceComponentKey(COMPONENT_SCALE_KEY, componentKey), it.scaleX
            )
            preferencesManager.putFloat(
                getServiceComponentKey(COMPONENT_POSITION_X_KEY, componentKey), it.translationX
            )
            preferencesManager.putFloat(
                getServiceComponentKey(COMPONENT_POSITION_Y_KEY, componentKey), it.translationY
            )

            when (it) {
                is TextView -> {
                    preferencesManager.putFloat(
                        getServiceComponentKey(COMPONENT_TEXT_SIZE_KEY, componentKey), it.textSize
                    )
                    preferencesManager.putFloat(
                        getServiceComponentKey(COMPONENT_LETTER_SPACING_KEY, componentKey), it.letterSpacing
                    )
                }
                is TextInputLayout -> {
                    it.editText?.let { editText ->
                        preferencesManager.putFloat(
                            getServiceComponentKey(COMPONENT_TEXT_SIZE_KEY, componentKey), editText.textSize
                        )
                        preferencesManager.putFloat(
                            getServiceComponentKey(COMPONENT_LETTER_SPACING_KEY, componentKey), editText.letterSpacing
                        )
                    }
                }

                else -> {}
            }
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

            when (it) {
                is TextView -> {
                    val textSize = preferencesManager.getFloat(
                        getServiceComponentKey(COMPONENT_TEXT_SIZE_KEY, componentKey), it.textSize
                    )
                    val letterSpacing = preferencesManager.getFloat(
                        getServiceComponentKey(COMPONENT_LETTER_SPACING_KEY, componentKey), it.letterSpacing
                    )

                    it.textSize = textSize
                    it.letterSpacing = letterSpacing
                }
                is TextInputLayout -> {
                    it.editText?.let { editText ->
                        val textSize = preferencesManager.getFloat(
                            getServiceComponentKey(COMPONENT_TEXT_SIZE_KEY, componentKey), editText.textSize
                        )
                        val letterSpacing = preferencesManager.getFloat(
                            getServiceComponentKey(COMPONENT_LETTER_SPACING_KEY, componentKey), editText.letterSpacing
                        )

                        editText.textSize = textSize
                        editText.letterSpacing = letterSpacing
                    }
                }

                else -> {}
            }
        }
    }

    private fun getComponentKey(component: View): String {
        return when (component.id) {
            R.id.serviceImage -> "serviceImage"
            R.id.serviceTitle -> "serviceTitle"
            R.id.phoneInputLayout -> "phoneInput"
            R.id.cedulaInputLayout -> "cedulaInput"
            R.id.amountInputLayout -> "amountInput"
            R.id.dateInputLayout -> "dateInput"
            R.id.confirmButton -> "confirmButton"
            R.id.infoButton -> "infoButton"
            R.id.increaseButton -> "increaseButton"
            R.id.decreaseButton -> "decreaseButton"
            R.id.resetAmountButton -> "resetAmountButton"
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
            exitEditMode(true)
        }
    }

    private fun enterEditMode() {
        // Save original positions
        saveOriginalPositions()

        // Show/hide UI elements
        editButton.visibility = View.GONE
        editButtonsContainer.visibility = View.VISIBLE
        editModeOverlay.visibility = View.VISIBLE

        // Animate overlay appearance
        editModeOverlay.alpha = 0f
        editModeOverlay.animate()
            .alpha(1f)
            .setDuration(300)
            .start()

        showSnackbar("Modo de edición activado. Toca los componentes para editarlos.")
    }

    private fun exitEditMode(saveChanges: Boolean) {
        if (!saveChanges) {
            // Restore original positions
            restoreOriginalPositions()
        }

        isEditMode = false
        editButton.visibility = View.VISIBLE
        editButtonsContainer.visibility = View.GONE

        // Animate overlay disappearance
        editModeOverlay.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                editModeOverlay.visibility = View.GONE
            }
            .start()

        hideEditControls()
        selectionIndicator.visibility = View.GONE
        selectedComponent = null

        showSnackbar(if (saveChanges) "Cambios guardados" else "Cambios descartados")
    }

    private fun saveOriginalPositions() {
        originalPositions.clear()
        getAllEditableViews().forEach { view ->
            originalPositions[view] = Pair(view.translationX, view.translationY)
        }
    }

    private fun restoreOriginalPositions() {
        originalPositions.forEach { (view, position) ->
            view.translationX = position.first
            view.translationY = position.second
        }
    }

    private fun getAllEditableViews(): List<View> {
        val views = mutableListOf<View>()
        val ids = listOf(
            R.id.serviceImage, R.id.serviceTitle,
            R.id.phoneInputLayout, R.id.cedulaInputLayout,
            R.id.amountInputLayout, R.id.dateInputLayout,
            R.id.confirmButton, R.id.infoButton,
            R.id.increaseButton, R.id.decreaseButton,
            R.id.resetAmountButton
        )

        ids.forEach { id ->
            activity.findViewById<View>(id)?.let { views.add(it) }
        }

        return views
    }

    private fun saveAllChanges() {
        getAllEditableViews().forEach { view ->
            saveComponentConfiguration(view)
        }
        exitEditMode(true)
    }

    private fun showResetConfirmation() {
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
        getAllEditableViews().forEach { view ->
            val componentKey = getComponentKey(view)

            // Clear all saved preferences for this component
            preferencesManager.putFloat(getServiceComponentKey(COMPONENT_SCALE_KEY, componentKey), 1.0f)
            preferencesManager.putFloat(getServiceComponentKey(COMPONENT_POSITION_X_KEY, componentKey), 0f)
            preferencesManager.putFloat(getServiceComponentKey(COMPONENT_POSITION_Y_KEY, componentKey), 0f)

            // Reset view properties
            view.scaleX = 1.0f
            view.scaleY = 1.0f
            view.translationX = 0f
            view.translationY = 0f

            when (view) {
                is TextView -> {
                    preferencesManager.putFloat(getServiceComponentKey(COMPONENT_TEXT_SIZE_KEY, componentKey), 16.0f)
                    preferencesManager.putFloat(getServiceComponentKey(COMPONENT_LETTER_SPACING_KEY, componentKey), 0.0f)
                    view.textSize = 16.0f
                    view.letterSpacing = 0.0f
                }
                is TextInputLayout -> {
                    view.editText?.let { editText ->
                        preferencesManager.putFloat(getServiceComponentKey(COMPONENT_TEXT_SIZE_KEY, componentKey), 16.0f)
                        preferencesManager.putFloat(getServiceComponentKey(COMPONENT_LETTER_SPACING_KEY, componentKey), 0.0f)
                        editText.textSize = 16.0f
                        editText.letterSpacing = 0.0f
                    }
                }
            }
        }

        showSnackbar("Configuración reseteada a valores por defecto")
    }

    private fun exportConfiguration() {
        val fileName = "config_servicio_${currentServiceType}_${System.currentTimeMillis()}.json"
        createDocumentLauncher.launch(fileName)
    }

    private fun importConfiguration() {
        openDocumentLauncher.launch(arrayOf("application/json"))
    }

    private fun saveConfigurationToUri(uri: Uri) {
        try {
            val config = JSONObject()
            config.put("serviceType", currentServiceType)
            config.put("version", CONFIG_VERSION)
            config.put("timestamp", System.currentTimeMillis())

            val componentsConfig = JSONObject()

            getAllEditableViews().forEach { view ->
                val componentKey = getComponentKey(view)
                val componentConfig = JSONObject()

                componentConfig.put("scale", view.scaleX)
                componentConfig.put("positionX", view.translationX)
                componentConfig.put("positionY", view.translationY)

                when (view) {
                    is TextView -> {
                        componentConfig.put("textSize", view.textSize)
                        componentConfig.put("letterSpacing", view.letterSpacing)
                    }
                    is TextInputLayout -> {
                        view.editText?.let { editText ->
                            componentConfig.put("textSize", editText.textSize)
                            componentConfig.put("letterSpacing", editText.letterSpacing)
                        }
                    }
                }

                componentsConfig.put(componentKey, componentConfig)
            }

            config.put("components", componentsConfig)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(config.toString(2).toByteArray())
            }

            showSnackbar("Configuración exportada exitosamente")
        } catch (e: Exception) {
            showSnackbar("Error al exportar: ${e.message}")
        }
    }

    private fun loadConfigurationFromUri(uri: Uri) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val content = inputStream.bufferedReader().use { it.readText() }
                val config = JSONObject(content)

                // Verify version compatibility
                val version = config.optString("version", "1.0")
                if (version != CONFIG_VERSION) {
                    showSnackbar("Versión de configuración incompatible")
                    return
                }

                val serviceType = config.optInt("serviceType", currentServiceType)
                if (serviceType != currentServiceType) {
                    showSnackbar("Esta configuración es para otro tipo de servicio")
                    return
                }

                val components = config.optJSONObject("components")
                components?.let { componentsConfig ->
                    getAllEditableViews().forEach { view ->
                        val componentKey = getComponentKey(view)
                        val componentConfig = componentsConfig.optJSONObject(componentKey)

                        componentConfig?.let {
                            view.scaleX = it.optDouble("scale", 1.0).toFloat()
                            view.scaleY = view.scaleX
                            view.translationX = it.optDouble("positionX", 0.0).toFloat()
                            view.translationY = it.optDouble("positionY", 0.0).toFloat()

                            when (view) {
                                is TextView -> {
                                    view.textSize = it.optDouble("textSize", 16.0).toFloat()
                                    view.letterSpacing = it.optDouble("letterSpacing", 0.0).toFloat()
                                }
                                is TextInputLayout -> {
                                    view.editText?.let { editText ->
                                        editText.textSize = it.optDouble("textSize", 16.0).toFloat()
                                        editText.letterSpacing = it.optDouble("letterSpacing", 0.0).toFloat()
                                    }
                                }
                            }

                            saveComponentConfiguration(view)
                        }
                    }
                }

                showSnackbar("Configuración importada exitosamente")
            }
        } catch (e: Exception) {
            showSnackbar("Error al importar: ${e.message}")
        }
    }

    private fun loadCurrentConfiguration() {
        getAllEditableViews().forEach { view ->
            loadComponentConfiguration(view)
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

    private fun showSnackbar(message: String) {
        Snackbar.make(rootLayout, message, Snackbar.LENGTH_SHORT).show()
    }
}