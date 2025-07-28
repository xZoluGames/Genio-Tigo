package com.example.geniotecni.tigo.events

data class PermissionGranted(
    val permission: String,
    val granted: Boolean
) : SystemEvent
