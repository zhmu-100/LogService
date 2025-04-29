package com.example.datalogger.redis

import com.example.datalogger.logger.ActivityLogger
import com.example.datalogger.logger.ErrorLogger
import com.example.datalogger.logger.LogRepository
import com.example.datalogger.model.ActivityLogEvent
import com.example.datalogger.model.ErrorLogEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.lettuce.core.RedisClient
import io.lettuce.core.api.async.RedisAsyncCommands
import kotlinx.coroutines.future.await
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Реализация логгера на основе Redis
 */
class RedisLoggerImpl(
    private val redisClient: RedisClient,
    private val objectMapper: ObjectMapper,
    private val activityLogChannel: String = "logs:activity",
    private val errorLogChannel: String = "logs:error",
    private val activityLogKey: String = "logs:activity:",
    private val errorLogKey: String = "logs:error:",
    private val eventTypesKey: String = "logs:event-types"
) : ActivityLogger, ErrorLogger, LogRepository {

    private val connection = redisClient.connect()
    private val asyncCommands: RedisAsyncCommands<String, String> = connection.async()
    
    // Хранение последних активностей пользователя в памяти
    private val userActivitiesCache = mutableMapOf<String, ConcurrentLinkedQueue<ActivityLogEvent>>()
    
    override suspend fun logActivity(
        userId: String?,
        eventType: String,
        deviceModel: String?,
        details: Map<String, Any?>
    ): ActivityLogEvent {
        val event = ActivityLogEvent(
            timestamp = Instant.now(),
            userId = userId,
            deviceModel = deviceModel,
            eventType = eventType,
            details = details
        )
        
        val eventJson = objectMapper.writeValueAsString(event)
        
        // Публикуем событие в канал Redis
        asyncCommands.publish(activityLogChannel, eventJson).await()
        
        // Сохраняем событие в Redis с временной меткой в качестве score для сортировки
        val score = event.timestamp.toEpochMilli().toDouble()
        asyncCommands.zadd("$activityLogKey${userId ?: "anonymous"}", score, eventJson).await()
        
        // Добавляем тип события в множество всех типов событий
        asyncCommands.sadd(eventTypesKey, eventType).await()
        
        // Кэшируем активность пользователя в памяти
        if (userId != null) {
            userActivitiesCache.getOrPut(userId) { ConcurrentLinkedQueue() }.add(event)
            // Ограничиваем размер кэша
            while (userActivitiesCache[userId]?.size ?: 0 > 20) {
                userActivitiesCache[userId]?.poll()
            }
        }
        
        return event
    }
    
    override suspend fun getRecentActivities(userId: String, limit: Int): List<ActivityLogEvent> {
        // Получаем последние активности из Redis
        val result = asyncCommands.zrevrange("$activityLogKey$userId", 0, limit - 1.toLong()).await()
        
        return result.map { objectMapper.readValue<ActivityLogEvent>(it) }
    }
    
    override suspend fun logError(
        userId: String?,
        eventType: String,
        errorMessage: String,
        deviceModel: String?,
        stackTrace: String?,
        previousActivities: List<ActivityLogEvent>
    ): ErrorLogEvent {
        // Если предыдущие активности не переданы, используем кэш
        val activities = if (previousActivities.isEmpty() && userId != null) {
            userActivitiesCache[userId]?.toList() ?: emptyList()
        } else {
            previousActivities
        }
        
        val event = ErrorLogEvent(
            timestamp = Instant.now(),
            userId = userId,
            deviceModel = deviceModel,
            eventType = eventType,
            errorMessage = errorMessage,
            stackTrace = stackTrace,
            previousActivities = activities
        )
        
        val eventJson = objectMapper.writeValueAsString(event)
        
        // Публикуем событие в канал Redis
        asyncCommands.publish(errorLogChannel, eventJson).await()
        
        // Сохраняем событие в Redis с временной меткой в качестве score для сортировки
        val score = event.timestamp.toEpochMilli().toDouble()
        asyncCommands.zadd("$errorLogKey${userId ?: "anonymous"}", score, eventJson).await()
        
        // Добавляем тип события в множество всех типов событий
        asyncCommands.sadd(eventTypesKey, eventType).await()
        
        return event
    }
    
    override suspend fun findActivityLogs(
        userId: String?,
        eventType: String?,
        fromTimestamp: Long?,
        toTimestamp: Long?,
        limit: Int,
        offset: Int
    ): List<ActivityLogEvent> {
        // Если указан userId, ищем только его логи
        val key = if (userId != null) "$activityLogKey$userId" else activityLogKey + "*"
        
        // Получаем логи из Redis с учетом временного диапазона
        val min = fromTimestamp?.toDouble() ?: Double.NEGATIVE_INFINITY
        val max = toTimestamp?.toDouble() ?: Double.POSITIVE_INFINITY
        
        val result = asyncCommands.zrangebyscore(key, min, max, offset.toLong(), limit.toLong()).await()
        
        // Фильтруем по типу события, если указан
        return result
            .map { objectMapper.readValue<ActivityLogEvent>(it) }
            .filter { eventType == null || it.eventType == eventType }
    }
    
    override suspend fun findErrorLogs(
        userId: String?,
        eventType: String?,
        fromTimestamp: Long?,
        toTimestamp: Long?,
        limit: Int,
        offset: Int
    ): List<ErrorLogEvent> {
        // Если указан userId, ищем только его логи
        val key = if (userId != null) "$errorLogKey$userId" else errorLogKey + "*"
        
        // Получаем логи из Redis с учетом временного диапазона
        val min = fromTimestamp?.toDouble() ?: Double.NEGATIVE_INFINITY
        val max = toTimestamp?.toDouble() ?: Double.POSITIVE_INFINITY
        
        val result = asyncCommands.zrangebyscore(key, min, max, offset.toLong(), limit.toLong()).await()
        
        // Фильтруем по типу события, если указан
        return result
            .map { objectMapper.readValue<ErrorLogEvent>(it) }
            .filter { eventType == null || it.eventType == eventType }
    }
    
    override suspend fun getEventTypes(): Set<String> {
        return asyncCommands.smembers(eventTypesKey).await().toSet()
    }
    
    fun close() {
        connection.close()
    }
}
