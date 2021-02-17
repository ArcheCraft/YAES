import org.jetbrains.kotlin.daemon.common.OSKind

plugins {
    java
    application
    kotlin("jvm")
}

group = "com.archecraft.yaes"
version = "1.0-SNAPSHOT"

val lwjglNatives = when (OSKind.current) {
    OSKind.Unix -> "natives-linux"
    OSKind.OSX -> "natives-macos"
    OSKind.Windows -> "natives-windows"
    else -> throw GradleException("Unkown OS")
}


repositories {
    google()
    mavenCentral()
    maven(url = "https://dl.bintray.com/hotkeytlt/maven")
    maven(url = "https://dl.bintray.com/kotlin/dokka")
    maven(url = "https://jitpack.io")
    maven(url = "https://kotlin.bintray.com/kotlinx")
    maven(url = "http://artifactory.nimblygames.com/artifactory/ng-public-release")
    mavenLocal()
    jcenter()
}

dependencies {
    val ktorVersion: String by project
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
    
    
    
    implementation("com.archecraft.lib:lib:1.0-SNAPSHOT")
    
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