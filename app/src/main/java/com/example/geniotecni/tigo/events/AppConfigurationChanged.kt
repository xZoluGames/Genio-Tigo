package com.example.geniotecni.tigo.events

data class AppConfigurationChanged(
    val configKey: String,
    val newValue: String
) : SystemEvent
