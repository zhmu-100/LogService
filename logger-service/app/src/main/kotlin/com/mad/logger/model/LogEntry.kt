package com.mad.logger.model

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
sealed class LogEntry {
    abstract val timestamp: Long
    abstract val userId: String?
    abstract val deviceModel: String?
    abstract val event: String
    abstract val level: LogLevel
    
    fun getFormattedTimestamp(): String {
        return Instant.ofEpochMilli(timestamp).toString()
    }
}

@Serializable
data class ActivityLogEntry(
    override val timestamp: Long = System.currentTimeMillis(),
    override val userId: String? = null,
    override val deviceModel: String? = null,
    override val event: String,
    override val level: LogLevel = LogLevel.INFO,
    val additionalData: Map<String, String> = emptyMap()
) : LogEntry()

@Serializable
data class ErrorLogEntry(
    override val timestamp: Long = System.currentTimeMillis(),
    override val userId: String? = null,
    override val deviceModel: String? = null,
    override val event: String,
    override val level: LogLevel = LogLevel.ERROR,
    val errorMessage: String,
    val stackTrace: String? = null,
    val previousActivities: List<ActivityLogEntry> = emptyList()
) : LogEntry()

@Serializable
enum class LogLevel {
    DEBUG, INFO, WARN, ERROR, FATAL
}
