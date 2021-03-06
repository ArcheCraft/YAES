package com.archecraft.yaes

import mu.KLogger
import mu.KotlinLogging
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.context.KoinContext
import org.koin.core.context.startKoin
import org.lwjgl.system.Configuration
import java.io.OutputStream
import java.io.PrintStream


private val LwjglLogger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    Configuration.DEBUG_STREAM.set(PrintStream(LoggingOutputStream(LwjglLogger)))
    
    val koinContext = ApplicationContext()
    
    startKoin(koinContext) {
    
    }
    
    val app = VulkanApplication()
    app.run()
}

val DEBUG = Configuration.DEBUG.get(false)


internal class ApplicationContext : KoinContext {
    private var koinApp: KoinApplication? = null
    
    
    override fun get(): Koin = getOrNull() ?: error("Koin Application not yet started")
    
    override fun getOrNull(): Koin? = koinApp?.koin
    
    
    fun getApp(): KoinApplication = getAppOrNull() ?: error("Koin Application not yet started")
    
    fun getAppOrNull(): KoinApplication? = koinApp
    
    
    override fun register(koinApplication: KoinApplication) {
        koinApp = koinApplication
    }
    
    override fun stop() {
        koinApp?.close()
    }
}

private class LoggingOutputStream(val logger: KLogger) : OutputStream() {
    private var closed = false
    private var buffer = StringBuffer()
    
    override fun close() {
        closed = true
    }
    
    private fun ensureOpen() {
        if (closed) throw IllegalStateException("This output stream is already closed!")
    }
    
    
    private fun write() {
        logger.debug { buffer.toString() }
        buffer = StringBuffer()
    }
    
    
    override fun flush() {
        ensureOpen()
    }
    
    override fun write(b: Int) {
        ensureOpen()
        buffer.append(Character.toChars(b))
        if (String(Character.toChars(b)) == System.lineSeparator()) write()
    }
    
    override fun write(b: ByteArray) {
        ensureOpen()
        val string = String(b)
        if (string.endsWith(System.lineSeparator())) {
            buffer.append(string.removeSuffix(System.lineSeparator()))
            write()
        } else {
            buffer.append(string)
        }
    }
    
    override fun write(b: ByteArray, off: Int, len: Int) {
        ensureOpen()
        val string = String(b, off, len)
        if (string.endsWith(System.lineSeparator())) {
            buffer.append(string.removeSuffix(System.lineSeparator()))
            write()
        } else {
            buffer.append(string)
        }
    }
}