package com.mad.logger.service

import com.mad.logger.config.RedisConfig
import com.mad.logger.model.ActivityLogEntry
import com.mad.logger.model.ErrorLogEntry
import com.mad.logger.model.LogEntry
import io.lettuce.core.RedisClient
import io.lettuce.core.pubsub.RedisPubSubAdapter
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

class RedisService(private val config: RedisConfig) {
  private val logger = KotlinLogging.logger {}
  private val json = Json { ignoreUnknownKeys = true }

  private val jedisPool = JedisPool(JedisPoolConfig(), config.host, config.port)
  private val redisClient = RedisClient.create("redis://${config.host}:${config.port}")

  private val _activityLogFlow = MutableSharedFlow<ActivityLogEntry>()
  val activityLogFlow: SharedFlow<ActivityLogEntry> = _activityLogFlow

  private val _errorLogFlow = MutableSharedFlow<ErrorLogEntry>()
  val errorLogFlow: SharedFlow<ErrorLogEntry> = _errorLogFlow

  private val coroutineScope = CoroutineScope(Dispatchers.IO)

  init {
    setupSubscriptions()
  }

  private fun setupSubscriptions() {
    val pubSubConnection: StatefulRedisPubSubConnection<String, String> =
        redisClient.connectPubSub()
    val pubSubListener =
        object : RedisPubSubAdapter<String, String>() {
          override fun message(channel: String, message: String) {
            when (channel) {
              config.activityChannel -> {
                try {
                  val logEntry = json.decodeFromString<ActivityLogEntry>(message)
                  coroutineScope.launch {
                    _activityLogFlow.emit(logEntry)
                    storeLog(logEntry)
                  }
                } catch (e: Exception) {
                  logger.error(e) { "Ошибка при обработке сообщения активности: $message" }
                }
              }
              config.errorChannel -> {
                try {
                  val logEntry = json.decodeFromString<ErrorLogEntry>(message)
                  coroutineScope.launch {
                    _errorLogFlow.emit(logEntry)
                    storeLog(logEntry)
                  }
                } catch (e: Exception) {
                  logger.error(e) { "Ошибка при обработке сообщения ошибки: $message" }
                }
              }
            }
          }
        }

    pubSubConnection.addListener(pubSubListener)
    val pubSubAsyncCommands = pubSubConnection.async()
    pubSubAsyncCommands.subscribe(config.activityChannel, config.errorChannel)

    logger.info {
      "Подписка на каналы Redis настроена: ${config.activityChannel}, ${config.errorChannel}"
    }
  }

  fun publishActivityLog(logEntry: ActivityLogEntry) {
    try {
      jedisPool.resource.use { jedis ->
        val message = json.encodeToString(logEntry)
        jedis.publish(config.activityChannel, message)
        storeLog(logEntry)
      }
    } catch (e: Exception) {
      logger.error(e) { "Ошибка при публикации лога активности" }
    }
  }

  fun publishErrorLog(logEntry: ErrorLogEntry) {
    try {
      jedisPool.resource.use { jedis ->
        val message = json.encodeToString(logEntry)
        jedis.publish(config.errorChannel, message)
        storeLog(logEntry)
      }
    } catch (e: Exception) {
      logger.error(e) { "Ошибка при публикации лога ошибки" }
    }
  }

  private fun storeLog(logEntry: LogEntry) {
    try {
      jedisPool.resource.use { jedis ->
        val key =
            when (logEntry) {
              is ActivityLogEntry -> "activity:${logEntry.timestamp}"
              is ErrorLogEntry -> "error:${logEntry.timestamp}"
            }
        val value = json.encodeToString(logEntry)
        jedis.set(key, value)

        // Добавляем в список по типу лога для удобного поиска
        val listKey =
            when (logEntry) {
              is ActivityLogEntry -> "activity_logs"
              is ErrorLogEntry -> "error_logs"
            }
        jedis.lpush(listKey, key)

        // Если есть userId, добавляем в список логов пользователя
        logEntry.userId?.let { userId ->
          val userListKey = "user:$userId:logs"
          jedis.lpush(userListKey, key)
        }
      }
    } catch (e: Exception) {
      logger.error(e) { "Ошибка при сохранении лога в Redis" }
    }
  }

  fun getActivityLogs(
      limit: Int = 100,
      offset: Int = 0,
      userId: String? = null
  ): List<ActivityLogEntry> {
    return try {
      jedisPool.resource.use { jedis ->
        val keys =
            if (userId != null) {
              getKeysForUser(jedis, userId, "activity")
            } else {
              jedis.lrange("activity_logs", offset.toLong(), (offset + limit - 1).toLong())
            }

        keys.mapNotNull { key ->
          jedis.get(key)?.let { json.decodeFromString<ActivityLogEntry>(it) }
        }
      }
    } catch (e: Exception) {
      logger.error(e) { "Ошибка при получении логов активности" }
      emptyList()
    }
  }

  fun getErrorLogs(limit: Int = 100, offset: Int = 0, userId: String? = null): List<ErrorLogEntry> {
    return try {
      jedisPool.resource.use { jedis ->
        val keys =
            if (userId != null) {
              getKeysForUser(jedis, userId, "error")
            } else {
              jedis.lrange("error_logs", offset.toLong(), (offset + limit - 1).toLong())
            }

        keys.mapNotNull { key -> jedis.get(key)?.let { json.decodeFromString<ErrorLogEntry>(it) } }
      }
    } catch (e: Exception) {
      logger.error(e) { "Ошибка при получении логов ошибок" }
      emptyList()
    }
  }

  private fun getKeysForUser(jedis: Jedis, userId: String, type: String): List<String> {
    val userKeys = jedis.lrange("user:$userId:logs", 0, -1)
    return userKeys.filter { it.startsWith("$type:") }
  }

  fun close() {
    jedisPool.close()
    redisClient.shutdown()
  }
}
