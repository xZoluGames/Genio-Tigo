package com.example.geniotecni.tigo.events

data class BluetoothDeviceDiscovered(
    val deviceName: String,
    val deviceAddress: String,
    val isPaired: Boolean
) : BluetoothEvent
