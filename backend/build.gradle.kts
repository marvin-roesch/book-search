import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.4.0"
    id("com.github.johnrengelman.shadow") version "5.0.0"
}

group = "io.paleocrafter"
version = "1.0.0"

repositories {
    mavenCentral()
    jcenter()
    maven {
        url = uri("https://github.com/psiegman/mvn-repo/raw/master/releases")
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.ktor:ktor-server-core:1.4.0")
    implementation("io.ktor:ktor-server-netty:1.4.0")
    implementation("io.ktor:ktor-jackson:1.4.0")
    implementation("io.ktor:ktor-auth:1.4.0")
    implementation("io.ktor:ktor-server-sessions:1.4.0")
    implementation("at.favre.lib:bcrypt:0.9.0")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("org.postgresql:postgresql:42.2.5")
    implementation("com.zaxxer:HikariCP:2.7.8")
    implementation("org.jetbrains.exposed:exposed:0.15.1")
    implementation("org.elasticsearch.client:elasticsearch-rest-high-level-client:7.1.1")
    implementation("nl.siegmann.epublib:epublib-core:3.1") {
        exclude(group = "org.slf4j", module = "slf4j-simple")
    }
    implementation("org.jsoup:jsoup:1.12.1")
    implementation("org.apache.logging.log4j:log4j-to-slf4j:2.11.2")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "14"
    kotlinOptions.freeCompilerArgs = listOf(
        "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi",
        "-Xuse-experimental=kotlinx.coroutines.ObsoleteCoroutinesApi",
        "-Xuse-experimental=io.ktor.util.KtorExperimentalAPI"
    )
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

val run: JavaExec by tasks
run.setEnvironment(
    "ELASTIC_HOST" to "http://127.0.0.1:9200",
    "DEFAULT_PASSWORD" to "booksearcher",
    "CRYPTO_KEY" to "966efc7f2526829ec7580bfb033652555adcc2bd8fb2a250aaa0ced12bbff00b7b60dd0e323f129171a27d66660af93b258eab90507ff28895fb79b459ced1f2",
    "DB_CONNECTION" to "jdbc:postgresql://127.0.0.1:5432/booksearch",
    "DB_DRIVER" to "org.postgresql.Driver",
    "DB_USERNAME" to "booksearch",
    "DB_PASSWORD" to "b00kse4rch",
    "DB_MIGRATION_USERNAME" to "booksearch_migrations",
    "DB_MIGRATION_PASSWORD" to "migr4tion5"
)
