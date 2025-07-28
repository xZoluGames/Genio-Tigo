package com.example.geniotecni.tigo.events

data class SettingChanged(
    val settingKey: String,
    val oldValue: Any?,
    val newValue: Any?
) : SystemEvent
