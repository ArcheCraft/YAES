import org.jetbrains.kotlin.daemon.common.OSKind
import java.util.Properties

plugins {
    java
    application
    idea
    id("org.jetbrains.gradle.plugin.idea-ext") version "0.10"
    kotlin("jvm")
}

group = "com.archecraft.yaes"
version = "1.0-SNAPSHOT"

val localProperties = Properties()
localProperties.load(rootProject.file("local.properties").inputStream())

val lwjglNatives = when (OSKind.current) {
    OSKind.Unix -> "natives-linux"
    OSKind.OSX -> "natives-macos"
    OSKind.Windows -> "natives-windows"
    else -> throw GradleException("Unkown OS")
}


repositories {
    google()
    mavenCentral()
    maven(url = "https://maven.pkg.github.com/ArcheCraft/ACLib") {
        credentials {
            username = localProperties.getProperty("gpr.user") ?: System.getenv("USERNAME")
            password = localProperties.getProperty("gpr.key") ?: System.getenv("TOKEN")
        }
    }
    maven(url = "https://dl.bintray.com/hotkeytlt/maven")
    maven(url = "https://dl.bintray.com/kotlin/dokka")
    maven(url = "https://jitpack.io")
    maven(url = "https://kotlin.bintray.com/kotlinx")
    maven(url = "http://artifactory.nimblygames.com/artifactory/ng-public-release")
    jcenter()
}

dependencies {
    val ktorVersion: String by project
    val acLibVersion: String by project
    val nightConfigVersion: String by project
    val asmVersion: String by project
    val zip4jVersion: String by project
    val kotlinCoroutinesVersion: String by project
    val kotlinLoggingVersion: String by project
    val koinVersion: String by project
    val betterParseVersion: String by project
    val junitVersion: String by project
    val picocliVersion: String by project
    val logbackVersion: String by project
    val googleCloudBomVersion: String by project
    val gsonVersion: String by project
    val lwjglVersion: String by project
    val jomlVersion: String by project
    
    implementation(enforcedPlatform("com.google.cloud:libraries-bom:$googleCloudBomVersion"))
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    
    
    
    implementation("com.archecraft.lib:lib:$acLibVersion")
    
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    
    
    implementation("org.lwjgl", "lwjgl")
    implementation("org.lwjgl", "lwjgl-assimp")
    implementation("org.lwjgl", "lwjgl-glfw")
    implementation("org.lwjgl", "lwjgl-openal")
    implementation("org.lwjgl", "lwjgl-shaderc")
    implementation("org.lwjgl", "lwjgl-stb")
    implementation("org.lwjgl", "lwjgl-vulkan")
    implementation("org.joml", "joml", jomlVersion)
    
    runtimeOnly("org.lwjgl", "lwjgl", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-assimp", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-glfw", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-openal", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-shaderc", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-stb", classifier = lwjglNatives)
    
    
    implementation("com.electronwill.night-config:toml:$nightConfigVersion")
    implementation("com.electronwill.night-config:hocon:$nightConfigVersion")
    implementation("com.github.h0tk3y.betterParse:better-parse:$betterParseVersion")
    implementation("info.picocli:picocli:$picocliVersion")
    
    implementation("com.google.guava:guava")
    implementation("com.google.code.gson:gson:$gsonVersion")
    
    implementation("org.ow2.asm:asm:$asmVersion")
    
    implementation("net.lingala.zip4j:zip4j:$zip4jVersion")
    
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
    implementation("org.koin:koin-core:$koinVersion")
    implementation("org.koin:koin-logger-slf4j:$koinVersion")
    
    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
    runtimeOnly("ch.qos.logback:logback-classic:$logbackVersion")
    
    testImplementation("junit", "junit", junitVersion)
}

tasks.run.configure {
    main = "com.archecraft.yaes.MainKt"
    jvmArgs("-Dorg.lwjgl.util.Debug=true")
    isIgnoreExitValue = true
}


fun org.gradle.plugins.ide.idea.model.IdeaModule.settings(configure: org.jetbrains.gradle.ext.ModuleSettings.() -> Unit) =
    (this as ExtensionAware).configure(configure)

val org.jetbrains.gradle.ext.ModuleSettings.packagePrefix: org.jetbrains.gradle.ext.PackagePrefixContainer
    get() = (this as ExtensionAware).the()

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
        
        settings {
            packagePrefix["src/main/kotlin"] = "com.archecraft.yaes"
        }
    }
}