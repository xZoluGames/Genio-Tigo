package com.example.geniotecni.tigo.ui.adapters

import java.io.Serializable

// Service Item data class
data class ServiceItem(
    val id: Int,
    val name: String,
    val description: String,
    val icon: Int,
    val color: Int,
    val isActive: Boolean = true
) : Serializable