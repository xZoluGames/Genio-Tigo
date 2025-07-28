package com.example.geniotecni.tigo.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Clase base para gestores que necesitan SharedPreferences
 */
abstract class BaseManager(
    protected val context: Context,
    protected val tag: String
) {
    
    protected val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("${tag}_prefs", Context.MODE_PRIVATE)
    }
    
    protected val prefs: SharedPreferences get() = sharedPreferences
    
    // Métodos básicos para SharedPreferences
    protected fun savePreference(key: String, value: Any) {
        with(sharedPreferences.edit()) {
            when (value) {
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Float -> putFloat(key, value)
                is Boolean -> putBoolean(key, value)
            }
            apply()
        }
    }
    
    protected fun getString(key: String, defaultValue: String = ""): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }
    
    protected fun getInt(key: String, defaultValue: Int = 0): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }
    
    protected fun getLong(key: String, defaultValue: Long = 0L): Long {
        return sharedPreferences.getLong(key, defaultValue)
    }
    
    protected fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return sharedPreferences.getFloat(key, defaultValue)
    }
    
    protected fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }
    
    protected fun hasPreference(key: String): Boolean {
        return sharedPreferences.contains(key)
    }
    
    protected fun removePreference(key: String) {
        with(sharedPreferences.edit()) {
            remove(key)
            apply()
        }
    }
    
    protected fun getAllKeys(): Set<String> {
        return sharedPreferences.all.keys
    }
    
    protected fun clearAllPreferences() {
        with(sharedPreferences.edit()) {
            clear()
            apply()
        }
    }
}