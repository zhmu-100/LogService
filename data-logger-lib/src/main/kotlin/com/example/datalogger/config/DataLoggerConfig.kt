package com.example.datalogger.config

import com.example.datalogger.logger.ActivityLogger
import com.example.datalogger.logger.ErrorLogger
import com.example.datalogger.logger.LogRepository
import com.example.datalogger.redis.RedisLoggerImpl
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI

/**
 * Конфигурация для библиотеки логирования
 */
class DataLoggerConfig(
    private val redisHost: String = "localhost",
    private val redisPort: Int = 6379,
    private val redisPassword: String? = null,
    private val activityLogChannel: String = "logs:activity",
    private val errorLogChannel: String = "logs:error",
    private val activityLogKey: String = "logs:activity:",
    private val errorLogKey: String = "logs:error:",
    private val eventTypesKey: String = "logs:event-types"
) {
    /**
     * Создает и настраивает ObjectMapper для сериализации/десериализации JSON
     */
    fun objectMapper(): ObjectMapper {
        return ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
    
    /**
     * Создает клиент Redis
     */
    fun redisClient(): RedisClient {
        val redisUri = RedisURI.builder()
            .withHost(redisHost)
            .withPort(redisPort)
            .apply { if (redisPassword != null) withPassword(redisPassword.toCharArray()) }
            .build()
            
        return RedisClient.create(redisUri)
    }
    
    /**
     * Создает экземпляр логгера
     */
    fun createLogger(): RedisLoggerImpl {
        return RedisLoggerImpl(
            redisClient = redisClient(),
            objectMapper = objectMapper(),
            activityLogChannel = activityLogChannel,
            errorLogChannel = errorLogChannel,
            activityLogKey = activityLogKey,
            errorLogKey = errorLogKey,
            eventTypesKey = eventTypesKey
        )
    }
}
