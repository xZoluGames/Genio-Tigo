package com.example.geniotecni.tigo.ui.common

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import com.example.geniotecni.tigo.R
import com.example.geniotecni.tigo.ui.adapters.ServiceItem
import com.example.geniotecni.tigo.models.PrintData
import com.example.geniotecni.tigo.data.processors.TransactionDataProcessor

/**
 * Functional adapter components to eliminate redundancy across adapters.
 * Uses higher-order functions and composition patterns.
 */
object FunctionalAdapterComponents {

    /**
     * Icon loading strategy - eliminates code duplication
     */
    sealed class IconLoadingStrategy {
        object VectorIcon : IconLoadingStrategy()
        object BitmapImage : IconLoadingStrategy()
        
        companion object {
            fun determineStrategy(context: Context, resourceId: Int): IconLoadingStrategy {
                return try {
                    val resourceName = context.resources.getResourceName(resourceId)
                    if (resourceName.contains("/ic_") && 
                        !resourceName.contains(".webp") && 
                        !resourceName.contains(".png") && 
                        !resourceName.contains(".jpg")) {
                        VectorIcon
                    } else {
                        BitmapImage
                    }
                } catch (e: Exception) {
                    BitmapImage // Default fallback
                }
            }
        }
    }

    /**
     * Functional icon loader - reusable across all adapters
     */
    fun loadIcon(
        imageView: ImageView,
        backgroundView: View,
        iconRes: Int,
        colorRes: Int,
        context: Context,
        onError: ((Exception) -> Unit)? = null
    ) {
        try {
            val strategy = IconLoadingStrategy.determineStrategy(context, iconRes)
            
            imageView.setImageResource(iconRes)
            
            when (strategy) {
                is IconLoadingStrategy.VectorIcon -> {
                    // Vector icons: apply white tint and colored background
                    val color = ContextCompat.getColor(context, colorRes)
                    backgroundView.background?.setTint(color)
                    imageView.setColorFilter(
                        ContextCompat.getColor(context, android.R.color.white),
                        android.graphics.PorterDuff.Mode.SRC_IN
                    )
                    imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                    imageView.adjustViewBounds = true
                }
                is IconLoadingStrategy.BitmapImage -> {
                    // Bitmap images: remove tint and use neutral background
                    imageView.clearColorFilter()
                    imageView.imageTintList = null
                    
                    val lightGrayColor = ContextCompat.getColor(context, R.color.md_theme_light_surfaceVariant)
                    backgroundView.background?.setTint(lightGrayColor)
                    
                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                    imageView.adjustViewBounds = false
                    
                    // Adjust size for images if dimension exists
                    try {
                        val imageSize = context.resources.getDimensionPixelSize(R.dimen.service_icon_image_size)
                        val layoutParams = imageView.layoutParams
                        layoutParams.width = imageSize
                        layoutParams.height = imageSize
                        imageView.layoutParams = layoutParams
                    } catch (e: Exception) {
                        // Dimension not found, keep original size
                    }
                }
            }
        } catch (e: Exception) {
            onError?.invoke(e) ?: run {
                // Default fallback behavior
                imageView.setImageResource(R.drawable.ic_service_default)
                imageView.clearColorFilter()
                val defaultColor = ContextCompat.getColor(context, colorRes)
                backgroundView.background?.setTint(defaultColor)
            }
        }
    }

    /**
     * Visibility controller - functional approach to show/hide views
     */
    fun setViewVisibility(
        view: View,
        content: String?,
        setText: (String) -> Unit = { (view as? TextView)?.text = it }
    ) {
        if (!content.isNullOrEmpty()) {
            view.visibility = View.VISIBLE
            setText(content)
        } else {
            view.visibility = View.GONE
        }
    }

    /**
     * Field setup composer - eliminates repetitive field setup code
     */
    data class FieldSetup(
        val view: View,
        val textView: TextView,
        val content: String?,
        val label: String? = null
    )

    fun setupFields(vararg fields: FieldSetup) {
        fields.forEach { field ->
            setViewVisibility(field.view, field.content) { content ->
                field.textView.text = content
                
                // Update label if provided
                field.label?.let { label ->
                    try {
                        val labelView = (field.view as? android.view.ViewGroup)?.getChildAt(0) as? TextView
                        labelView?.text = label
                    } catch (e: Exception) {
                        // Label update failed, continue without it
                    }
                }
            }
        }
    }

