package com.example.dataloggerservice.controller

import com.example.datalogger.logger.LogRepository
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/logs/analytics")
class LogAnalyticsController(
    private val logRepository: LogRepository
) {
    
    @GetMapping("/event-types")
    fun getEventTypes(): ResponseEntity<Set<String>> = runBlocking {
        val eventTypes = logRepository.getEventTypes()
        ResponseEntity.ok(eventTypes)
    }
}
