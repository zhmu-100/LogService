package com.example.dataloggerservice.config

import com.example.datalogger.config.DataLoggerConfig
import com.example.datalogger.logger.ActivityLogger
import com.example.datalogger.logger.ErrorLogger
import com.example.datalogger.logger.LogRepository
import com.example.datalogger.redis.RedisLoggerImpl
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RedisConfig(
    @Value("\${spring.redis.host:localhost}") private val redisHost: String,
    @Value("\${spring.redis.port:6379}") private val redisPort: Int,
    @Value("\${spring.redis.password:}") private val redisPassword: String?
) {
    
    @Bean
    fun dataLoggerConfig(): DataLoggerConfig {
        return DataLoggerConfig(
            redisHost = redisHost,
            redisPort = redisPort,
            redisPassword = redisPassword
        )
    }
    
    @Bean
    fun redisLogger(config: DataLoggerConfig): RedisLoggerImpl {
        return config.createLogger()
    }
    
    @Bean
    fun activityLogger(redisLogger: RedisLoggerImpl): ActivityLogger = redisLogger
    
    @Bean
    fun errorLogger(redisLogger: RedisLoggerImpl): ErrorLogger = redisLogger
    
    @Bean
    fun logRepository(redisLogger: RedisLoggerImpl): LogRepository = redisLogger
}
