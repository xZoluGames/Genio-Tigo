package com.example.geniotecni.tigo.utils

import android.content.Context
import android.view.View
import android.widget.Toast
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

// View extensions
fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.INVISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

// Context extensions
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

// String extensions
fun String.toFormattedAmount(): String {
    return try {
        val number = this.replace(",", "").replace(".", "").toLong()
        DecimalFormat("#,###").format(number)
    } catch (e: Exception) {
        this
    }
}

fun String.cleanPhone(): String {
    return this.replace(Regex("[^0-9]"), "")
}

// Date extensions
fun Date.toFormattedString(pattern: String = Constants.DATE_FORMAT): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(this)
}

fun Long.toFormattedDate(pattern: String = Constants.DATE_FORMAT): String {
    return Date(this).toFormattedString(pattern)
}