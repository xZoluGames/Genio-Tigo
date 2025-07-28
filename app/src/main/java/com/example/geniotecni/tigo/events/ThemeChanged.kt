package com.example.geniotecni.tigo.events

data class ThemeChanged(
    val newTheme: Int,
    val previousTheme: Int
) : UIEvent

