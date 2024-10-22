pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = "MinecraftForge"
            url = uri("https://maven.minecraftforge.net")
            content { includeGroupAndSubgroups("net.minecraftforge") }
        }
        maven {
            name = "Partchment Mappings"
            url = uri("https://maven.parchmentmc.org")
        }
        maven {
            name = "Mixin"
            url = uri("https://repo.spongepowered.org/repository/maven-public/")
            content { includeGroupAndSubgroups("org.spongepowered") }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}