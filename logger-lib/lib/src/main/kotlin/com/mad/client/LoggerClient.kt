package com.mad.client

import com.mad.model.ActivityLogEntry
import com.mad.model.ErrorLogEntry
import com.mad.model.LogLevel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

/**
 * Клиент для отправки логов в сервис логирования через Redis
 */
class LoggerClient(
    host: String = "localhost",
    port: Int = 6379,
    password: String = "",
    private val activityChannel: String = "logger:activity",
    private val errorChannel: String = "logger:error"
) {
    private val jedisPool = JedisPool(JedisPoolConfig(), host, port)
    private val json = Json { ignoreUnknownKeys = true }
    
    // Хранение последних действий для включения в логи ошибок
    private val recentActivities = mutableListOf<ActivityLogEntry>()
    private val maxStoredActivities = 10
    
    fun logActivity(
        event: String,
        userId: String? = null,
        deviceModel: String? = null,
        level: LogLevel = LogLevel.INFO,
        additionalData: Map<String, String> = emptyMap()
    ) {
        val logEntry = ActivityLogEntry(
            timestamp = System.currentTimeMillis(),
            userId = userId,
            deviceModel = deviceModel,
            event = event,
            level = level,
            additionalData = additionalData
        )
        
        // Сохраняем активность для возможного включения в лог ошибки
        synchronized(recentActivities) {
            recentActivities.add(logEntry)
            if (recentActivities.size > maxStoredActivities) {
                recentActivities.removeAt(0)
            }
        }
        
        publishToRedis(activityChannel, json.encodeToString(logEntry))
    }
    
    fun logError(
        event: String,
        errorMessage: String,
        userId: String? = null,
        deviceModel: String? = null,
        stackTrace: String? = null,
        level: LogLevel = LogLevel.ERROR
    ) {
        // Получаем копию последних активностей
        val previousActivities = synchronized(recentActivities) {
            recentActivities.toList()
        }
        
        val logEntry = ErrorLogEntry(
            timestamp = System.currentTimeMillis(),
            userId = userId,
            deviceModel = deviceModel,
            event = event,
            errorMessage = errorMessage,
            stackTrace = stackTrace,
            previousActivities = previousActivities,
            level = level
        )
        
        publishToRedis(errorChannel, json.encodeToString(logEntry))
    }
    
    private fun publishToRedis(channel: String, message: String) {
        try {
            jedisPool.resource.use { jedis: Jedis ->
                jedis.publish(channel, message)
            }
        } catch (e: Exception) {
            System.err.println("Ошибка при отправке лога в Redis: ${e.message}")
        }
    }
    
    fun close() {
        jedisPool.close()
    }
}
