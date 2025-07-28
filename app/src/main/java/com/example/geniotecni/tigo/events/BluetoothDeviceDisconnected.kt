package com.example.geniotecni.tigo.events

data class BluetoothDeviceDisconnected(
    val deviceName: String,
    val deviceAddress: String,
    val reason: String? = null
) : BluetoothEvent

