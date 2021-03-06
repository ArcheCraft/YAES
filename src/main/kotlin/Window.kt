package com.archecraft.yaes

import io.ktor.utils.io.bits.*
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI
import org.lwjgl.glfw.GLFWVulkan
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.NULL

class Window(private val width: Int, private val height: Int, private val title: String, private val framebufferResizeCallback: (width: Int, height: Int) -> Unit) {
    private var window = NULL
    
    var iconified = false
        private set
    val handle
        get() = window
    val size: Pair<Int, Int>
        get() {
            val width = MemoryStack.stackGet().mallocInt (1)
            val height = MemoryStack.stackGet().mallocInt(1)
            glfwGetFramebufferSize(window, width, height)
            return width.get(0) to height.get(0)
        }
    
    
    fun init() {
        glfwInit()
        
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API)
        
        window = glfwCreateWindow(width, height, title, NULL, NULL)
        glfwSetFramebufferSizeCallback(window) { _, width: Int, height: Int -> framebufferResizeCallback(width, height) }
        glfwSetWindowIconifyCallback(window) { _, iconified: Boolean -> this.iconified = iconified }
    }
    
    fun destroy() {
        glfwDestroyWindow(window)
        
        glfwTerminate()
    }
    
    
    fun shouldClose() = glfwWindowShouldClose(window)
    
    fun update() {
        glfwPollEvents()
    }
}