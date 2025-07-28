package com.example.geniotecni.tigo.events

data class NetworkStateChanged(
    val connected: Boolean,
    val connectionType: String?
) : SystemEvent
