Расположение .env файла - в папке app (там же, где и build.gradle.kts)

.env файл:

# Redis Config
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_CHANNEL_ACTIVITY=logger:activity
REDIS_CHANNEL_ERROR=logger:error

# Logger Config
LOGGER_FILE_ENABLED=true
LOGGER_FILE_PATH=logs
LOGGER_FILE_ROTATION=daily
LOGGER_CONSOLE_LEVEL=INFO

# API Config
API_HOST=0.0.0.0
API_PORT=8095
