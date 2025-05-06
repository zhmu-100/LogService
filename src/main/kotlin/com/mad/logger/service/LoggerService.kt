package com.mad.logger.service

import com.mad.logger.config.LoggerConfig
import com.mad.logger.model.ActivityLogEntry
import com.mad.logger.model.ErrorLogEntry
import com.mad.logger.model.LogLevel
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import mu.KotlinLogging

class LoggerService(private val redisService: RedisService, private val config: LoggerConfig) {
  private val logger = KotlinLogging.logger {}
  private var processingJob: Job? = null

  // Хранение последних действий пользователя для включения в логи ошибок
  private val userActivities = mutableMapOf<String, MutableList<ActivityLogEntry>>()
  private val maxStoredActivities = 10 // Максимальное количество хранимых действий на пользователя

  fun startProcessing() {
    if (processingJob != null) return

    processingJob =
        CoroutineScope(Dispatchers.IO).launch {
          launch {
            redisService.activityLogFlow.collect { logEntry -> processActivityLog(logEntry) }
          }

          launch { redisService.errorLogFlow.collect { logEntry -> processErrorLog(logEntry) } }
        }

    logger.info { "Запущена обработка логов" }
  }

  fun stopProcessing() {
    processingJob?.cancel()
    processingJob = null
    logger.info { "Остановлена обработка логов" }
  }

  private fun processActivityLog(logEntry: ActivityLogEntry) {
    if (shouldLogToConsole(logEntry.level)) {
      logger.info {
        "Активность: ${logEntry.event} | Пользователь: ${logEntry.userId ?: "неизвестен"} | Устройство: ${logEntry.deviceModel ?: "неизвестно"}"
      }
    }

    // Сохранение в файл, если включено
    if (config.logToFile) {
      writeToFile("activity", logEntry.toString())
    }

    // Сохранение активности пользователя для возможного включения в лог ошибки
    logEntry.userId?.let { userId ->
      val activities = userActivities.getOrPut(userId) { mutableListOf() }
      activities.add(logEntry)

      // Ограничение количества хранимых активностей
      if (activities.size > maxStoredActivities) {
        activities.removeAt(0)
      }
    }
  }

  private fun processErrorLog(logEntry: ErrorLogEntry) {
    // Вывод в консоль (ошибки всегда выводятся)
    logger.error {
      "Ошибка: ${logEntry.event} | Сообщение: ${logEntry.errorMessage} | " +
          "Пользователь: ${logEntry.userId ?: "неизвестен"} | Устройство: ${logEntry.deviceModel ?: "неизвестно"}"
    }

    // Сохранение в файл, если включено
    if (config.logToFile) {
      writeToFile("error", logEntry.toString())
    }
  }

  private fun shouldLogToConsole(level: LogLevel): Boolean {
    val configLevel =
        try {
          LogLevel.valueOf(config.consoleLogLevel)
        } catch (e: Exception) {
          LogLevel.INFO
        }

    return level.ordinal >= configLevel.ordinal
  }

  private fun writeToFile(type: String, content: String) {
    try {
      val now = LocalDateTime.now()
      val dateStr =
          when (config.rotationPeriod.lowercase()) {
            "hourly" -> now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"))
            "daily" -> now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            "monthly" -> now.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            else -> now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
          }

      val directory = File(config.logFilePath)
      if (!directory.exists()) {
        directory.mkdirs()
      }

      val file = File(directory, "${type}_${dateStr}.log")
      file.appendText("${now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)} - $content\n")
    } catch (e: Exception) {
      logger.error(e) { "Ошибка при записи лога в файл" }
    }
  }

  fun logActivity(
      event: String,
      userId: String? = null,
      deviceModel: String? = null,
      level: LogLevel = LogLevel.INFO,
      additionalData: Map<String, String> = emptyMap()
  ) {
    val logEntry =
        ActivityLogEntry(
            timestamp = System.currentTimeMillis(),
            userId = userId,
            deviceModel = deviceModel,
            event = event,
            level = level,
            additionalData = additionalData)

    redisService.publishActivityLog(logEntry)
  }

  fun logError(
      event: String,
      errorMessage: String,
      userId: String? = null,
      deviceModel: String? = null,
      stackTrace: String? = null,
      level: LogLevel = LogLevel.ERROR
  ) {
    // Получение предыдущих активностей пользователя, если доступны
    val previousActivities =
        if (userId != null) {
          userActivities[userId]?.toList() ?: emptyList()
        } else {
          emptyList()
        }

    val logEntry =
        ErrorLogEntry(
            timestamp = System.currentTimeMillis(),
            userId = userId,
            deviceModel = deviceModel,
            event = event,
            errorMessage = errorMessage,
            stackTrace = stackTrace,
            previousActivities = previousActivities,
            level = level)

    redisService.publishErrorLog(logEntry)
  }

  fun getActivityLogs(
      limit: Int = 100,
      offset: Int = 0,
      userId: String? = null
  ): List<ActivityLogEntry> {
    return redisService.getActivityLogs(limit, offset, userId)
  }

  fun getErrorLogs(limit: Int = 100, offset: Int = 0, userId: String? = null): List<ErrorLogEntry> {
    return redisService.getErrorLogs(limit, offset, userId)
  }
}
