package com.mad.logger.config

import java.io.File
import java.util.*

data class AppConfig (
    val redis: RedisConfig = RedisConfig(),
    val logger: LoggerConfig = LoggerConfig(),
    val api: ApiConfig = ApiConfig()
) {
    companion object {
        fun load(): AppConfig {
            val properties = Properties()
            val configFile = File("application.properties")
            
            if (configFile.exists()) {
                configFile.inputStream().use { properties.load(it) }
            }
            
            return AppConfig(
                redis = RedisConfig(
                    host = properties.getProperty("redis.host", "localhost"),
                    port = properties.getProperty("redis.port", "6379").toInt(),
                    password = properties.getProperty("redis.password", ""),
                    activityChannel = properties.getProperty("redis.channel.activity", "logger:activity"),
                    errorChannel = properties.getProperty("redis.channel.error", "logger:error")
                ),
                logger = LoggerConfig(
                    logToFile = properties.getProperty("logger.file.enabled", "true").toBoolean(),
                    logFilePath = properties.getProperty("logger.file.path", "logs"),
                    rotationPeriod = properties.getProperty("logger.file.rotation", "daily"),
                    consoleLogLevel = properties.getProperty("logger.console.level", "INFO")
                ),
                api = ApiConfig(
                    host = properties.getProperty("api.host", "0.0.0.0"),
                    port = properties.getProperty("api.port", "8095").toInt()
                )
            )
        }
    }
}

data class RedisConfig(
    val host: String = "localhost",
    val port: Int = 6379,
    val password: String = "",
    val activityChannel: String = "logger:activity",
    val errorChannel: String = "logger:error"
)

data class LoggerConfig(
    val logToFile: Boolean = true,
    val logFilePath: String = "logs",
    val rotationPeriod: String = "daily", // daily, hourly, etc.
    val consoleLogLevel: String = "INFO"
)

data class ApiConfig(
    val host: String = "0.0.0.0",
    val port: Int = 8095
)
