package com.example.geniotecni.tigo.helpers

import android.animation.*
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import androidx.core.view.ViewCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.RecyclerView
import android.os.Build
import android.view.ViewAnimationUtils

class AnimationHelper {

    companion object {
        const val DURATION_SHORT = 200L
        const val DURATION_MEDIUM = 300L
        const val DURATION_LONG = 500L

        private val defaultInterpolator = FastOutSlowInInterpolator()
    }

    // Fade animations
    fun fadeIn(view: View, duration: Long = DURATION_MEDIUM, onEnd: (() -> Unit)? = null) {
        view.apply {
            alpha = 0f
            visibility = View.VISIBLE
            animate()
                .alpha(1f)
                .setDuration(duration)
                .setInterpolator(defaultInterpolator)
                .withEndAction { onEnd?.invoke() }
                .start()
        }
    }

    fun fadeOut(view: View, duration: Long = DURATION_MEDIUM, onEnd: (() -> Unit)? = null) {
        view.animate()
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(defaultInterpolator)
            .withEndAction {
                view.visibility = View.GONE
                onEnd?.invoke()
            }
            .start()
    }

    // Scale animations
    fun scaleIn(view: View, duration: Long = DURATION_MEDIUM) {
        view.apply {
            scaleX = 0f
            scaleY = 0f
            visibility = View.VISIBLE
            animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(duration)
                .setInterpolator(OvershootInterpolator())
                .start()
        }
    }

    fun scaleOut(view: View, duration: Long = DURATION_MEDIUM) {
        view.animate()
            .scaleX(0f)
            .scaleY(0f)
            .setDuration(duration)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction { view.visibility = View.GONE }
            .start()
    }

    // Slide animations
    fun slideInFromBottom(view: View, duration: Long = DURATION_MEDIUM) {
        view.apply {
            translationY = height.toFloat()
            visibility = View.VISIBLE
            animate()
                .translationY(0f)
                .setDuration(duration)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    fun slideOutToBottom(view: View, duration: Long = DURATION_MEDIUM) {
        view.animate()
            .translationY(view.height.toFloat())
            .setDuration(duration)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction { view.visibility = View.GONE }
            .start()
    }

    // Card elevation animation
    fun animateCardPress(view: View, isPressedDown: Boolean) {
        val elevation = if (isPressedDown) 2f else 8f
        val scale = if (isPressedDown) 0.96f else 1f

        view.animate()
            .scaleX(scale)
            .scaleY(scale)
            .setDuration(100)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        ViewCompat.animate(view)
            .translationZ(elevation)
            .setDuration(100)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    // RecyclerView item animations
    fun animateRecyclerView(recyclerView: RecyclerView) {
        val controller = AnimationUtils.loadLayoutAnimation(
            recyclerView.context,
            android.R.anim.slide_in_left
        )
        recyclerView.layoutAnimation = controller
        recyclerView.scheduleLayoutAnimation()
    }

    // Ripple effect
    fun createRippleEffect(view: View, x: Float, y: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.background?.let { background ->
                background.setHotspot(x, y)
                background.state = intArrayOf(
                    android.R.attr.state_pressed,
                    android.R.attr.state_enabled
                )
            }
        }
    }

    // Shake animation for errors
    fun shake(view: View) {
        val shake = ObjectAnimator.ofFloat(view, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f)
        shake.duration = 500
        shake.start()
    }

    // Bounce animation
    fun bounce(view: View) {
        val bounceAnim = AnimatorSet()
        val scaleXAnim = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.2f, 1f)
        val scaleYAnim = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.2f, 1f)

        bounceAnim.playTogether(scaleXAnim, scaleYAnim)
        bounceAnim.duration = 300
        bounceAnim.interpolator = BounceInterpolator()
        bounceAnim.start()
    }

    // Circular reveal animation
    fun circularReveal(view: View, centerX: Int, centerY: Int, startRadius: Float, endRadius: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val anim = ViewAnimationUtils.createCircularReveal(view, centerX, centerY, startRadius, endRadius)
            anim.duration = DURATION_LONG
            view.visibility = View.VISIBLE
            anim.start()
        } else {
            fadeIn(view)
        }
    }

    // Morph animation between views
    fun morphView(fromView: View, toView: View, duration: Long = DURATION_LONG) {
        val scaleX = PropertyValuesHolder.ofFloat("scaleX", 1f, 0f)
        val scaleY = PropertyValuesHolder.ofFloat("scaleY", 1f, 0f)
        val alpha = PropertyValuesHolder.ofFloat("alpha", 1f, 0f)

        val hideAnimator = ObjectAnimator.ofPropertyValuesHolder(fromView, scaleX, scaleY, alpha)
        hideAnimator.duration = duration / 2

        hideAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                fromView.visibility = View.GONE
                scaleIn(toView, duration / 2)
            }
        })

        hideAnimator.start()
    }
}
