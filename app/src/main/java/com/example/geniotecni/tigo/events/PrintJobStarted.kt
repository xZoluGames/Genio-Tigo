package com.example.geniotecni.tigo.events

data class PrintJobStarted(
    val deviceAddress: String,
    val jobId: String,
    val dataSize: Int
) : BluetoothEvent
