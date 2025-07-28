package com.example.geniotecni.tigo.events

data class BluetoothStateChanged(
    val enabled: Boolean,
    val previousState: Boolean
) : BluetoothEvent
