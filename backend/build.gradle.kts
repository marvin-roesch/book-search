import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.3.21"
    id("com.github.johnrengelman.shadow") version "5.0.0"
}

group = "io.paleocrafter"
version = "0.1.0"

repositories {
    mavenCentral()
    jcenter()
    maven {
        url = uri("https://github.com/psiegman/mvn-repo/raw/master/releases")
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.ktor:ktor-server-core:1.2.1")
    implementation("io.ktor:ktor-server-netty:1.2.1")
    implementation("io.ktor:ktor-jackson:1.2.1")
    implementation("com.h2database:h2:1.4.199")
    implementation("org.jetbrains.exposed:exposed:0.14.2")
    implementation("org.elasticsearch.client:elasticsearch-rest-high-level-client:7.1.1")
    implementation("nl.siegmann.epublib:epublib-core:3.1")

}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClassName = "io.ktor.server.netty.EngineMain"
}

tasks.withType<Jar> {
    archiveFileName.set("book-search.jar")
    manifest {
        attributes(
            mapOf(
                "Main-Class" to application.mainClassName
            )
        )
    }
}
