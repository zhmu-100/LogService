package com.example.datalogger.logger

import com.example.datalogger.model.ActivityLogEvent
import com.example.datalogger.model.ErrorLogEvent
import com.example.datalogger.model.LogEvent

/**
 * Интерфейс для логирования пользовательской активности
 */
interface ActivityLogger {
    /**
     * Логирует событие пользовательской активности
     */
    suspend fun logActivity(
        userId: String?,
        eventType: String,
        deviceModel: String? = null,
        details: Map<String, Any?> = emptyMap()
    ): ActivityLogEvent
    
    /**
     * Получает последние события активности для пользователя
     */
    suspend fun getRecentActivities(userId: String, limit: Int = 10): List<ActivityLogEvent>
}

/**
 * Интерфейс для логирования ошибок
 */
interface ErrorLogger {
    /**
     * Логирует событие ошибки
     */
    suspend fun logError(
        userId: String?,
        eventType: String,
        errorMessage: String,
        deviceModel: String? = null,
        stackTrace: String? = null,
        previousActivities: List<ActivityLogEvent> = emptyList()
    ): ErrorLogEvent
}

/**
 * Интерфейс для поиска и фильтрации логов
 */
interface LogRepository {
    /**
     * Поиск логов активности по фильтрам
     */
    suspend fun findActivityLogs(
        userId: String? = null,
        eventType: String? = null,
        fromTimestamp: Long? = null,
        toTimestamp: Long? = null,
        limit: Int = 100,
        offset: Int = 0
    ): List<ActivityLogEvent>
    
    /**
     * Поиск логов ошибок по фильтрам
     */
    suspend fun findErrorLogs(
        userId: String? = null,
        eventType: String? = null,
        fromTimestamp: Long? = null,
        toTimestamp: Long? = null,
        limit: Int = 100,
        offset: Int = 0
    ): List<ErrorLogEvent>
    
    /**
     * Получение всех типов событий
     */
    suspend fun getEventTypes(): Set<String>
}
