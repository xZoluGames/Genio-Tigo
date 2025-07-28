package com.example.geniotecni.tigo.events
data class ErrorDisplayRequested(
    val message: String,
    val severity: ErrorSeverity = ErrorSeverity.ERROR,
    val actionRequired: Boolean = false
) : UIEvent
