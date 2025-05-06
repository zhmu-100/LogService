import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.21"
    kotlin("plugin.serialization") version "1.8.21"
    id("com.ncorti.ktfmt.gradle") version "0.11.0"
    application
}

group = "com.mad"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    
    // Redis
    implementation("redis.clients:jedis:4.3.1")
    implementation("io.lettuce:lettuce-core:6.2.3.RELEASE")
    
    // Ktor для минимального API
    implementation("io.ktor:ktor-server-core:2.3.0")
    implementation("io.ktor:ktor-server-netty:2.3.0")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.0")
    
    // Логирование
    implementation("ch.qos.logback:logback-classic:1.4.7")
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    
    // Тестирование
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
    testImplementation("io.mockk:mockk:1.13.5")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}


tasks.jar {
  manifest {
    attributes["Main-Class"] = application.mainClass.get()
  }
  duplicatesStrategy = DuplicatesStrategy.INCLUDE
  from({
    configurations
      .runtimeClasspath
      .get()
      .filter { it.name.endsWith(".jar") }
      .map { zipTree(it) }
  })
}


application {
    mainClass.set("com.mad.logger.ApplicationKt")
}
