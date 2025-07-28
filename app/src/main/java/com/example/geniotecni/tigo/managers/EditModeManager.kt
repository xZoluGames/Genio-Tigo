package com.example.geniotecni.tigo.managers

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.example.geniotecni.tigo.R
import com.example.geniotecni.tigo.utils.AppLogger
import com.example.geniotecni.tigo.utils.BaseManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONObject

class EditModeManager(
    context: Context,
    private val activity: AppCompatActivity,
    private val rootLayout: CoordinatorLayout,
    private val preferencesManager: PreferencesManager
) : BaseManager(context, "EditModeManager") {
    companion object {
        private const val TAG = "EditModeManager"
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
    private val originalClickListeners = mutableMapOf<View, View.OnClickListener?>()

    // Track constraint relationships
    private val constraintRelationships = mutableMapOf<View, ConstraintInfo>()
    
    // Track original background drawables for highlighting
    private val originalBackgrounds = mutableMapOf<View, Drawable?>()
    
    // Edit mode state
    private var isEditModeEnabled = false

    // Edit mode UI components - nullable since they might not exist in all layouts
    private var editButton: FloatingActionButton? = null
    private var editButtonsContainer: View? = null
    private var saveButton: MaterialButton? = null
    private var resetButton: MaterialButton? = null
    private var exportButton: MaterialButton? = null
    private var importButton: MaterialButton? = null
    private var selectionOverlay: SelectionOverlay? = null
    private var editControlsPanel: MaterialCardView? = null

    // Edit controls
    private var editControlsTitle: TextView? = null
    private var scaleSlider: Slider? = null
    private var textSizeSlider: Slider? = null
    private var letterSpacingSlider: Slider? = null
    private var textSizeControl: LinearLayout? = null
    private var letterSpacingControl: LinearLayout? = null
    private var applyChangesButton: MaterialButton? = null
    private var cancelChangesButton: MaterialButton? = null
    private var groupEditCheckbox: CheckBox? = null

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
            // Note: loadCurrentConfiguration() is called in setCurrentServiceType() 
            // so we don't call it here to avoid loading default values
            analyzeConstraints()
        } catch (e: Exception) {
            e.printStackTrace()
            showSnackbar("Error al inicializar modo edición: ${e.message}")
        }
    }

    private fun findViews() {
        Log.d(TAG, "=== INICIANDO findViews ===")
        // Try to find edit mode UI elements, but don't fail if they don't exist
        try {
            editButton = activity.findViewById(R.id.editButton)
            Log.d(TAG, "EditButton encontrado: ${editButton != null}")
            if (editButton != null) {
                Log.d(TAG, "EditButton tipo: ${editButton!!::class.java.simpleName}")
                Log.d(TAG, "EditButton visibility: ${editButton!!.visibility}")
                Log.d(TAG, "EditButton isClickable: ${editButton!!.isClickable}")
                Log.d(TAG, "EditButton isEnabled: ${editButton!!.isEnabled}")
            } else {
                Log.e(TAG, "ERROR CRÍTICO: EditButton es null - verificar R.id.editButton en layout")
            }
            editButtonsContainer = activity.findViewById(R.id.editButtonsContainer)
            saveButton = activity.findViewById(R.id.saveButton)
            resetButton = activity.findViewById(R.id.resetButton)
            exportButton = activity.findViewById(R.id.exportButton)
            importButton = activity.findViewById(R.id.importButton)
            editControlsPanel = activity.findViewById(R.id.editControlsPanel)

            // Edit controls
            editControlsTitle = activity.findViewById(R.id.editControlsTitle)
            scaleSlider = activity.findViewById(R.id.scaleSlider)
            textSizeSlider = activity.findViewById(R.id.textSizeSlider)
            letterSpacingSlider = activity.findViewById(R.id.letterSpacingSlider)
            textSizeControl = activity.findViewById(R.id.textSizeControl)
            letterSpacingControl = activity.findViewById(R.id.letterSpacingControl)
            applyChangesButton = activity.findViewById(R.id.applyChangesButton)
            cancelChangesButton = activity.findViewById(R.id.cancelChangesButton)
            groupEditCheckbox = activity.findViewById(R.id.groupEditCheckbox)
            
            Log.d(TAG, "=== RESUMEN DE VISTAS ENCONTRADAS ===")
            Log.d(TAG, "editButton: ${editButton != null}")
            Log.d(TAG, "editButtonsContainer: ${editButtonsContainer != null}")
            Log.d(TAG, "editControlsPanel: ${editControlsPanel != null}")
            Log.d(TAG, "saveButton: ${saveButton != null}")
            Log.d(TAG, "=== FIN findViews ===")
        } catch (e: Exception) {
            Log.e(TAG, "ERROR CRÍTICO en findViews", e)
            Log.w(TAG, "Algunos elementos de EditMode no encontrados - modo edit deshabilitado")
        }
    }

    private fun createSelectionOverlay() {
        try {
            selectionOverlay = SelectionOverlay(context)
            selectionOverlay?.let { overlay ->
                rootLayout.addView(overlay, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                overlay.visibility = View.GONE
                overlay.bringToFront()
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo crear overlay de selección: ${e.message}")
        }
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
        Log.d(TAG, "=== CONFIGURANDO LISTENERS ===")

        if (editButton == null) {
            Log.e(TAG, "ERROR CRÍTICO: No se puede configurar listener - editButton es null")
            return
        }

        // Remover cualquier listener previo y agregar el nuevo
        editButton?.setOnClickListener(null)
        editButton?.setOnClickListener {
            Log.d(TAG, "=== EDITBUTTON PRESIONADO ===")
            Log.d(TAG, "Timestamp: ${System.currentTimeMillis()}")
            Log.d(TAG, "Thread: ${Thread.currentThread().name}")
            Log.d(TAG, "EditButton state - enabled: ${editButton?.isEnabled}, clickable: ${editButton?.isClickable}")
            Log.d(TAG, "Modo actual antes de toggle: $isEditMode")
            try {
                toggleEditMode()
                Log.d(TAG, "toggleEditMode ejecutado exitosamente")
            } catch (e: Exception) {
                Log.e(TAG, "ERROR CRÍTICO en toggleEditMode", e)
                showSnackbar("Error: ${e.message}")
            }
        }

        Log.d(TAG, "Listener del editButton configurado correctamente")
        saveButton?.setOnClickListener { saveAllChanges() }
        resetButton?.setOnClickListener { showResetConfirmation() }
        exportButton?.setOnClickListener { exportConfiguration() }
        importButton?.setOnClickListener { importConfiguration() }

        // Slider listeners
        scaleSlider?.addOnChangeListener { _, value, _ ->
            selectedComponent?.let { component ->
                component.scaleX = value
                component.scaleY = value

                // Para ImageView, también ajustar el layout params para que se adapte mejor
                if (component is ImageView) {
                    val baseSize = 72 // Tamaño base en dp
                    val newSize = (baseSize * value).toInt()

                    component.layoutParams?.let { params ->
                        params.width = (newSize * context.resources.displayMetrics.density).toInt()
                        params.height = (newSize * context.resources.displayMetrics.density).toInt()
                        component.layoutParams = params
                    }
                }

                selectionOverlay?.updateSelection(component)
            }
        }

        textSizeSlider?.addOnChangeListener { _, value, _ ->
            selectedComponent?.let { component ->
                when {
                    // Edición grupal de TextInputLayouts
                    groupEditCheckbox?.isChecked == true && component is TextInputLayout -> {
                        applyTextSizeToAllTextInputs(value)
                    }
                    // NUEVO: Edición grupal de MaterialButtons
                    groupEditCheckbox?.isChecked == true && component is MaterialButton -> {
                        applyTextSizeToAllButtons(value)
                    }
                    // Edición individual
                    else -> {
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
                            is MaterialButton -> {  // NUEVO
                                Log.d("EditModeManager", "Aplicando textSize al MaterialButton: ${value}sp")
                                component.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, value)
                            }
                        }
                    }
                }
            }
        }

        letterSpacingSlider?.addOnChangeListener { _, value, _ ->
            selectedComponent?.let { component ->
                when {
                    // Edición grupal de TextInputLayouts
                    groupEditCheckbox?.isChecked == true && component is TextInputLayout -> {
                        applyLetterSpacingToAllTextInputs(value)
                    }
                    // NUEVO: Edición grupal de MaterialButtons
                    groupEditCheckbox?.isChecked == true && component is MaterialButton -> {
                        applyLetterSpacingToAllButtons(value)
                    }
                    // Edición individual
                    else -> {
                        when (component) {
                            is TextView -> component.letterSpacing = value
                            is TextInputLayout -> component.editText?.letterSpacing = value
                            is MaterialButton -> component.letterSpacing = value  // NUEVO
                        }
                    }
                }
            }
        }
        
        // Add touch listeners for transparency effect while dragging
        addSliderTouchListeners()

        applyChangesButton?.setOnClickListener {
            selectedComponent?.let {
                saveComponentConfiguration(it)
                hideEditControls()
                showSnackbar("Cambios aplicados")
            }
        }

        cancelChangesButton?.setOnClickListener {
            selectedComponent?.let {
                loadComponentConfiguration(it)
                hideEditControls()
            }
        }
    }
    
    private fun addSliderTouchListeners() {
        // Track original alpha values
        val originalPanelAlpha = 1.0f

        // Common touch listener for all sliders
        val sliderTouchListener = object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                // Hacer transparente el panel de controles completo
                editControlsPanel?.animate()
                    ?.alpha(0.3f)  // 30% de opacidad
                    ?.setDuration(150)
                    ?.start()
            }

            override fun onStopTrackingTouch(slider: Slider) {
                // Restaurar opacidad del panel
                editControlsPanel?.animate()
                    ?.alpha(originalPanelAlpha)
                    ?.setDuration(150)
                    ?.start()
            }
        }

        // Apply the same touch listener to all sliders
        scaleSlider?.addOnSliderTouchListener(sliderTouchListener)
        textSizeSlider?.addOnSliderTouchListener(sliderTouchListener)
        letterSpacingSlider?.addOnSliderTouchListener(sliderTouchListener)
    }

    private fun toggleEditMode() {
        Log.d(TAG, "toggleEditMode llamado - modo actual: $isEditMode")
        isEditMode = !isEditMode
        if (isEditMode) {
            Log.d(TAG, "Entrando en modo edición")
            enterEditMode()
        } else {
            Log.d(TAG, "Saliendo del modo edición")
            exitEditMode(true)
        }
    }

    private fun enterEditMode() {
        // Save original positions
        saveOriginalPositions()

        // Change edit button to close (X) with red background
        editButton?.let { button ->
            button.setImageResource(R.drawable.ic_close)
            button.backgroundTintList = ContextCompat.getColorStateList(context, android.R.color.holo_red_dark)
        }

        // Show/hide UI elements
        editButtonsContainer?.visibility = View.VISIBLE
        // Don't show the dark overlay - allow full interaction
        selectionOverlay?.visibility = View.VISIBLE

        // Disable interactive components to prevent interference
        disableInteractiveComponents()

        // Setup touch listeners for all editable components
        setupEditModeTouchListeners()
        
        // Add visual indicators to editable components
        highlightEditableComponents()

        showSnackbar("Modo de edición activado. Toca los componentes para editarlos.")
    }

    private fun exitEditMode(saveChanges: Boolean) {
        if (saveChanges) {
            // Save all component configurations when exiting edit mode
            Log.d(TAG, "Guardando configuración al salir del modo edición para servicio: $currentServiceType")
            getAllEditableViews().forEach { view ->
                saveComponentConfiguration(view)
            }
            Log.d(TAG, "Configuración guardada exitosamente")
        } else {
            restoreOriginalPositions()
        }

        isEditMode = false
        
        // Restore edit button to original state
        editButton?.let { button ->
            button.setImageResource(R.drawable.ic_edit)
            button.backgroundTintList = ContextCompat.getColorStateList(context, R.color.md_theme_light_primary)
        }
        
        editButtonsContainer?.visibility = View.GONE

        hideEditControls()
        selectionOverlay?.visibility = View.GONE
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
            // Store original click listener before overriding
            val originalClickListener = when (view.id) {
                R.id.executeUSSDButton, R.id.manualReferenceButton -> {
                    // For buttons, we don't want to store anything as they have their listeners set in MainActivity
                    null
                }
                else -> null
            }
            originalClickListeners[view] = originalClickListener
            
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
            
            // Only override click listener for non-button components in edit mode
            if (view.id != R.id.executeUSSDButton && view.id != R.id.manualReferenceButton) {
                view.setOnClickListener {
                    if (isEditMode) {
                        selectComponent(view)
                        showEditControls(view)
                    } else {
                        // Allow normal functionality when not in edit mode
                        originalClickListeners[view]?.onClick(view)
                    }
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
            
            // Only restore click listener for non-button components
            if (view.id != R.id.executeUSSDButton && view.id != R.id.manualReferenceButton) {
                // Restore original click listener
                view.setOnClickListener(originalClickListeners[view])
            }
            // For buttons, don't touch their click listeners as they are managed by MainActivity
            
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
        originalClickListeners.clear()
    }

    private fun handleComponentTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                selectComponent(view)
                return true
            }

            MotionEvent.ACTION_UP -> {
                // Always show edit controls on tap (no dragging/movement allowed)
                showEditControls(view)
                return true
            }
        }
        return false
    }

    // Movement functions removed - only size/text editing allowed

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
        
        selectionOverlay?.showSelection(view)
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

        selectedComponent = view

        // Update control panel title
        editControlsTitle?.text = "Editando: ${getComponentName(view)}"

        // Obtener referencia al LinearLayout que contiene el scale
        val scaleControl = activity.findViewById<LinearLayout>(R.id.scaleControl)

        // Determinar qué controles mostrar basándose en el tipo de componente
        val isTextComponent = view is TextView || view is TextInputLayout || view is MaterialButton
        val isImageComponent = view is ImageView

        if (isImageComponent) {
            // Para imágenes: mostrar solo scale con rango más amplio
            scaleControl?.visibility = View.VISIBLE
            textSizeControl?.visibility = View.GONE
            letterSpacingControl?.visibility = View.GONE
            groupEditCheckbox?.visibility = View.GONE

            // Para imágenes, permitir un rango mayor de escala
            scaleSlider?.valueFrom = 0.3f
            scaleSlider?.valueTo = 3.0f
            scaleSlider?.stepSize = 0.1f

            // Set current scale value
            val alignedScale = alignToStepSize(view.scaleX, 0.3f, 0.1f)
            scaleSlider?.value = alignedScale
        } else if (isTextComponent) {
            // Para componentes de texto: ocultar scale, mostrar solo texto
            scaleControl?.visibility = View.GONE
            textSizeControl?.visibility = View.VISIBLE
            letterSpacingControl?.visibility = View.VISIBLE

            // Restaurar valores normales del slider si se cambió
            scaleSlider?.valueFrom = 0.5f
            scaleSlider?.valueTo = 2.0f
            scaleSlider?.stepSize = 0.1f

            // Actualizar texto del checkbox según el tipo
            groupEditCheckbox?.visibility = when (view) {
                is TextInputLayout -> {
                    groupEditCheckbox?.text = "Editar todos los campos de texto"
                    View.VISIBLE
                }
                is MaterialButton -> {
                    groupEditCheckbox?.text = "Editar todos los botones"
                    View.VISIBLE
                }
                else -> View.GONE
            }

            // Configurar valores de texto...
            when (view) {
                is TextView -> {
                    val textSizeInSp = view.textSize / context.resources.displayMetrics.scaledDensity
                    val clampedTextSize = textSizeInSp.coerceIn(12f, 28f)
                    val alignedTextSize = alignToStepSize(clampedTextSize, 12f, 1f)
                    textSizeSlider?.value = alignedTextSize

                    val alignedLetterSpacing = alignToStepSize(view.letterSpacing, 0f, 0.01f)
                    letterSpacingSlider?.value = alignedLetterSpacing
                }
                is TextInputLayout -> {
                    view.editText?.let { editText ->
                        val textSizeInSp = editText.textSize / context.resources.displayMetrics.scaledDensity
                        val clampedTextSize = textSizeInSp.coerceIn(12f, 28f)
                        val alignedTextSize = alignToStepSize(clampedTextSize, 12f, 1f)
                        textSizeSlider?.value = alignedTextSize

                        val alignedLetterSpacing = alignToStepSize(editText.letterSpacing, 0f, 0.01f)
                        letterSpacingSlider?.value = alignedLetterSpacing
                    }
                }
                is MaterialButton -> {
                    val textSizeInSp = view.textSize / context.resources.displayMetrics.scaledDensity
                    val clampedTextSize = textSizeInSp.coerceIn(12f, 28f)
                    val alignedTextSize = alignToStepSize(clampedTextSize, 12f, 1f)
                    textSizeSlider?.value = alignedTextSize

                    val alignedLetterSpacing = alignToStepSize(view.letterSpacing, 0f, 0.01f)
                    letterSpacingSlider?.value = alignedLetterSpacing
                }
            }
        }

        // Position and show control panel
        positionEditControlsPanel(view)
        editControlsPanel?.visibility = View.VISIBLE
        editControlsPanel?.animate()
            ?.alpha(1f)
            ?.translationY(0f)
            ?.setDuration(300)
            ?.start()
    }

    private fun hideEditControls() {
        editControlsPanel?.animate()
            ?.alpha(0f)
            ?.translationY(100f)
            ?.setDuration(200)
            ?.withEndAction {
                editControlsPanel?.visibility = View.GONE
            }
            ?.start()
    }

    private fun getComponentName(view: View): String {
        return when (view.id) {
            R.id.serviceImage -> "Imagen del Servicio"
            R.id.serviceTitle -> "Título del Servicio"
            R.id.phoneInputLayout -> "Campo de Teléfono"
            R.id.cedulaInputLayout -> "Campo de Cédula"
            R.id.amountInputLayout -> "Campo de Monto"
            R.id.dateInputLayout -> "Campo de Fecha"
            R.id.executeUSSDButton -> "Botón Ejecutar USSD"        // NUEVO
            R.id.manualReferenceButton -> "Botón Referencia Manual" // NUEVO
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
            R.id.executeUSSDButton,    // NUEVO: Botón Ejecutar USSD
            R.id.manualReferenceButton  // NUEVO: Botón Referencia Manual
        )

        ids.forEach { id ->
            activity.findViewById<View>(id)?.let { views.add(it) }
        }

        return views
    }

    private fun saveAllChanges() {
        // Save all changes and exit edit mode
        // Note: exitEditMode(true) will also save all changes, so we just call it directly
        exitEditMode(true)
    }

    private fun saveComponentConfiguration(view: View) {
        val componentKey = getComponentKey(view)
        val componentName = getComponentName(view)

        // Save only scale (no position changes allowed)
        preferencesManager.setFloat(
            getServiceComponentKey(COMPONENT_SCALE_KEY, componentKey), view.scaleX
        )
        
        Log.d(TAG, "Guardando configuración para $componentName: scale=${view.scaleX}")

        // Save text properties if applicable
        when (view) {
            is TextView -> {
                // Convert px to sp for consistent storage
                val textSizeInSp = view.textSize / context.resources.displayMetrics.scaledDensity
                Log.d(TAG, "Guardando $componentName textSize: ${view.textSize}px = ${textSizeInSp}sp, letterSpacing=${view.letterSpacing}")
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
                    Log.d(TAG, "Guardando $componentName editText textSize: ${editText.textSize}px = ${textSizeInSp}sp, letterSpacing=${editText.letterSpacing}")
                    preferencesManager.setFloat(
                        getServiceComponentKey(COMPONENT_TEXT_SIZE_KEY, componentKey), textSizeInSp
                    )
                    preferencesManager.setFloat(
                        getServiceComponentKey(COMPONENT_LETTER_SPACING_KEY, componentKey), editText.letterSpacing
                    )
                }
            }
            is MaterialButton -> {  // NUEVO
                // Convert px to sp for consistent storage
                val textSizeInSp = view.textSize / context.resources.displayMetrics.scaledDensity
                Log.d(TAG, "Guardando $componentName button textSize: ${view.textSize}px = ${textSizeInSp}sp, letterSpacing=${view.letterSpacing}")
                preferencesManager.setFloat(
                    getServiceComponentKey(COMPONENT_TEXT_SIZE_KEY, componentKey), textSizeInSp
                )
                preferencesManager.setFloat(
                    getServiceComponentKey(COMPONENT_LETTER_SPACING_KEY, componentKey), view.letterSpacing
                )
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

        // Load scale
        val scale = preferencesManager.getFloatValue(scaleKey, 1.0f)
        view.scaleX = scale
        view.scaleY = scale

        // Para ImageView, también ajustar el tamaño del layout
        if (view is ImageView) {
            val baseSize = 72 // Tamaño base en dp
            val newSize = (baseSize * scale).toInt()

            view.layoutParams?.let { params ->
                params.width = (newSize * context.resources.displayMetrics.density).toInt()
                params.height = (newSize * context.resources.displayMetrics.density).toInt()
                view.layoutParams = params
            }
        }

        // Load text properties if applicable
        when (view) {
            is TextView -> {
                val textSizeInSp = preferencesManager.getFloatValue(
                    getServiceComponentKey(COMPONENT_TEXT_SIZE_KEY, componentKey), 16f
                )
                val letterSpacing = preferencesManager.getFloatValue(
                    getServiceComponentKey(COMPONENT_LETTER_SPACING_KEY, componentKey), 0f
                )

                Log.d(TAG, "Cargando ${getComponentName(view)} textSize: ${textSizeInSp}sp, letterSpacing: $letterSpacing")
                // Use setTextSize with TypedValue.COMPLEX_UNIT_SP to ensure correct unit
                view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, textSizeInSp)
                view.letterSpacing = letterSpacing
            }
            is TextInputLayout -> {
                view.editText?.let { editText ->
                    val textSizeInSp = preferencesManager.getFloatValue(
                        getServiceComponentKey(COMPONENT_TEXT_SIZE_KEY, componentKey), 16f
                    )
                    val letterSpacing = preferencesManager.getFloatValue(
                        getServiceComponentKey(COMPONENT_LETTER_SPACING_KEY, componentKey), 0f
                    )

                    Log.d(TAG, "Cargando ${getComponentName(view)} editText textSize: ${textSizeInSp}sp, letterSpacing: $letterSpacing")
                    // Use setTextSize with TypedValue.COMPLEX_UNIT_SP to ensure correct unit
                    editText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, textSizeInSp)
                    editText.letterSpacing = letterSpacing
                }
            }
            is MaterialButton -> {
                val textSizeInSp = preferencesManager.getFloatValue(
                    getServiceComponentKey(COMPONENT_TEXT_SIZE_KEY, componentKey), 16f
                )
                val letterSpacing = preferencesManager.getFloatValue(
                    getServiceComponentKey(COMPONENT_LETTER_SPACING_KEY, componentKey), 0f
                )

                Log.d(TAG, "Cargando ${getComponentName(view)} button textSize: ${textSizeInSp}sp, letterSpacing: $letterSpacing")
                view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, textSizeInSp)
                view.letterSpacing = letterSpacing
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
            R.id.executeUSSDButton -> "executeUSSDButton"        // NUEVO
            R.id.manualReferenceButton -> "manualReferenceButton" // NUEVO
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
        when (view) {
            is ImageView -> {
                // Para imágenes, usar escala 1.0 como default
                view.scaleX = 1.0f
                view.scaleY = 1.0f
            }
            is TextView -> {
                // Reset scale para texto
                view.scaleX = 1.0f
                view.scaleY = 1.0f
                // Use default values that match the slider ranges with proper SP units
                view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16f)
                view.letterSpacing = 0f
            }
            is TextInputLayout -> {
                // Reset scale
                view.scaleX = 1.0f
                view.scaleY = 1.0f
                view.editText?.let { editText ->
                    // Use default values that match the slider ranges with proper SP units
                    editText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16f)
                    editText.letterSpacing = 0f
                }
            }
            is MaterialButton -> {
                // Reset scale
                view.scaleX = 1.0f
                view.scaleY = 1.0f
                // Use default values that match the slider ranges with proper SP units
                view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16f)
                view.letterSpacing = 0f
            }
            else -> {
                // Para cualquier otro componente
                view.scaleX = 1.0f
                view.scaleY = 1.0f
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
                    is MaterialButton -> {  // NUEVO
                        componentConfig.put("textSize", view.textSize)
                        componentConfig.put("letterSpacing", view.letterSpacing)
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
                                // IMPORTANTE: textSize en el JSON está en px, convertir a sp
                                val textSizeInPx = it.optDouble("textSize", 32.0).toFloat()
                                val textSizeInSp = textSizeInPx / context.resources.displayMetrics.scaledDensity
                                view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, textSizeInSp)
                                view.letterSpacing = it.optDouble("letterSpacing", 0.0).toFloat()
                            }
                            is TextInputLayout -> {
                                view.editText?.let { editText ->
                                    // IMPORTANTE: textSize en el JSON está en px, convertir a sp
                                    val textSizeInPx = it.optDouble("textSize", 32.0).toFloat()
                                    val textSizeInSp = textSizeInPx / context.resources.displayMetrics.scaledDensity
                                    editText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, textSizeInSp)
                                    editText.letterSpacing = it.optDouble("letterSpacing", 0.0).toFloat()
                                }
                            }
                            is MaterialButton -> {  // NUEVO
                                // IMPORTANTE: textSize en el JSON está en px, convertir a sp
                                val textSizeInPx = it.optDouble("textSize", 32.0).toFloat()
                                val textSizeInSp = textSizeInPx / context.resources.displayMetrics.scaledDensity
                                view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, textSizeInSp)
                                view.letterSpacing = it.optDouble("letterSpacing", 0.0).toFloat()
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
        Log.d(TAG, "Cargando configuración para servicio tipo: $currentServiceType")
        val viewsWithConfig = mutableListOf<String>()
        val viewsWithoutConfig = mutableListOf<String>()
        
        getAllEditableViews().forEach { view ->
            val componentKey = getComponentKey(view)
            val scaleKey = getServiceComponentKey(COMPONENT_SCALE_KEY, componentKey)
            val hasConfig = preferencesManager.contains(scaleKey)
            
            if (hasConfig) {
                viewsWithConfig.add(getComponentName(view))
            } else {
                viewsWithoutConfig.add(getComponentName(view))
            }
            
            loadComponentConfiguration(view)
        }
        
        Log.d(TAG, "Configuración encontrada para: $viewsWithConfig")
        Log.d(TAG, "Sin configuración (usando defaults): $viewsWithoutConfig")
        Log.d(TAG, "Configuración cargada completamente para servicio $currentServiceType")
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
        
        // Disable action buttons that should not work in edit mode
        activity.findViewById<MaterialButton>(R.id.executeUSSDButton)?.let { button ->
            button.isEnabled = false
            button.alpha = 0.5f
            Log.d("EditModeManager", "executeUSSDButton deshabilitado")
        }
        
        activity.findViewById<MaterialButton>(R.id.manualReferenceButton)?.let { button ->
            button.isEnabled = false
            button.alpha = 0.5f
            Log.d("EditModeManager", "manualReferenceButton deshabilitado")
        }
        
        // Disable text inputs interaction during edit mode
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
    
    
    private fun enableEditMode() {
        isEditModeEnabled = true
        
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
        
        // Re-enable action buttons
        activity.findViewById<MaterialButton>(R.id.executeUSSDButton)?.let { button ->
            button.isEnabled = true
            button.alpha = 1.0f
            Log.d("EditModeManager", "executeUSSDButton habilitado")
        }
        
        activity.findViewById<MaterialButton>(R.id.manualReferenceButton)?.let { button ->
            button.isEnabled = true
            button.alpha = 1.0f
            Log.d("EditModeManager", "manualReferenceButton habilitado")
        }
        
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
        editControlsPanel?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                editControlsPanel?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                
                val targetLocation = IntArray(2)
                targetView.getLocationInWindow(targetLocation)
                
                val panelLocation = IntArray(2)
                editControlsPanel?.getLocationInWindow(panelLocation)
                
                val rootLocation = IntArray(2)
                rootLayout.getLocationInWindow(rootLocation)
                
                // Calculate desired position (below the target view)
                val targetBottom = targetLocation[1] + targetView.height
                val availableSpaceBelow = rootLayout.height + rootLocation[1] - targetBottom
                val panelHeight = editControlsPanel?.height
                
                Log.d("EditModeManager", "Target bottom: $targetBottom, Available space below: $availableSpaceBelow, Panel height: $panelHeight")
                
                val layoutParams = editControlsPanel?.layoutParams as CoordinatorLayout.LayoutParams

                if (panelHeight != null) {
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
                }
                
                editControlsPanel?.layoutParams  = layoutParams
                editControlsPanel?.requestLayout()
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

    private fun isTextInputLayout(view: View): Boolean {
        return view is TextInputLayout
    }
    
    private fun applyTextSizeToAllTextInputs(textSize: Float) {
        getAllEditableViews().forEach { view ->
            if (view is TextInputLayout) {
                view.editText?.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, textSize)
                Log.d("EditModeManager", "Aplicando textSize grupal: ${textSize}sp a ${getComponentName(view)}")
            }
        }
    }
    
    private fun applyLetterSpacingToAllTextInputs(letterSpacing: Float) {
        getAllEditableViews().forEach { view ->
            if (view is TextInputLayout) {
                view.editText?.letterSpacing = letterSpacing
                Log.d("EditModeManager", "Aplicando letterSpacing grupal: $letterSpacing a ${getComponentName(view)}")
            }
        }
    }
    
    private fun applyTextSizeToAllButtons(textSize: Float) {
        getAllEditableViews().forEach { view ->
            if (view is MaterialButton) {
                view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, textSize)
                Log.d("EditModeManager", "Aplicando textSize grupal: ${textSize}sp a ${getComponentName(view)}")
            }
        }
    }

    private fun applyLetterSpacingToAllButtons(letterSpacing: Float) {
        getAllEditableViews().forEach { view ->
            if (view is MaterialButton) {
                view.letterSpacing = letterSpacing
                Log.d("EditModeManager", "Aplicando letterSpacing grupal: $letterSpacing a ${getComponentName(view)}")
            }
        }
    }
    
    private fun applyScaleToAllTextInputs(scale: Float) {
        getAllEditableViews().forEach { view ->
            if (view is TextInputLayout) {
                // Limit scale to prevent breaking layout constraints
                val maxScale = 1.5f
                val minScale = 0.8f
                val constrainedScale = scale.coerceIn(minScale, maxScale)
                
                view.scaleX = constrainedScale
                view.scaleY = constrainedScale
                Log.d("EditModeManager", "Aplicando scale grupal: $constrainedScale a ${getComponentName(view)}")
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