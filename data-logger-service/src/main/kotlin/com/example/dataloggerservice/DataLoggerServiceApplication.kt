package com.example.dataloggerservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DataLoggerServiceApplication

fun main(args: Array<String>) {
    runApplication<DataLoggerServiceApplication>(*args)
}
