plugins {
    alias(libs.plugins.kotlin.jvm)
    application
    id("com.google.protobuf") version "0.9.4"
}

repositories {
    mavenCentral()
}

val grpcVersion = "1.58.0"
val grpcKotlinVersion = "1.4.0"
val protobufVersion = "3.24.4"

dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation(libs.guava)
    
    // gRPC и Protobuf
    implementation("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("com.google.protobuf:protobuf-kotlin:$protobufVersion")
    implementation("com.google.protobuf:protobuf-java:$protobufVersion") // Основная зависимость
    runtimeOnly("io.grpc:grpc-netty:$grpcVersion")
    
    // Для google.protobuf.Struct используем основной protobuf-java
    // protobuf-struct не существует как отдельный артефакт
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

application {
    mainClass = "org.example.AppKt"
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:$grpcKotlinVersion:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
                create("grpckt")
            }
            it.builtins {
                create("kotlin")
            }
        }
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}