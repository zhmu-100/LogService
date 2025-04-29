package com.example.dataloggerservice.controller

import com.example.datalogger.logger.ErrorLogger
import com.example.datalogger.logger.LogRepository
import com.example.datalogger.model.ErrorLogEvent
import com.example.dataloggerservice.dto.ErrorLogRequest
import com.example.dataloggerservice.dto.LogSearchRequest
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController
@RequestMapping("/api/logs/error")
class ErrorLogController(
    private val errorLogger: ErrorLogger,
    private val logRepository: LogRepository
) {
    
    @PostMapping
    fun logError(@Valid @RequestBody request: ErrorLogRequest): ResponseEntity<ErrorLogEvent> = runBlocking {
        val event = errorLogger.logError(
            userId = request.userId,
            eventType = request.eventType,
            errorMessage = request.errorMessage,
            deviceModel = request.deviceModel,
            stackTrace = request.stackTrace,
            previousActivities = request.previousActivities ?: emptyList()
        )
        
        ResponseEntity.ok(event)
    }
    
    @PostMapping("/search")
    fun searchErrorLogs(@Valid @RequestBody request: LogSearchRequest): ResponseEntity<List<ErrorLogEvent>> = runBlocking {
        val logs = logRepository.findErrorLogs(
            userId = request.userId,
            eventType = request.eventType,
            fromTimestamp = request.fromTimestamp,
            toTimestamp = request.toTimestamp,
            limit = request.limit ?: 100,
            offset = request.offset ?: 0
        )
        
        ResponseEntity.ok(logs)
    }
}
