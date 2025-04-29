package com.example.datalogger.model

import java.time.Instant

/**
 * Базовый интерфейс для всех событий логирования
 */
interface LogEvent {
    val timestamp: Instant
    val userId: String?
    val deviceModel: String?
    val eventType: String
}

/**
 * Событие пользовательской активности
 */
data class ActivityLogEvent(
    override val timestamp: Instant = Instant.now(),
    override val userId: String?,
    override val deviceModel: String?,
    override val eventType: String,
    val details: Map<String, Any?> = emptyMap()
) : LogEvent

/**
 * Событие ошибки
 */
data class ErrorLogEvent(
    override val timestamp: Instant = Instant.now(),
    override val userId: String?,
    override val deviceModel: String?,
    override val eventType: String,
    val errorMessage: String,
    val stackTrace: String? = null,
    val previousActivities: List<ActivityLogEvent> = emptyList()
) : LogEvent
