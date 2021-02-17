rootProject.name = "YAES-Engine"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        mavenCentral()
    }
    
    
    val kotlinVersion: String by settings
    
    plugins {
        kotlin("jvm") version kotlinVersion
    }
}
