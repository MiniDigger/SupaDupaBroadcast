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
description = "SupaDupaBroadcast"
java.sourceCompatibility = JavaVersion.VERSION_1_8

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

fun executeGitCommand(vararg command: String): String {
    val byteOut = ByteArrayOutputStream()
    exec {
        commandLine = listOf("git", *command)
        standardOutput = byteOut
    }
    return byteOut.toString(Charsets.UTF_8.name()).trim()
}

fun latestCommitHash(): String {
    return executeGitCommand("rev-parse", "--short", "HEAD")
}

fun latestCommitMessage(): String {
    return executeGitCommand("log", "-1", "--pretty=%B")
}

fun branchName(): String {
    return executeGitCommand("rev-parse", "--abbrev-ref", "HEAD")
}

val branch = branchName()
val commitHash = latestCommitHash()
val baseVersion = project.version as String
val isRelease = !baseVersion.contains("-")
val isMainBranch = branch == "master"
val suffixedVersion = if (isRelease) baseVersion else "$baseVersion+$commitHash"

if (isMainBranch) {
    hangarPublish {
        publications.register("plugin") {
            version = suffixedVersion
            id = "SupaDupaBroadcast"
            channel = if (!isRelease) "Snapshot" else "Release"
            apiKey = System.getenv("HANGAR_API_TOKEN")

            platforms {
                paper {
                    jar = tasks.jar.flatMap { it.archiveFile }
                    platformVersions = listOf("1.9-1.20.4")
                }
            }
        }
    }
}
