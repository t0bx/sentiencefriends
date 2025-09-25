import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.Exec

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

val shadowJarTask = tasks.named<ShadowJar>("shadowJar")

val remoteUser: String? = (findProperty("remoteUser") as String?).takeIf { !it.isNullOrBlank() } ?: System.getenv("REMOTE_USER")
val remoteHost: String? = (findProperty("remoteHost") as String?).takeIf { !it.isNullOrBlank() } ?: System.getenv("REMOTE_HOST")
val remotePathLobby: String? = (findProperty("remotePathLobby") as String?).takeIf { !it.isNullOrBlank() } ?: System.getenv("REMOTE_PATH_LOBBY")
val remoteIdentity: String? = (findProperty("remoteIdentity") as String?).takeIf { !it.isNullOrBlank() } ?: System.getenv("REMOTE_IDENTITY")

fun scpArgsFor(identity: String?) = mutableListOf("scp", "-C", "-o", "StrictHostKeyChecking=no").apply {
    if (!identity.isNullOrBlank()) addAll(listOf("-i", identity))
}

val deployLobby by tasks.registering(Exec::class) {
    dependsOn(shadowJarTask)

    doFirst {
        if (remoteUser == null || remoteHost == null || remotePathLobby == null) {
            throw GradleException("Missing remote config for deployLobby: remoteUser, remoteHost and remotePathLobby are required.")
        }

        val jarFile = shadowJarTask.get().archiveFile.get().asFile
        if (!jarFile.exists()) throw GradleException("shadowJar output not found: ${jarFile.absolutePath}")

        val scpArgs = scpArgsFor(remoteIdentity)
        scpArgs += listOf(jarFile.absolutePath, "${remoteUser}@${remoteHost}:${remotePathLobby}")

        logger.lifecycle("Uploading ${jarFile.name} -> ${remoteUser}@${remoteHost}:${remotePathLobby}")
        commandLine = scpArgs
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("SentienceFriends-Lobby")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
    archiveExtension.set("jar")

    finalizedBy(deployLobby)
}