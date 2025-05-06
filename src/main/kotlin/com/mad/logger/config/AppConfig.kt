package com.mad.logger.config

import io.github.cdimascio.dotenv.dotenv

data class AppConfig(val redis: RedisConfig, val logger: LoggerConfig, val api: ApiConfig) {
  companion object {
    fun load(): AppConfig {
      val dotenv = dotenv { ignoreIfMissing = false }

      return AppConfig(
          redis =
              RedisConfig(
                  host = dotenv["REDIS_HOST"] ?: "localhost",
                  port = dotenv["REDIS_PORT"]?.toIntOrNull() ?: 6379,
                  password = dotenv["REDIS_PASSWORD"] ?: "",
                  activityChannel = dotenv["REDIS_CHANNEL_ACTIVITY"] ?: "logger:activity",
                  errorChannel = dotenv["REDIS_CHANNEL_ERROR"] ?: "logger:error"),
          logger =
              LoggerConfig(
                  logToFile = dotenv["LOGGER_FILE_ENABLED"]?.toBoolean() ?: true,
                  logFilePath = dotenv["LOGGER_FILE_PATH"] ?: "logs",
                  rotationPeriod = dotenv["LOGGER_FILE_ROTATION"] ?: "daily",
                  consoleLogLevel = dotenv["LOGGER_CONSOLE_LEVEL"] ?: "INFO"),
          api =
              ApiConfig(
                  host = dotenv["API_HOST"] ?: "0.0.0.0",
                  port = dotenv["API_PORT"]?.toIntOrNull() ?: 8095))
    }
  }
}

data class RedisConfig(
    val host: String,
    val port: Int,
    val password: String,
    val activityChannel: String,
    val errorChannel: String
)

data class LoggerConfig(
    val logToFile: Boolean,
    val logFilePath: String,
    val rotationPeriod: String,
    val consoleLogLevel: String
)

data class ApiConfig(val host: String, val port: Int)
