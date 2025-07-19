package com.example.geniotecni.tigo.managers

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
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
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.ViewTreeObserver
import androidx.core.content.ContextCompat
import com.example.geniotecni.tigo.managers.PreferencesManager
import com.example.geniotecni.tigo.utils.AppLogger

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
        private const val CONFIG_VERSION = "3.2"
        private const val TOUCH_SLOP = 10 // Minimum movement to consider drag
    }

    private var isEditMode = false
    private var selectedComponent: View? = null
    private var currentServiceType = 0
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var isDragging = false
    private var originalPositions = mutableMapOf<View, PositionInfo>()
    private val touchListeners = mutableMapOf<View, View.OnTouchListener>()

    // Track constraint relationships
    private val constraintRelationships = mutableMapOf<View, ConstraintInfo>()
    
    // Track original background drawables for highlighting
    private val originalBackgrounds = mutableMapOf<View, Drawable?>()

    // Edit mode UI components
    private lateinit var editButton: MaterialButton
    private lateinit var editButtonsContainer: View
    private lateinit var cancelButton: MaterialButton
    private lateinit var saveButton: MaterialButton
    private lateinit var resetButton: MaterialButton
    private lateinit var exportButton: MaterialButton
    private lateinit var importButton: MaterialButton
    private lateinit var selectionOverlay: SelectionOverlay
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

    data class PositionInfo(
        val translationX: Float,
        val translationY: Float,
        val scaleX: Float,
        val scaleY: Float,
        val layoutParams: ViewGroup.LayoutParams
    )

    data class ConstraintInfo(
        val leftToLeft: Int?,
        val leftToRight: Int?,
        val rightToLeft: Int?,
        val rightToRight: Int?,
        val topToTop: Int?,
        val topToBottom: Int?,
        val bottomToTop: Int?,
        val bottomToBottom: Int?
    )

    fun initializeEditMode() {
        try {
            findViews()
            setupListeners()
            createSelectionOverlay()
            loadCurrentConfiguration()
            analyzeConstraints()
        } catch (e: Exception) {
            e.printStackTrace()
            showSnackbar("Error al inicializar modo edición: ${e.message}")
        }
    }

    private fun findViews() {
        editButton = activity.findViewById(R.id.editButton) ?: throw RuntimeException("editButton not found")
        editButtonsContainer = activity.findViewById(R.id.editButtonsContainer) ?: throw RuntimeException("editButtonsContainer not found")
        cancelButton = activity.findViewById(R.id.cancelButton) ?: throw RuntimeException("cancelButton not found")
        saveButton = activity.findViewById(R.id.saveButton) ?: throw RuntimeException("saveButton not found")
        resetButton = activity.findViewById(R.id.resetButton) ?: throw RuntimeException("resetButton not found")
        exportButton = activity.findViewById(R.id.exportButton) ?: throw RuntimeException("exportButton not found")
        importButton = activity.findViewById(R.id.importButton) ?: throw RuntimeException("importButton not found")
        editControlsPanel = activity.findViewById(R.id.editControlsPanel) ?: throw RuntimeException("editControlsPanel not found")

        // Edit controls
        editControlsTitle = activity.findViewById(R.id.editControlsTitle) ?: throw RuntimeException("editControlsTitle not found")
        scaleSlider = activity.findViewById(R.id.scaleSlider) ?: throw RuntimeException("scaleSlider not found")
        textSizeSlider = activity.findViewById(R.id.textSizeSlider) ?: throw RuntimeException("textSizeSlider not found")
        letterSpacingSlider = activity.findViewById(R.id.letterSpacingSlider) ?: throw RuntimeException("letterSpacingSlider not found")
        textSizeControl = activity.findViewById(R.id.textSizeControl) ?: throw RuntimeException("textSizeControl not found")
        letterSpacingControl = activity.findViewById(R.id.letterSpacingControl) ?: throw RuntimeException("letterSpacingControl not found")
        applyChangesButton = activity.findViewById(R.id.applyChangesButton) ?: throw RuntimeException("applyChangesButton not found")
        cancelChangesButton = activity.findViewById(R.id.cancelChangesButton) ?: throw RuntimeException("cancelChangesButton not found")
    }

    private fun createSelectionOverlay() {
        selectionOverlay = SelectionOverlay(context)
        rootLayout.addView(selectionOverlay, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        selectionOverlay.visibility = View.GONE
        selectionOverlay.bringToFront()
    }

    private fun analyzeConstraints() {
        // Analyze constraint relationships for maintaining layout integrity
        val parent = activity.findViewById<ViewGroup>(R.id.serviceContainer)
        if (parent is ConstraintLayout) {
            // Store constraint relationships for each editable view
            getAllEditableViews().forEach { view ->
                val params = view.layoutParams as? ConstraintLayout.LayoutParams
                params?.let {
                    constraintRelationships[view] = ConstraintInfo(
                        leftToLeft = if (it.leftToLeft != ConstraintLayout.LayoutParams.UNSET) it.leftToLeft else null,
                        leftToRight = if (it.leftToRight != ConstraintLayout.LayoutParams.UNSET) it.leftToRight else null,
                        rightToLeft = if (it.rightToLeft != ConstraintLayout.LayoutParams.UNSET) it.rightToLeft else null,
                        rightToRight = if (it.rightToRight != ConstraintLayout.LayoutParams.UNSET) it.rightToRight else null,
                        topToTop = if (it.topToTop != ConstraintLayout.LayoutParams.UNSET) it.topToTop else null,
                        topToBottom = if (it.topToBottom != ConstraintLayout.LayoutParams.UNSET) it.topToBottom else null,
                        bottomToTop = if (it.bottomToTop != ConstraintLayout.LayoutParams.UNSET) it.bottomToTop else null,
                        bottomToBottom = if (it.bottomToBottom != ConstraintLayout.LayoutParams.UNSET) it.bottomToBottom else null
                    )
                }
            }
        }
    }

    private fun setupListeners() {
        editButton.setOnClickListener { toggleEditMode() }
        cancelButton.setOnClickListener { exitEditMode(false) }
        saveButton.setOnClickListener { saveAllChanges() }
        resetButton.setOnClickListener { showResetConfirmation() }
        exportButton.setOnClickListener { exportConfiguration() }
        importButton.setOnClickListener { importConfiguration() }

        // Slider listeners
        scaleSlider.addOnChangeListener { _, value, _ ->
            selectedComponent?.let { component ->
                component.scaleX = value
                component.scaleY = value
                selectionOverlay.updateSelection(component)
            }
        }

        textSizeSlider.addOnChangeListener { _, value, _ ->
            selectedComponent?.let { component ->
                when (component) {
                    is TextView -> {
                        Log.d("EditModeManager", "Aplicando textSize al TextView: ${value}sp")
                        component.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, value)
                    }
                    is TextInputLayout -> {
                        component.editText?.let { editText ->
                            Log.d("EditModeManager", "Aplicando textSize al EditText: ${value}sp")
                            editText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, value)
                        }
                    }
                }
            }
        }

        letterSpacingSlider.addOnChangeListener { _, value, _ ->
            selectedComponent?.let { component ->
                when (component) {
                    is TextView -> component.letterSpacing = value
                    is TextInputLayout -> component.editText?.letterSpacing = value
                }
            }
        }
        
        // Add touch listeners for transparency effect while dragging
        addSliderTouchListeners()

        applyChangesButton.setOnClickListener {
            selectedComponent?.let {
                saveComponentConfiguration(it)
                hideEditControls()
                showSnackbar("Cambios aplicados")
            }
        }

        cancelChangesButton.setOnClickListener {
            selectedComponent?.let {
                loadComponentConfiguration(it)
                hideEditControls()
            }
        }
    }
    
    private fun addSliderTouchListeners() {
        // Track original alpha values
        val originalAlphaMap = mutableMapOf<View, Float>()
        
        // Common touch listener for all sliders
        val sliderTouchListener = object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                selectedComponent?.let { component ->
                    // Store original alpha and make component transparent
                    originalAlphaMap[component] = component.alpha
                    component.alpha = 0.5f // Set to 50% transparency
                }
            }
            
            override fun onStopTrackingTouch(slider: Slider) {
                selectedComponent?.let { component ->
                    // Restore original alpha
                    val originalAlpha = originalAlphaMap[component] ?: 1.0f
                    component.alpha = originalAlpha
                    originalAlphaMap.remove(component)
                }
            }
        }
        
        // Apply the same touch listener to all sliders
        scaleSlider.addOnSliderTouchListener(sliderTouchListener)
        textSizeSlider.addOnSliderTouchListener(sliderTouchListener)
        letterSpacingSlider.addOnSliderTouchListener(sliderTouchListener)
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
        // Don't show the dark overlay - allow full interaction
        selectionOverlay.visibility = View.VISIBLE

        // Disable interactive components to prevent interference
        disableInteractiveComponents()

        // Setup touch listeners for all editable components
        setupEditModeTouchListeners()
        
        // Add visual indicators to editable components
        highlightEditableComponents()

        showSnackbar("Modo de edición activado. Toca los componentes para editarlos.")
    }

    private fun exitEditMode(saveChanges: Boolean) {
        if (!saveChanges) {
            restoreOriginalPositions()
        }

        isEditMode = false
        editButton.visibility = View.VISIBLE
        editButtonsContainer.visibility = View.GONE

        hideEditControls()
        selectionOverlay.visibility = View.GONE
        selectedComponent = null

        // Re-enable interactive components
        enableInteractiveComponents()

        // Remove touch listeners and visual indicators
        removeEditModeTouchListeners()
        removeEditableHighlights()

        showSnackbar(if (saveChanges) "Cambios guardados" else "Cambios descartados")
    }

    private fun setupEditModeTouchListeners() {
        getAllEditableViews().forEach { view ->
            val touchListener = View.OnTouchListener { v, event ->
                if (isEditMode) {
                    handleComponentTouch(view, event)
                } else {
                    // Allow normal functionality when not in edit mode
                    false
                }
            }
            touchListeners[view] = touchListener
            view.setOnTouchListener(touchListener)
            view.isClickable = true
            
            // Add long press listener for edit mode selection
            view.setOnLongClickListener {
                if (isEditMode) {
                    selectComponent(view)
                    showEditControls(view)
                    true
                } else {
                    false
                }
            }
            
            // Add single tap listener for edit mode selection (for non-image components)
            view.setOnClickListener {
                if (isEditMode) {
                    selectComponent(view)
                    showEditControls(view)
                } else {
                    // Allow normal functionality when not in edit mode
                    view.performClick()
                }
            }
        }
        
        // Setup special handling for TextInputLayout components
        setupTextInputLayoutTouchListeners()
    }
    
    private fun setupTextInputLayoutTouchListeners() {
        getAllEditableViews().forEach { view ->
            if (view is TextInputLayout) {
                // Find the EditText child and set up touch listener to redirect to parent
                view.editText?.let { editText ->
                    val childTouchListener = View.OnTouchListener { _, event ->
                        if (isEditMode) {
                            // Pass touch event to parent TextInputLayout for edit mode handling
                            view.onTouchEvent(event)
                        } else {
                            // Allow normal EditText functionality when not in edit mode
                            false
                        }
                    }
                    editText.setOnTouchListener(childTouchListener)
                    
                    // Also disable focus in edit mode to prevent keyboard popup
                    if (isEditMode) {
                        editText.isFocusable = false
                        editText.isFocusableInTouchMode = false
                    }
                }
            }
        }
    }

    private fun removeEditModeTouchListeners() {
        touchListeners.forEach { (view, _) ->
            view.setOnTouchListener(null)
            view.setOnLongClickListener(null)
            view.setOnClickListener(null)
            view.isClickable = true
            
            // Restore EditText functionality for TextInputLayout components
            if (view is TextInputLayout) {
                view.editText?.let { editText ->
                    editText.setOnTouchListener(null)
                    editText.isFocusable = true
                    editText.isFocusableInTouchMode = true
                }
            }
        }
        touchListeners.clear()
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

                if (Math.abs(deltaX) > TOUCH_SLOP || Math.abs(deltaY) > TOUCH_SLOP) {
                    isDragging = true
                    if (isMovableComponent(view)) {
                        // Apply movement while maintaining constraints
                        applyConstrainedMovement(view, deltaX, deltaY)
                        dragStartX = event.rawX
                        dragStartY = event.rawY
                        selectionOverlay.updateSelection(view)
                        
                        // Show live feedback during drag
                        showSnackbar("Moviendo ${getComponentName(view)}")
                    } else {
                        showSnackbar("${getComponentName(view)} no se puede mover")
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    // Single tap - show edit controls
                    showEditControls(view)
                } else {
                    // Drag ended - save position
                    if (isMovableComponent(view)) {
                        saveComponentConfiguration(view)
                        showSnackbar("Posición guardada")
                    }
                }
                isDragging = false
                return true
            }
        }
        return false
    }

    private fun applyConstrainedMovement(view: View, deltaX: Float, deltaY: Float) {
        // For now, apply simple translation
        // In a more complex implementation, you would check constraint relationships
        view.translationX += deltaX
        view.translationY += deltaY

        // TODO: Implement constraint-aware movement
        // This would involve checking the constraint relationships and updating
        // positions relative to the constrained views
    }

    private fun isMovableComponent(view: View): Boolean {
        // Allow movement for all UI components except edit mode controls
        return when (view.id) {
            // Edit mode controls - NOT movable
            R.id.editButton,
            R.id.editButtonsContainer,
            R.id.cancelButton,
            R.id.saveButton,
            R.id.resetButton,
            R.id.exportButton,
            R.id.importButton,
            R.id.editControlsPanel,
            R.id.editControlsTitle,
            R.id.scaleSlider,
            R.id.textSizeSlider,
            R.id.letterSpacingSlider,
            R.id.textSizeControl,
            R.id.letterSpacingControl,
            R.id.applyChangesButton,
            R.id.cancelChangesButton -> false
            
            // All other components - MOVABLE
            else -> true
        }
    }

    private fun alignToStepSize(value: Float, minValue: Float, stepSize: Float): Float {
        if (stepSize == 0f) return value
        val adjustedValue = value - minValue
        val steps = Math.round(adjustedValue / stepSize)
        return minValue + (steps * stepSize)
    }

    private fun selectComponent(view: View) {
        // Remove selection highlight from previous component
        selectedComponent?.let { previousView ->
            originalBackgrounds[previousView]?.let { originalDrawable ->
                previousView.background = originalDrawable
            } ?: run {
                val highlightDrawable = ContextCompat.getDrawable(context, R.drawable.edit_mode_highlight)
                previousView.background = highlightDrawable
            }
        }
        
        selectedComponent = view
        
        // Apply selected state background
        val selectedDrawable = ContextCompat.getDrawable(context, R.drawable.edit_mode_selected)
        view.background = selectedDrawable
        
        selectionOverlay.showSelection(view)
        animateSelection()
    }

    private fun animateSelection() {
        selectedComponent?.let { view ->
            val scaleUp = ObjectAnimator.ofFloat(view, "scaleX", view.scaleX, view.scaleX * 1.05f, view.scaleX)
            val scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", view.scaleY, view.scaleY * 1.05f, view.scaleY)

            AnimatorSet().apply {
                playTogether(scaleUp, scaleUpY)
                duration = 200
                start()
            }
        }
    }

    private fun showEditControls(view: View) {
        Log.d("EditModeManager", "=== MOSTRANDO CONTROLES DE EDICIÓN ===")
        Log.d("EditModeManager", "Componente seleccionado: ${getComponentName(view)}")
        Log.d("EditModeManager", "View ID: ${view.id}")
        Log.d("EditModeManager", "ScaleX actual: ${view.scaleX}")
        Log.d("EditModeManager", "ScaleY actual: ${view.scaleY}")
        Log.d("EditModeManager", "TranslationX actual: ${view.translationX}")
        Log.d("EditModeManager", "TranslationY actual: ${view.translationY}")
        
        selectedComponent = view

        // Update control panel title
        editControlsTitle.text = "Editando: ${getComponentName(view)}"

        // Set current values with stepSize alignment and clamping
        val alignedScale = alignToStepSize(view.scaleX, 0.5f, 0.1f)
        Log.d("EditModeManager", "Scale alineado: $alignedScale")
        scaleSlider.value = alignedScale

        when (view) {
            is TextView -> {
                Log.d("EditModeManager", "Configurando controles para TextView")
                Log.d("EditModeManager", "TextSize original: ${view.textSize}")
                Log.d("EditModeManager", "LetterSpacing original: ${view.letterSpacing}")
                
                textSizeControl.visibility = View.VISIBLE
                letterSpacingControl.visibility = View.VISIBLE
                
                // Convert px to sp and clamp to slider range (12-28)
                val textSizeInSp = view.textSize / context.resources.displayMetrics.scaledDensity
                val clampedTextSize = textSizeInSp.coerceIn(12f, 28f)
                val alignedTextSize = alignToStepSize(clampedTextSize, 12f, 1f)
                Log.d("EditModeManager", "TextSize original: ${view.textSize}px = ${textSizeInSp}sp, clamped y alineado: $alignedTextSize")
                textSizeSlider.value = alignedTextSize
                
                val alignedLetterSpacing = alignToStepSize(view.letterSpacing, 0f, 0.01f)
                Log.d("EditModeManager", "LetterSpacing alineado: $alignedLetterSpacing")
                letterSpacingSlider.value = alignedLetterSpacing
            }
            is TextInputLayout -> {
                Log.d("EditModeManager", "Configurando controles para TextInputLayout")
                textSizeControl.visibility = View.VISIBLE
                letterSpacingControl.visibility = View.VISIBLE
                view.editText?.let { editText ->
                    Log.d("EditModeManager", "EditText TextSize original: ${editText.textSize}")
                    Log.d("EditModeManager", "EditText LetterSpacing original: ${editText.letterSpacing}")
                    
                    // Convert px to sp and clamp to slider range (12-28)
                    val textSizeInSp = editText.textSize / context.resources.displayMetrics.scaledDensity
                    val clampedTextSize = textSizeInSp.coerceIn(12f, 28f)
                    val alignedTextSize = alignToStepSize(clampedTextSize, 12f, 1f)
                    Log.d("EditModeManager", "EditText TextSize original: ${editText.textSize}px = ${textSizeInSp}sp, clamped y alineado: $alignedTextSize")
                    textSizeSlider.value = alignedTextSize
                    
                    val alignedLetterSpacing = alignToStepSize(editText.letterSpacing, 0f, 0.01f)
                    Log.d("EditModeManager", "EditText LetterSpacing alineado: $alignedLetterSpacing")
                    letterSpacingSlider.value = alignedLetterSpacing
                }
            }
            else -> {
                Log.d("EditModeManager", "Configurando controles para componente genérico (sin texto)")
                textSizeControl.visibility = View.GONE
                letterSpacingControl.visibility = View.GONE
            }
        }

        // Position and show control panel
        positionEditControlsPanel(view)
        editControlsPanel.visibility = View.VISIBLE
        editControlsPanel.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .start()
            
        Log.d("EditModeManager", "=== CONTROLES DE EDICIÓN MOSTRADOS ===")
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
    }

    private fun getComponentName(view: View): String {
        return when (view.id) {
            R.id.serviceImage -> "Imagen del Servicio"
            R.id.serviceTitle -> "Título del Servicio"
            R.id.phoneInputLayout -> "Campo de Teléfono"
            R.id.cedulaInputLayout -> "Campo de Cédula"
            R.id.amountInputLayout -> "Campo de Monto"
            R.id.dateInputLayout -> "Campo de Fecha"
            // Removed button mappings - buttons no longer exist
            else -> "Componente"
        }
    }

    private fun saveOriginalPositions() {
        originalPositions.clear()
        getAllEditableViews().forEach { view ->
            originalPositions[view] = PositionInfo(
                translationX = view.translationX,
                translationY = view.translationY,
                scaleX = view.scaleX,
                scaleY = view.scaleY,
                layoutParams = view.layoutParams
            )
        }
    }

    private fun restoreOriginalPositions() {
        originalPositions.forEach { (view, position) ->
            view.translationX = position.translationX
            view.translationY = position.translationY
            view.scaleX = position.scaleX
            view.scaleY = position.scaleY
        }
    }

    private fun getAllEditableViews(): List<View> {
        val views = mutableListOf<View>()
        val ids = listOf(
            R.id.serviceImage, R.id.serviceTitle,
            R.id.phoneInputLayout, R.id.cedulaInputLayout,
            R.id.amountInputLayout, R.id.dateInputLayout,
            // Removed button IDs - buttons no longer exist
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

    private fun saveComponentConfiguration(view: View) {
        val componentKey = getComponentKey(view)

        // Save position and scale
        preferencesManager.setFloat(
            getServiceComponentKey(COMPONENT_SCALE_KEY, componentKey), view.scaleX
        )
        preferencesManager.setFloat(
            getServiceComponentKey(COMPONENT_POSITION_X_KEY, componentKey), view.translationX
        )
        preferencesManager.setFloat(
            getServiceComponentKey(COMPONENT_POSITION_Y_KEY, componentKey), view.translationY
        )

        // Save text properties if applicable
        when (view) {
            is TextView -> {
                // Convert px to sp for consistent storage
                val textSizeInSp = view.textSize / context.resources.displayMetrics.scaledDensity
                Log.d("EditModeManager", "Guardando textSize: ${view.textSize}px = ${textSizeInSp}sp")
                preferencesManager.setFloat(
                    getServiceComponentKey(COMPONENT_TEXT_SIZE_KEY, componentKey), textSizeInSp
                )
                preferencesManager.setFloat(
                    getServiceComponentKey(COMPONENT_LETTER_SPACING_KEY, componentKey), view.letterSpacing
                )
            }
            is TextInputLayout -> {
                view.editText?.let { editText ->
                    // Convert px to sp for consistent storage
                    val textSizeInSp = editText.textSize / context.resources.displayMetrics.scaledDensity
                    Log.d("EditModeManager", "Guardando editText textSize: ${editText.textSize}px = ${textSizeInSp}sp")
                    preferencesManager.setFloat(
                        getServiceComponentKey(COMPONENT_TEXT_SIZE_KEY, componentKey), textSizeInSp
                    )
                    preferencesManager.setFloat(
                        getServiceComponentKey(COMPONENT_LETTER_SPACING_KEY, componentKey), editText.letterSpacing
                    )
                }
            }
        }
    }

    private fun loadComponentConfiguration(view: View) {
        val componentKey = getComponentKey(view)

        // Check if configuration exists for this service and component
        val scaleKey = getServiceComponentKey(COMPONENT_SCALE_KEY, componentKey)
        val hasExistingConfig = preferencesManager.contains(scaleKey)

        if (!hasExistingConfig) {
            // No configuration exists, reset to defaults
            resetComponentToDefault(view)
            return
        }

        // Load position and scale
        view.scaleX = preferencesManager.getFloat(scaleKey, 1.0f)
        view.scaleY = view.scaleX
        view.translationX = preferencesManager.getFloat(
            getServiceComponentKey(COMPONENT_POSITION_X_KEY, componentKey), 0f
        )
        view.translationY = preferencesManager.getFloat(
            getServiceComponentKey(COMPONENT_POSITION_Y_KEY, componentKey), 0f
        )

        // Load text properties if applicable
        when (view) {
            is TextView -> {
                val textSizeInSp = preferencesManager.getFloat(
                    getServiceComponentKey(COMPONENT_TEXT_SIZE_KEY, componentKey), 16f
                )
                val letterSpacing = preferencesManager.getFloat(
                    getServiceComponentKey(COMPONENT_LETTER_SPACING_KEY, componentKey), 0f
                )

                Log.d("EditModeManager", "Cargando textSize: ${textSizeInSp}sp")
                // Use setTextSize with TypedValue.COMPLEX_UNIT_SP to ensure correct unit
                view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, textSizeInSp)
                view.letterSpacing = letterSpacing
            }
            is TextInputLayout -> {
                view.editText?.let { editText ->
                    val textSizeInSp = preferencesManager.getFloat(
                        getServiceComponentKey(COMPONENT_TEXT_SIZE_KEY, componentKey), 16f
                    )
                    val letterSpacing = preferencesManager.getFloat(
                        getServiceComponentKey(COMPONENT_LETTER_SPACING_KEY, componentKey), 0f
                    )

                    Log.d("EditModeManager", "Cargando editText textSize: ${textSizeInSp}sp")
                    // Use setTextSize with TypedValue.COMPLEX_UNIT_SP to ensure correct unit
                    editText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, textSizeInSp)
                    editText.letterSpacing = letterSpacing
                }
            }
        }
        val startTime = System.currentTimeMillis()
        val loadTime = System.currentTimeMillis() - startTime
        AppLogger.i("EditModeManager", "Configuración cargada para ${getComponentName(view)} en ${loadTime}ms")
    }

    private fun getComponentKey(component: View): String {
        return when (component.id) {
            R.id.serviceImage -> "serviceImage"
            R.id.serviceTitle -> "serviceTitle"
            R.id.phoneInputLayout -> "phoneInput"
            R.id.cedulaInputLayout -> "cedulaInput"
            R.id.amountInputLayout -> "amountInput"
            R.id.dateInputLayout -> "dateInput"
            // Removed button key mappings - buttons no longer exist
            else -> "unknown_${component.id}"
        }
    }

    private fun getServiceComponentKey(baseKey: String, componentKey: String): String {
        return "${baseKey}_service_${currentServiceType}_${componentKey}"
    }

    private fun showResetConfirmation() {
        AlertDialog.Builder(context)
            .setTitle("Resetear Configuración")
            .setMessage("¿Estás seguro de que quieres resetear todos los componentes a sus valores por defecto?")
            .setPositiveButton("Sí") { _, _ ->
                resetToDefaults()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun resetToDefaults() {
        getAllEditableViews().forEach { view ->
            resetComponentToDefault(view)
            saveComponentConfiguration(view)
        }

        showSnackbar("Configuración reseteada a valores por defecto")
    }

    private fun resetComponentToDefault(view: View) {
        // Reset to match the default values used in alignToStepSize and showEditControls
        view.scaleX = 1.0f
        view.scaleY = 1.0f
        view.translationX = 0f
        view.translationY = 0f

        when (view) {
            is TextView -> {
                // Use default values that match the slider ranges with proper SP units
                view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16f) // Within range 12-28
                view.letterSpacing = 0f // Within range 0-0.1
            }
            is TextInputLayout -> {
                view.editText?.let { editText ->
                    // Use default values that match the slider ranges with proper SP units
                    editText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16f) // Within range 12-28
                    editText.letterSpacing = 0f // Within range 0-0.1
                }
            }
        }
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
                outputStream.write(config.toString(4).toByteArray())
            }

            showSnackbar("Configuración exportada exitosamente")
        } catch (e: Exception) {
            showSnackbar("Error al exportar: ${e.message}")
        }
    }

    private fun loadConfigurationFromUri(uri: Uri) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val content = inputStream.bufferedReader().readText()
                val config = JSONObject(content)

                if (config.getString("version") != CONFIG_VERSION) {
                    showSnackbar("Versión de configuración incompatible")
                    return
                }

                val serviceType = config.getInt("serviceType")
                if (serviceType != currentServiceType) {
                    showSnackbar("Esta configuración es para otro tipo de servicio")
                    return
                }

                val componentsConfig = config.getJSONObject("components")

                getAllEditableViews().forEach { view ->
                    val componentKey = getComponentKey(view)
                    componentsConfig.optJSONObject(componentKey)?.let {
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
        return false // Allow normal touch events even in edit mode
    }

    fun isInEditMode(): Boolean = isEditMode

    private fun highlightEditableComponents() {
        getAllEditableViews().forEach { view ->
            // Store original background for restoration
            originalBackgrounds[view] = view.background
            
            // Add subtle highlight to indicate it's editable
            val highlightDrawable = ContextCompat.getDrawable(context, R.drawable.edit_mode_highlight)
            view.background = highlightDrawable
            
            // Add a subtle scale animation to draw attention
            view.animate()
                .scaleX(1.02f)
                .scaleY(1.02f)
                .setDuration(200)
                .withEndAction {
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start()
                }
                .start()
        }
    }
    
    private fun removeEditableHighlights() {
        getAllEditableViews().forEach { view ->
            // Restore original backgrounds
            originalBackgrounds[view]?.let { originalDrawable ->
                view.background = originalDrawable
            } ?: run {
                view.background = null
            }
        }
        originalBackgrounds.clear()
    }

    private fun disableInteractiveComponents() {
        Log.d("EditModeManager", "Deshabilitando componentes interactivos")
        
        // Disable buttons
        // Button disable code removed - buttons no longer exist
        
        // Disable text inputs (make them non-focusable but keep touchable for editing)
        activity.findViewById<TextInputEditText>(R.id.phoneInput)?.let { input ->
            input.isFocusable = false
            input.isFocusableInTouchMode = false
        }
        activity.findViewById<TextInputEditText>(R.id.cedulaInput)?.let { input ->
            input.isFocusable = false
            input.isFocusableInTouchMode = false
        }
        activity.findViewById<TextInputEditText>(R.id.amountInput)?.let { input ->
            input.isFocusable = false
            input.isFocusableInTouchMode = false
        }
        activity.findViewById<TextInputEditText>(R.id.dateInput)?.let { input ->
            input.isFocusable = false
            input.isFocusableInTouchMode = false
        }
    }
    
    private fun enableInteractiveComponents() {
        Log.d("EditModeManager", "Habilitando componentes interactivos")
        
        // Re-enable buttons
        // Button enable code removed - buttons no longer exist
        
        // Re-enable text inputs
        activity.findViewById<TextInputEditText>(R.id.phoneInput)?.let { input ->
            input.isFocusable = true
            input.isFocusableInTouchMode = true
        }
        activity.findViewById<TextInputEditText>(R.id.cedulaInput)?.let { input ->
            input.isFocusable = true
            input.isFocusableInTouchMode = true
        }
        activity.findViewById<TextInputEditText>(R.id.amountInput)?.let { input ->
            input.isFocusable = true
            input.isFocusableInTouchMode = true
        }
        activity.findViewById<TextInputEditText>(R.id.dateInput)?.let { input ->
            input.isFocusable = true
            input.isFocusableInTouchMode = true
        }
    }
    
    private fun positionEditControlsPanel(targetView: View) {
        Log.d("EditModeManager", "Posicionando panel de edición para ${getComponentName(targetView)}")
        
        // Wait for layout to be complete
        editControlsPanel.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                editControlsPanel.viewTreeObserver.removeOnGlobalLayoutListener(this)
                
                val targetLocation = IntArray(2)
                targetView.getLocationInWindow(targetLocation)
                
                val panelLocation = IntArray(2)
                editControlsPanel.getLocationInWindow(panelLocation)
                
                val rootLocation = IntArray(2)
                rootLayout.getLocationInWindow(rootLocation)
                
                // Calculate desired position (below the target view)
                val targetBottom = targetLocation[1] + targetView.height
                val availableSpaceBelow = rootLayout.height + rootLocation[1] - targetBottom
                val panelHeight = editControlsPanel.height
                
                Log.d("EditModeManager", "Target bottom: $targetBottom, Available space below: $availableSpaceBelow, Panel height: $panelHeight")
                
                val layoutParams = editControlsPanel.layoutParams as CoordinatorLayout.LayoutParams
                
                if (availableSpaceBelow >= panelHeight + 50) {
                    // Position below the target view
                    Log.d("EditModeManager", "Posicionando panel debajo del componente")
                    layoutParams.topMargin = targetBottom - rootLocation[1] + 16
                    layoutParams.gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
                } else {
                    // Not enough space below, position above or use scroll
                    val availableSpaceAbove = targetLocation[1] - rootLocation[1]
                    
                    if (availableSpaceAbove >= panelHeight + 50) {
                        // Position above the target view
                        Log.d("EditModeManager", "Posicionando panel arriba del componente")
                        layoutParams.topMargin = (targetLocation[1] - rootLocation[1] - panelHeight - 16).coerceAtLeast(0)
                        layoutParams.gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
                    } else {
                        // Default to bottom with scroll capability
                        Log.d("EditModeManager", "Posicionando panel en la parte inferior con scroll")
                        layoutParams.gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
                        layoutParams.bottomMargin = 16
                        
                        // Scroll to make the target view visible
                        scrollToView(targetView)
                    }
                }
                
                editControlsPanel.layoutParams = layoutParams
                editControlsPanel.requestLayout()
            }
        })
    }
    
    private fun scrollToView(targetView: View) {
        Log.d("EditModeManager", "Haciendo scroll para mostrar ${getComponentName(targetView)}")
        
        // Find the ScrollView in the layout
        val scrollView = rootLayout.findViewById<androidx.core.widget.NestedScrollView>(R.id.scrollView)
        
        scrollView?.let { scroll ->
            scroll.post {
                val targetLocation = IntArray(2)
                targetView.getLocationInWindow(targetLocation)
                
                val scrollLocation = IntArray(2)
                scroll.getLocationInWindow(scrollLocation)
                
                // Calculate scroll position to center the target view
                val scrollY = targetLocation[1] - scrollLocation[1] - (scroll.height / 2) + (targetView.height / 2)
                
                Log.d("EditModeManager", "Scrolling to position: $scrollY")
                scroll.smoothScrollTo(0, scrollY.coerceAtLeast(0))
            }
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(rootLayout, message, Snackbar.LENGTH_SHORT).show()
    }

    // Custom selection overlay view
    inner class SelectionOverlay(context: Context) : View(context) {
        private val paint = Paint().apply {
            color = Color.parseColor("#2196F3")
            style = Paint.Style.STROKE
            strokeWidth = 4f
            pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
        }

        private val handlePaint = Paint().apply {
            color = Color.parseColor("#2196F3")
            style = Paint.Style.FILL
        }

        private var selectedView: View? = null
        private val selectionRect = RectF()
        private val handleSize = 20f

        fun showSelection(view: View) {
            selectedView = view
            visibility = View.VISIBLE
            updateSelection(view)
        }

        fun updateSelection(view: View) {
            val location = IntArray(2)
            view.getLocationInWindow(location)
            val myLocation = IntArray(2)
            getLocationInWindow(myLocation)

            selectionRect.set(
                location[0] - myLocation[0] - 10f,
                location[1] - myLocation[1] - 10f,
                location[0] - myLocation[0] + view.width * view.scaleX + 10f,
                location[1] - myLocation[1] + view.height * view.scaleY + 10f
            )

            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            if (selectedView == null) return

            // Draw selection rectangle
            canvas.drawRoundRect(selectionRect, 8f, 8f, paint)

            // Draw resize handles
            // Top-left
            canvas.drawCircle(selectionRect.left, selectionRect.top, handleSize, handlePaint)
            // Top-right
            canvas.drawCircle(selectionRect.right, selectionRect.top, handleSize, handlePaint)
            // Bottom-left
            canvas.drawCircle(selectionRect.left, selectionRect.bottom, handleSize, handlePaint)
            // Bottom-right
            canvas.drawCircle(selectionRect.right, selectionRect.bottom, handleSize, handlePaint)
        }
    }
}