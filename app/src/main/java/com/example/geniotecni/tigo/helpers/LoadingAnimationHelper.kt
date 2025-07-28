package com.example.geniotecni.tigo.helpers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.example.geniotecni.tigo.R
import kotlinx.coroutines.*

class LoadingAnimationHelper(private val activity: AppCompatActivity, private val context: Context) {

    private var loadingView: View? = null
    private var progressBar: ProgressBar? = null
    private var fadeInAnimation: Animation? = null
    private var fadeOutAnimation: Animation? = null
    
    init {
        setupAnimations()
    }
    
    private fun setupAnimations() {
        fadeInAnimation = AnimationUtils.loadAnimation(activity, android.R.anim.fade_in)
        fadeOutAnimation = AnimationUtils.loadAnimation(activity, android.R.anim.fade_out)
        
        // Set animation duration
        fadeInAnimation?.duration = 300
        fadeOutAnimation?.duration = 300
    }
    
    fun showLoading(message: String = "Cargando...", duration: Long = 1500L) {
        // For now, we'll just show a simple toast for loading indication
        // In the future, this can be enhanced with a proper overlay
    }
    
    fun hideLoading() {
        // Simple implementation - no overlay to hide for now
    }
    
    fun showLoadingAndNavigate(
        targetActivity: Class<*>,
        message: String = "Cargando...",
        duration: Long = 1500L,
        extras: Intent.() -> Unit = {}
    ) {
        // Use coroutine to wait for brief delay then navigate
        CoroutineScope(Dispatchers.Main).launch {
            delay(duration)
            
            val intent = Intent(activity, targetActivity).apply(extras)
            activity.startActivity(intent)
            
            // Add custom transition animation
            activity.overridePendingTransition(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
            )
        }
    }
    
    fun showProcessingAnimation(
        onComplete: () -> Unit,
        message: String = "Procesando...",
        duration: Long = 2000L
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            delay(duration)
            onComplete()
        }
    }
    
    fun createSlideTransition(entering: Boolean = true) {
        if (entering) {
            activity.overridePendingTransition(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
            )
        } else {
            activity.overridePendingTransition(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
            )
        }
    }
    
    fun createFadeTransition() {
        activity.overridePendingTransition(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
    }
    
    fun createScaleTransition() {
        // Custom scale animations would be defined in res/anim folder
        activity.overridePendingTransition(
            R.anim.scale_in,
            R.anim.scale_out
        )
    }
    
    companion object {
        fun applySlideTransition(activity: Activity) {
            activity.overridePendingTransition(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
            )
        }
        
        fun applyFadeTransition(activity: Activity) {
            activity.overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        }
        
        fun applyZoomTransition(activity: Activity) {
            // This would use custom animations in res/anim
            activity.overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        }
    }
}