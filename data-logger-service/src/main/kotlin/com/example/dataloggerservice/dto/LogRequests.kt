package com.example.dataloggerservice.dto

import com.example.datalogger.model.ActivityLogEvent
import javax.validation.constraints.NotBlank

data class ActivityLogRequest(
    val userId: String?,
    @field:NotBlank(message = "Тип события обязателен")
    val eventType: String,
    val deviceModel: String?,
    val details: Map<String, Any?>?
)

data class ErrorLogRequest(
    val userId: String?,
    @field:NotBlank(message = "Тип события обязателен")
    val eventType: String,
    @field:NotBlank(message = "Сообщение об ошибке обязательно")
    val errorMessage: String,
    val deviceModel: String?,
    val stackTrace: String?,
    val previousActivities: List<ActivityLogEvent>?
)

data class LogSearchRequest(
    val userId: String?,
    val eventType: String?,
    val fromTimestamp: Long?,
    val toTimestamp: Long?,
    val limit: Int?,
    val offset: Int?
)