    /**
     * Animation composer - reusable animation patterns
     */
    object AnimationComposer {
        fun fadeInWithDelay(view: View, position: Int, duration: Long = 200L) {
            view.alpha = 0f
            view.animate()
                .alpha(1f)
                .setDuration(duration)
                .setStartDelay((position * 30).toLong())
                .start()
        }

        fun scaleOnClick(view: View, onComplete: () -> Unit) {
            view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .withEndAction { onComplete() }
                        .start()
                }
                .start()
        }
    }

    /**
     * DiffUtil callback generator - eliminates boilerplate
     */
    inline fun <reified T> createDiffCallback(
        oldList: List<T>,
        newList: List<T>,
        crossinline areItemsTheSame: (T, T) -> Boolean,
        crossinline areContentsTheSame: (T, T) -> Boolean = { old, new -> old == new }
    ): DiffUtil.Callback {
        return object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldList.size
            override fun getNewListSize(): Int = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return areItemsTheSame(oldList[oldItemPosition], newList[newItemPosition])
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return areContentsTheSame(oldList[oldItemPosition], newList[newItemPosition])
            }
        }
    }

    /**
     * Service-specific extensions for functional composition
     */
    object ServiceExtensions {
        fun ServiceItem.isViewMoreItem(): Boolean = this.name == "Ver más"
        
        fun ServiceItem.getViewType(): Int = if (isViewMoreItem()) 1 else 0
        
        fun List<ServiceItem>.updateWith(
            newItems: List<ServiceItem>,
            adapter: androidx.recyclerview.widget.RecyclerView.Adapter<*>
        ) {
            val diffCallback = createDiffCallback(
                oldList = this,
                newList = newItems,
                areItemsTheSame = { old, new -> old.id == new.id }
            )
            val diffResult = DiffUtil.calculateDiff(diffCallback)
            diffResult.dispatchUpdatesTo(adapter)
        }
    }

    /**
     * Transaction data extensions for print history
     */
    object TransactionExtensions {
        fun TransactionDataProcessor.ProcessedTransactionData.hasValidData(): Boolean {
            return !phone.isNullOrEmpty() || !cedula.isNullOrEmpty() || 
                   !formattedAmount.isNullOrEmpty() || reference1 != null || reference2 != null
        }
        
        fun TransactionDataProcessor.ProcessedTransactionData.getPrimaryReference(): String {
            return reference1 ?: reference2 ?: legacyReference ?: "N/A"
        }
    }

    /**
     * Status chip helper - functional approach
     */
    fun updateStatusChip(
        chip: com.google.android.material.chip.Chip,
        isCompleted: Boolean,
        context: Context
    ) {
        val (text, colorRes) = if (isCompleted) {
            "Completado" to R.color.status_success
        } else {
            "Pendiente" to R.color.status_warning
        }
        
        chip.text = text
        chip.chipBackgroundColor = ContextCompat.getColorStateList(context, colorRes)
    }

    /**
     * Click handler composer - eliminates repetitive click handling
     */
    fun View.setOnClickWithAnimation(action: () -> Unit) {
        setOnClickListener {
            AnimationComposer.scaleOnClick(this, action)
        }
    }

    /**
     * Error handling composer for adapters
     */
    inline fun <T> safeAdapterOperation(
        operation: () -> T,
        onError: (Exception) -> Unit = {},
        defaultValue: T? = null
    ): T? {
        return try {
            operation()
        } catch (e: Exception) {
            onError(e)
            defaultValue
        }
    }

    /**
     * Text formatting utilities - reduces repetitive string operations
     */
    object TextFormatters {
        fun formatDateTime(date: String, time: String): String = "$date $time"
        
        fun formatAmount(amount: String): String {
            return if (amount.endsWith("Gs.")) amount else "$amount Gs."
        }
        
        fun formatReference(ref: String?): String = ref?.takeIf { it.isNotEmpty() } ?: "N/A"
    }

    /**
     * Memory optimization utilities
     */
    object MemoryOptimizer {
        fun <T> List<T>.chunked(chunkSize: Int = 20): List<List<T>> {
            return this.chunked(chunkSize)
        }
        
        fun clearImageCache(imageView: ImageView) {
            imageView.setImageDrawable(null)
            imageView.clearColorFilter()
            imageView.imageTintList = null
        }
    }
}