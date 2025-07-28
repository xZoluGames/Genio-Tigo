package com.example.geniotecni.tigo.events

data class ServiceSelected(
    val serviceId: String,
    val serviceName: String,
    val fromScreen: String
) : UIEvent
