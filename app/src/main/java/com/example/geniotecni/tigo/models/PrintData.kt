package com.example.geniotecni.tigo.models

data class PrintData(
    val service: String,
    val date: String,
    val time: String,
    val message: String,
    val referenceData: ReferenceData
)

data class ReferenceData(
    val ref1: String, 
    val ref2: String
)

data class ServiceConfig(
    val showPhone: Boolean = false,
    val showCedula: Boolean = false,
    val showAmount: Boolean = false,
    val showConsulta: Boolean = false,
    val showNacimiento: Boolean = false,
    val cedulaHint: String = "Ingrese el numero de cedula",
    val amountHint: String = "Ingrese el monto",
    val phoneHint: String = "Ingrese el numero de telefono",
    val consultaHint: String = "Ingrese el numero de consulta",
    val nacimientoHint: String = "Ingrese la fecha de nacimiento",
    val minAmount: Long = 0L,
    val maxAmount: Long = Long.MAX_VALUE
)