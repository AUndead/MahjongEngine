plugins {
    java
    jacoco
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
}

group = "doublemoon.mahjongcraft"
version = "0.1.0-SNAPSHOT"

val kotlinRuntimeVersion = "2.2.0"
val kotlinSerializationVersion = "1.9.0"
val packetEventsVersion = "2.11.2"
val mahjongUtilsVersion = "0.7.7"
val mariadbVersion = "3.5.3"
val h2Version = "2.3.232"
val hikariVersion = "6.3.0"
val adventureVersion = "4.17.0"
val junitVersion = "5.12.2"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("com.github.retrooper:packetevents-spigot:$packetEventsVersion")
    implementation("io.github.ssttkkl:mahjong-utils:$mahjongUtilsVersion")
    implementation("org.mariadb.jdbc:mariadb-java-client:$mariadbVersion")
    implementation("com.h2database:h2:$h2Version")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinRuntimeVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinSerializationVersion")
    testCompileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("net.kyori:adventure-api:$adventureVersion")
    testImplementation("net.kyori:adventure-text-minimessage:$adventureVersion")
    testImplementation("net.kyori:adventure-text-serializer-plain:$adventureVersion")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    withType<JavaCompile>().configureEach {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
        filesMatching(listOf("plugin.yml", "paper-plugin.yml")) {
            expand(
                "version" to project.version,
                "mahjongUtilsVersion" to mahjongUtilsVersion,
                "mariadbVersion" to mariadbVersion,
                "h2Version" to h2Version,
                "hikariVersion" to hikariVersion,
                "kotlinRuntimeVersion" to kotlinRuntimeVersion,
                "kotlinSerializationVersion" to kotlinSerializationVersion
            )
        }
    }

    test {
        useJUnitPlatform()
        finalizedBy(jacocoTestReport)
    }

    jacocoTestReport {
        dependsOn(test)
        reports {
            xml.required.set(true)
            html.required.set(true)
            csv.required.set(false)
        }
    }

    check {
        dependsOn(jacocoTestReport)
    }
}
