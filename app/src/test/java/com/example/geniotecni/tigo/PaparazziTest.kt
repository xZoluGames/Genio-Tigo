package com.example.geniotecni.tigo

import android.widget.TextView
import app.cash.paparazzi.DeviceConfig.Companion.PIXEL_5
import app.cash.paparazzi.Paparazzi
import com.google.android.material.button.MaterialButton
import org.junit.Rule
import org.junit.Test

class PaparazziTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = PIXEL_5,
        theme = "Theme.MaterialComponents.DayNight.NoActionBar"
    )

    @Test
    fun launchSimpleView() {
        val textView = TextView(paparazzi.context).apply {
            text = "Genio Tecni App - Paparazzi Test"
            textSize = 20f
            setPadding(32, 32, 32, 32)
        }
        
        paparazzi.snapshot(textView)
    }

    @Test
    fun launchMaterialButton() {
        val button = MaterialButton(paparazzi.context).apply {
            text = "Test Button"
            setPadding(48, 24, 48, 24)
        }
        
        paparazzi.snapshot(button)
    }
}