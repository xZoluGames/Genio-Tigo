package com.example.geniotecni.tigo.ui.adapters

import java.io.Serializable

data class ServiceItem(
    val name: String,
    val description: String,
    val icon: Int,
    val colorResId: Int,
    val serviceType: Int
) : Serializable