package com.example.geniotecni.tigo.events

data class BluetoothDeviceConnected(
    val deviceName: String,
    val deviceAddress: String,
    val connectionType: String = "RFCOMM"
) : BluetoothEvent
