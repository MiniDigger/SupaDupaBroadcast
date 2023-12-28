import java.io.ByteArrayOutputStream

plugins {
    `java-library`
    id("io.papermc.hangar-publish-plugin") version "0.1.1"
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/groups/public")
}

dependencies {
    api("org.spigotmc:spigot-api:1.9-R0.1-SNAPSHOT")
}

group = "me.MiniDigger"
version = "1.1-SNAPSHOT-${latestCommitHash()}"
description = "SupaDupaBroadcast"
java.sourceCompatibility = JavaVersion.VERSION_1_8

fun latestCommitHash(): String {
    val byteOut = ByteArrayOutputStream()
    exec {
        commandLine = listOf("git", "rev-parse", "--short", "HEAD")
        standardOutput = byteOut
    }
    return byteOut.toString(Charsets.UTF_8.name()).trim()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

tasks {
    processResources {
        filesMatching(listOf("plugin.yml")) {
            expand("version" to project.version)
        }
    }
}

hangarPublish {
    publications.register("plugin") {
        version = project.version as String
        id = "SupaDupaBroadcast"
        channel = if ((project.version as String).contains("SNAPSHOT")) "Snapshot" else "Release"
        apiKey = System.getenv("HANGAR_API_TOKEN")

        platforms {
            paper {
                jar = tasks.jar.flatMap { it.archiveFile }
                platformVersions = listOf("1.9.0-1.20.4")
            }
        }
    }
}
