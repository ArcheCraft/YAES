package com.archecraft.yaes.vulkan

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.StructBuffer
import org.lwjgl.vulkan.*
import java.nio.ByteBuffer
import java.nio.IntBuffer


fun <T, BUFFER : StructBuffer<T, BUFFER>> MemoryStack.getList(fillCount: (IntBuffer) -> Unit, createBuffer: MemoryStack.(Int) -> BUFFER, fillBuffer: (IntBuffer, BUFFER) -> Unit): List<T> {
    val count = mallocInt(1)
    fillCount(count)
    val buffer = createBuffer(count.get(0))
    fillBuffer(count, buffer)
    return buffer.toList()
}


fun MemoryStack.getExtensions(): List<VkExtensionProperties> = getList({
    VK10.vkEnumerateInstanceExtensionProperties((null as ByteBuffer?), it, null)
}, {
    VkExtensionProperties.mallocStack(it, this)
}) { count, buffer ->
    VK10.vkEnumerateInstanceExtensionProperties((null as ByteBuffer?), count, buffer)
}

fun MemoryStack.getLayers(): List<VkLayerProperties> = getList({
    VK10.vkEnumerateInstanceLayerProperties(it, null)
}, {
    VkLayerProperties.mallocStack(it, this)
}) { count, buffer ->
    VK10.vkEnumerateInstanceLayerProperties(count, buffer)
}

fun MemoryStack.getPhysicalDevices(instance: VkInstance): List<VkPhysicalDevice> {
    val count = mallocInt(1)
    VK10.vkEnumeratePhysicalDevices(instance, count, null)
    val devices = mallocPointer(count.get(0))
    VK10.vkEnumeratePhysicalDevices(instance, count, devices)
    return (0 until devices.capacity()).map { VkPhysicalDevice(devices.get(0), instance) }
}

fun MemoryStack.getQueueFamilies(device: VkPhysicalDevice): List<VkQueueFamilyProperties> = getList({
    VK10.vkGetPhysicalDeviceQueueFamilyProperties(device, it, null)
}, {
    VkQueueFamilyProperties.mallocStack(it, this)
}) { count, buffer ->
    VK10.vkGetPhysicalDeviceQueueFamilyProperties(device, count, buffer)
}

fun MemoryStack.getExtensions(device: VkPhysicalDevice): List<VkExtensionProperties> = getList({
    VK10.vkEnumerateDeviceExtensionProperties(device, (null as ByteBuffer?), it, null)
}, {
    VkExtensionProperties.mallocStack(it, this)
}) { count, buffer ->
    VK10.vkEnumerateDeviceExtensionProperties(device, (null as ByteBuffer?), count, buffer)
}

fun MemoryStack.getSurfaceFormats(device: VkPhysicalDevice, surface: Long): List<VkSurfaceFormatKHR> = getList({
    KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, it, null)
}, {
    VkSurfaceFormatKHR.mallocStack(it, this)
}) { count, buffer ->
    KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, count, buffer)
}

fun MemoryStack.getPresentModes(device: VkPhysicalDevice, surface: Long): List<Int> {
    val count = mallocInt(1)
    KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, count, null)
    val presentModes = mallocInt(count.get(0))
    KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, count, presentModes)
    return (0 until presentModes.capacity()).map { presentModes.get(it) }
}