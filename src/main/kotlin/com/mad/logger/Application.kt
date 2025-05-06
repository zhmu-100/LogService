package com.mad.logger

import com.mad.logger.api.configureRouting
import com.mad.logger.config.AppConfig
import com.mad.logger.service.LoggerService
import com.mad.logger.service.RedisService
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun main() {
  val config = AppConfig.load()
  logger.info { "Запуск сервиса логирования с конфигурацией: $config" }

  val redisService = RedisService(config.redis)
  val loggerService = LoggerService(redisService, config.logger)

  // Запуск обработчика сообщений из Redis
  loggerService.startProcessing()

  // Запуск API сервера
  embeddedServer(Netty, port = config.api.port, host = config.api.host) {
        install(ContentNegotiation) {
          json(
              Json {
                prettyPrint = true
                isLenient = true
              })
        }
        configureRouting(loggerService)
      }
      .start(wait = true)
}
