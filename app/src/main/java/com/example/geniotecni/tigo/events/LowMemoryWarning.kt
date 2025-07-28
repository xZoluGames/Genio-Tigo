package com.example.geniotecni.tigo.events

data class LowMemoryWarning(
    val availableMemoryMB: Long,
    val usedMemoryMB: Long
) : SystemEvent
