package com.example.dataloggerservice.controller

import com.example.datalogger.logger.ActivityLogger
import com.example.datalogger.logger.LogRepository
import com.example.datalogger.model.ActivityLogEvent
import com.example.dataloggerservice.dto.ActivityLogRequest
import com.example.dataloggerservice.dto.LogSearchRequest
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController
@RequestMapping("/api/logs/activity")
class ActivityLogController(
    private val activityLogger: ActivityLogger,
    private val logRepository: LogRepository
) {
    
    @PostMapping
    fun logActivity(@Valid @RequestBody request: ActivityLogRequest): ResponseEntity<ActivityLogEvent> = runBlocking {
        val event = activityLogger.logActivity(
            userId = request.userId,
            eventType = request.eventType,
            deviceModel = request.deviceModel,
            details = request.details ?: emptyMap()
        )
        
        ResponseEntity.ok(event)
    }
    
    @GetMapping("/user/{userId}")
    fun getRecentUserActivities(
        @PathVariable userId: String,
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<List<ActivityLogEvent>> = runBlocking {
        val activities = activityLogger.getRecentActivities(userId, limit)
        ResponseEntity.ok(activities)
    }
    
    @PostMapping("/search")
    fun searchActivityLogs(@Valid @RequestBody request: LogSearchRequest): ResponseEntity<List<ActivityLogEvent>> = runBlocking {
        val logs = logRepository.findActivityLogs(
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
