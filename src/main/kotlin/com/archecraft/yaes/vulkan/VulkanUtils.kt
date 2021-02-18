package com.archecraft.yaes.vulkan

import com.archecraft.yaes.Logger
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTDebugUtils.*
import java.nio.LongBuffer
import com.archecraft.yaes.VulkanApplication.Vertex
import java.nio.ByteBuffer
import com.archecraft.yaes.VulkanApplication.UniformBufferObject
import org.joml.Matrix4f


inline fun <R> withStack(block: MemoryStack.() -> R) = MemoryStack.stackPush().use(block)

inline fun <R> MemoryStack.withStack(block: MemoryStack.() -> R) = push().use(block)


fun Int.checkResult(message: String) {
    if (this != VK10.VK_SUCCESS) {
        throw VulkanException("$message (ErrorCode: $this)")
    }
}

class VulkanException(message: String) : RuntimeException(message)


fun MemoryStack.asPointerBuffer(string: String) = asPointerBuffer(listOf(string))

fun MemoryStack.asPointerBuffer(strings: Collection<String>): PointerBuffer {
    val pointers = mallocPointer(strings.size)
    
    strings.map(::UTF8).forEach(pointers::put)
    
    return pointers.rewind()
}


fun memcpy(buffer: ByteBuffer, vertices: Array<Vertex>) {
    for (vertex in vertices) {
        buffer.putFloat(vertex.pos.x())
        buffer.putFloat(vertex.pos.y())
        buffer.putFloat(vertex.color.x())
        buffer.putFloat(vertex.color.y())
        buffer.putFloat(vertex.color.z())
        buffer.putFloat(vertex.texCoord.x())
        buffer.putFloat(vertex.texCoord.y())
    }
    buffer.rewind()
}

fun memcpy(buffer: ByteBuffer, indices: IntArray) {
    for (index in indices) {
        buffer.putInt(index)
    }
    buffer.rewind()
}

fun memcpy(buffer: ByteBuffer, ubo: UniformBufferObject) {
    val mat4size = 16 * Float.SIZE_BYTES
    
    ubo.model.get(0, buffer)
    ubo.view.get(alignas(mat4size, alignof(ubo.view)), buffer)
    ubo.proj.get(alignas(mat4size * 2, alignof(ubo.view)), buffer)
    buffer.rewind()
}

fun memcpy(dst: ByteBuffer, src: ByteBuffer, size: Long) {
    src.limit(size.toInt())
    dst.put(src)
    src.limit(src.capacity()).rewind()
}


private fun logDebugMessage(messageSeverity: Int, message: () -> String) = when (messageSeverity) {
    VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT -> Logger.trace(message)
    VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT -> Logger.debug(message)
    VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT -> Logger.warn(message)
    VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT -> {
        Logger.error { Thread.currentThread().stackTrace.map { it.toString() }.toString() }
        Logger.error(message)
    }
    else -> Logger.info(message)
}

private fun Int.debugMessageTypeToString() = when (this) {
    VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT -> "General"
    VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT -> "Invalid usage"
    VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT -> "Not optimal"
    else -> "Unknown"
}

fun debugCallback(messageSeverity: Int, messageType: Int, pCallbackData: Long, pUserData: Long): Int {
    val callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData)
    logDebugMessage(messageSeverity) { "Validation layer (${messageType.debugMessageTypeToString()}): ${callbackData.pMessageString().removeSuffix(System.lineSeparator())}" }
    return VK10.VK_FALSE
}

fun populateDebugMessengerCreateInfo(debugCreateInfo: VkDebugUtilsMessengerCreateInfoEXT) {
    debugCreateInfo.apply {
        messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT or VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT or VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT or VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT)
        messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT or VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT or VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT)
        pfnUserCallback(::debugCallback)
    }
}

fun createDebugUtilsMessengerEXT(instance: VkInstance, createInfo: VkDebugUtilsMessengerCreateInfoEXT, allocationCallbacks: VkAllocationCallbacks?, pDebugMessenger: LongBuffer): Int {
    return if (VK10.vkGetInstanceProcAddr(instance, "vkCreateDebugUtilsMessengerEXT") != MemoryUtil.NULL)
        vkCreateDebugUtilsMessengerEXT(instance, createInfo, allocationCallbacks, pDebugMessenger)
    else
        VK10.VK_ERROR_EXTENSION_NOT_PRESENT
}

fun destroyDebugUtilsMessengerEXT(instance: VkInstance, debugMessenger: Long, allocationCallbacks: VkAllocationCallbacks?) {
    if (VK10.vkGetInstanceProcAddr(instance, "vkDestroyDebugUtilsMessengerEXT") != MemoryUtil.NULL) {
        vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, allocationCallbacks)
    }
}