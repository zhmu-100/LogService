 
plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
    kotlin("plugin.serialization") version "1.9.0"
    `java-library`
}
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "com.mad"
            artifactId = "my-kotlin-library"
            version = "1.0.0"
        }
    }
    repositories {
        mavenLocal()
    }
}
repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // Use JUnit Jupiter for testing.
    testImplementation(libs.junit.jupiter)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("redis.clients:jedis:4.4.3")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // This dependency is exported to consumers, that is to say found on their compile classpath.
    api(libs.commons.math3)

    // This dependency is used internally, and not exposed to consumers on their own compile classpath.
    implementation(libs.guava)
}
// Apply a specific Java toolchain to ease working on different environments.

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}
tasks.withType<Jar> {
  manifest {
    attributes["Implementation-Title"] = "My Kotlin Library"
    attributes["Implementation-Version"] = "1.0.0"
  }
}
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
