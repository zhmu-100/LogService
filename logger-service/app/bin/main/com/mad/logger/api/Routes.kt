package com.mad.logger.api

import com.mad.logger.config.AppConfig
import com.mad.logger.model.LogLevel
import com.mad.logger.service.LoggerService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun Application.configureRouting(loggerService: LoggerService) {
    routing {
        route("/api/logs") {
            // Получение логов активности
            get("/activity") {
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val userId = call.request.queryParameters["userId"]
                
                val logs = loggerService.getActivityLogs(limit, offset, userId)
                call.respond(logs)
            }
            
            // Получение логов ошибок
            get("/error") {
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val userId = call.request.queryParameters["userId"]
                
                val logs = loggerService.getErrorLogs(limit, offset, userId)
                call.respond(logs)
            }
            
            // Добавление лога активности
            post("/activity") {
                val request = call.receive<ActivityLogRequest>()
                
                loggerService.logActivity(
                    event = request.event,
                    userId = request.userId,
                    deviceModel = request.deviceModel,
                    level = request.level ?: LogLevel.INFO,
                    additionalData = request.additionalData ?: emptyMap()
                )
                
                call.respond(HttpStatusCode.Created)
            }
            
            // Добавление лога ошибки
            post("/error") {
                val request = call.receive<ErrorLogRequest>()
                
                loggerService.logError(
                    event = request.event,
                    errorMessage = request.errorMessage,
                    userId = request.userId,
                    deviceModel = request.deviceModel,
                    stackTrace = request.stackTrace,
                    level = request.level ?: LogLevel.ERROR
                )
                
                call.respond(HttpStatusCode.Created)
            }
        }
        
        // Проверка работоспособности
        get("/health") {
            call.respond(mapOf("status" to "UP"))
        }
    }
}

@Serializable
data class ActivityLogRequest(
    val event: String,
    val userId: String? = null,
    val deviceModel: String? = null,
    val level: LogLevel? = null,
    val additionalData: Map<String, String>? = null
)

@Serializable
data class ErrorLogRequest(
    val event: String,
    val errorMessage: String,
    val userId: String? = null,
    val deviceModel: String? = null,
    val stackTrace: String? = null,
    val level: LogLevel? = null
)
