package com.example.geniotecni.tigo.events

data class LanguageChanged(
    val newLanguage: String,
    val previousLanguage: String
) : UIEvent
