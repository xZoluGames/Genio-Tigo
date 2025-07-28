package com.example.geniotecni.tigo.events

sealed interface AppEvent {
    /**
     * Timestamp cuando se creó el evento
     */
    val timestamp: Long get() = System.currentTimeMillis()

    /**
     * Identificador único del evento
     */
    val eventId: String get() = "${this::class.simpleName}_${timestamp}_${hashCode()}"
}