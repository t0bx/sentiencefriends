import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.8"
}

group = "de.t0bx.sentiencefriends.proxy"
version = "1.0.0"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    implementation("com.zaxxer:HikariCP:6.3.0")

    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")

    // https://mvnrepository.com/artifact/mysql/mysql-connector-java
    implementation("mysql:mysql-connector-java:8.0.33")

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
val remoteIdentity: String? = (findProperty("remoteIdentity") as String?).takeIf { !it.isNullOrBlank() } ?: System.getenv("REMOTE_IDENTITY")

fun scpArgsFor(identity: String?) = mutableListOf("scp", "-C", "-o", "StrictHostKeyChecking=no").apply {
    if (!identity.isNullOrBlank()) addAll(listOf("-i", identity))
}

val deployProxy by tasks.registering(Exec::class) {
    dependsOn(shadowJarTask)
    doFirst {
        if (remoteUser == null || remoteHost == null || (findProperty("remotePathProxy") as String?).isNullOrBlank()) {
            throw GradleException("Missing remote config for deployProxy: remoteUser, remoteHost and remotePathProxy are required.")
        }
        val remotePathProxy = findProperty("remotePathProxy") as String
        val jarFile = shadowJarTask.get().archiveFile.get().asFile
        val scpArgs = scpArgsFor(remoteIdentity)
        scpArgs += listOf(jarFile.absolutePath, "${remoteUser}@${remoteHost}:${remotePathProxy}")
        commandLine = scpArgs
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("SentienceFriends-Velocity")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
    archiveExtension.set("jar")
    manifest {
        attributes["Main-Class"] = "de.t0bx.sentiencefriends.proxy.ProxyPlugin"
    }

    finalizedBy(deployProxy)
}