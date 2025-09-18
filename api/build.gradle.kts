plugins {
    id("java")
}

group = "de.t0bx.sentiencefriends.api"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")

    // https://mvnrepository.com/artifact/io.netty/netty-all
    implementation("io.netty:netty-all:4.2.3.Final")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}