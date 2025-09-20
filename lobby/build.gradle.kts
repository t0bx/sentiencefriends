plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.8"
}

group = "de.t0bx.sentiencefriends.lobby"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.7-R0.1-SNAPSHOT")
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")

    // https://mvnrepository.com/artifact/io.netty/netty-all
    implementation("io.netty:netty-all:4.2.3.Final")

    implementation(project(":api"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.shadowJar {
    archiveBaseName.set("SentienceFriends-Lobby")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
    archiveExtension.set("jar")
}