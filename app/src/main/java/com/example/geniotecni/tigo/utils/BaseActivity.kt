package com.example.geniotecni.tigo.utils

import androidx.appcompat.app.AppCompatActivity

/**
 * Clase base para actividades
 */
abstract class BaseActivity : AppCompatActivity() {
    
    protected abstract val tag: String
    
    protected open val requiresPermissions: Array<String> = emptyArray()
}