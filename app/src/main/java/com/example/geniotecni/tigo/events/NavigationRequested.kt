package com.example.geniotecni.tigo.events

data class NavigationRequested(
    val fromScreen: String,
    val toScreen: String,
    val data: Map<String, Any> = emptyMap()
) : UIEvent

