package com.archecraft.yaes

import com.archecraft.yaes.vulkan.*
import mu.KotlinLogging
import org.joml.*
import org.joml.Math.clamp
import org.joml.Math.toRadians
import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface
import org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions
import org.lwjgl.stb.STBImage.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTDebugUtils.*
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK10.*
import java.lang.ClassLoader.getSystemResource
import java.net.URI
import java.nio.IntBuffer
import java.nio.LongBuffer
import java.nio.file.Paths


val Logger = KotlinLogging.logger {}


@Suppress("SpellCheckingInspection", "PrivatePropertyName", "EXPERIMENTAL_API_USAGE", "DuplicatedCode")
class VulkanApplication {
    private lateinit var window: Window
    
    
    private lateinit var instance: VkInstance
    private var debugMessenger: Long = VK_NULL_HANDLE
    private var surface: Long = VK_NULL_HANDLE
    
    private lateinit var physicalDevice: VkPhysicalDevice
    private lateinit var device: VkDevice
    
    private lateinit var graphicsQueue: VkQueue
    private lateinit var presentQueue: VkQueue
    
    private var swapchain: Long = VK_NULL_HANDLE
    private lateinit var swapChainImages: List<Long>
    private var swapChainImageFormat: Int = 0
    private lateinit var swapChainExtent: VkExtent2D
    private lateinit var swapChainImageViews: List<Long>
    private lateinit var swapChainFramebuffers: List<Long>
    
    private var renderPass: Long = VK_NULL_HANDLE
    private var descriptorPool: Long = VK_NULL_HANDLE
    private var descriptorSetLayout: Long = VK_NULL_HANDLE
    private lateinit var descriptorSets: List<Long>
    private var pipelineLayout: Long = VK_NULL_HANDLE
    private var graphicsPipeline: Long = VK_NULL_HANDLE
    
    private var commandPool: Long = VK_NULL_HANDLE
    
    private var textureImage: Long = VK_NULL_HANDLE
    private var textureImageMemory: Long = VK_NULL_HANDLE
    private var textureImageView: Long = VK_NULL_HANDLE
    private var textureSampler: Long = VK_NULL_HANDLE
    
    private var vertexBuffer: Long = VK_NULL_HANDLE
    private var vertexBufferMemory: Long = VK_NULL_HANDLE
    private var indexBuffer: Long = VK_NULL_HANDLE
    private var indexBufferMemory: Long = VK_NULL_HANDLE
    
    private lateinit var uniformBuffers: List<Long>
    private lateinit var uniformBuffersMemory: List<Long>
    
    private lateinit var commandBuffers: List<VkCommandBuffer>
    
    private lateinit var inFlightFrames: MutableList<Frame>
    private lateinit var imagesInFlight: MutableMap<Int, Frame>
    private var currentFrame = 0
    private val MAX_FRAMES_IN_FLIGHT = 2
    
    private var framebufferResized: Boolean = false
    
    
    private class QueueFamilyIndices {
        var graphicsFamily: Int? = null
        var presentFamily: Int? = null
        
        
        val unique get() = if (isComplete()) listOf(graphicsFamily!!, presentFamily!!).distinct() else emptyList()
        
        fun isComplete() = graphicsFamily != null && presentFamily != null
    }
    
    private class SwapChainSupportDetails {
        lateinit var capabilities: VkSurfaceCapabilitiesKHR
        lateinit var formats: List<VkSurfaceFormatKHR>
        lateinit var presentModes: List<Int>
    }
    
    private class Frame(val imageAvailableSemaphore: Long, val renderFinishedSemaphore: Long, val fence: Long) {
        val bufferImageAvailableSemaphore: LongBuffer get() = MemoryStack.stackGet().longs(imageAvailableSemaphore)
        val bufferRenderFinishedSemaphore: LongBuffer get() = MemoryStack.stackGet().longs(renderFinishedSemaphore)
        val bufferFence: LongBuffer get() = MemoryStack.stackGet().longs(fence)
    }
    
    class Vertex(val pos: Vector2f, val color: Vector3f, val texCoord: Vector2f) {
        companion object {
            const val SIZEOF = (2 + 3 + 2) * Float.SIZE_BYTES
            const val OFFSETOF_POS = 0
            const val OFFSETOF_COLOR = 2 * Float.SIZE_BYTES
            const val OFFSETOF_TEXCOORD = (2 + 3) * Float.SIZE_BYTES
            
            
            val bindingDescription: VkVertexInputBindingDescription.Buffer
                get() {
                    val buffer = VkVertexInputBindingDescription.callocStack(1)
                    
                    buffer.get(0).apply {
                        binding(0)
                        stride(SIZEOF)
                        inputRate(VK_VERTEX_INPUT_RATE_VERTEX)
                    }
                    
                    return buffer
                }
            
            val attributeDescription: VkVertexInputAttributeDescription.Buffer
                get() {
                    val buffer = VkVertexInputAttributeDescription.callocStack(3)
                    
                    buffer.get(0).apply {
                        binding(0)
                        location(0)
                        format(VK_FORMAT_R32G32_SFLOAT)
                        offset(OFFSETOF_POS)
                    }
                    
                    buffer.get(1).apply {
                        binding(0)
                        location(1)
                        format(VK_FORMAT_R32G32B32_SFLOAT)
                        offset(OFFSETOF_COLOR)
                    }
    
                    buffer.get(2).apply {
                        binding(0)
                        location(2)
                        format(VK_FORMAT_R32G32_SFLOAT)
                        offset(OFFSETOF_TEXCOORD)
                    }
                    
                    return buffer
                }
        }
    }
    
    class UniformBufferObject(val model: Matrix4f = Matrix4f(), val view: Matrix4f = Matrix4f(), val proj: Matrix4f = Matrix4f()) {
        companion object {
            const val SIZEOF = 3 * 16 * Float.SIZE_BYTES
        }
    }
    
    
    private val validationLayers: Set<String> = if (DEBUG) setOf("VK_LAYER_KHRONOS_validation") else emptySet()
    private val deviceExtensions: Set<String> = setOf(VK_KHR_SWAPCHAIN_EXTENSION_NAME)
    
    private val VERTICES = arrayOf(
        Vertex(Vector2f(-0.5f, -0.5f), Vector3f(1.0f, 0.0f, 0.0f), Vector2f(1.0f, 0.0f)),
        Vertex(Vector2f(0.5f, -0.5f), Vector3f(0.0f, 1.0f, 0.0f), Vector2f(0.0f, 0.0f)),
        Vertex(Vector2f(0.5f, 0.5f), Vector3f(0.0f, 0.0f, 1.0f), Vector2f(0.0f, 1.0f)),
        Vertex(Vector2f(-0.5f, 0.5f), Vector3f(1.0f, 1.0f, 1.0f), Vector2f(1.0f, 1.0f))
    )
    private val INDICES = intArrayOf(
        0, 1, 2,
        2, 3, 0
    )
    
    
    fun run() {
        initWindow()
        initVulkan()
        mainLoop()
        cleanup()
    }
    
    
    private fun initWindow() {
        window = Window(800, 600, "YAES") { _, _ -> framebufferResized = true }
        window.init()
    }
    
    // TODO: stupCommandBuffer() -> helper functions record  to it -> flushSetupCommandBuffer()
    private fun initVulkan() = withStack {
        createInstance()
        setupDebugMessenger()
        createSurface()
        pickPhysicalDevice()
        createLogicalDevice()
        createSwapChain()
        createImageViews()
        createRenderPass()
        createDescriptorSetLayout()
        createGraphicsPipeline()
        createFramebuffers()
        createCommandPool()
        createTextureImage()
        createTextureImageView()
        createTextureSampler()
        createVertexBuffer()
        createIndexBuffer()
        createUniformBuffers()
        createDescriptorPool()
        createDescriptorSets()
        createCommandBuffers()
        createSyncObjects()
    }
    
    private fun mainLoop() {
        while (!window.shouldClose()) {
            window.update()
            if (!window.iconified) drawFrame()
        }
        
        vkDeviceWaitIdle(device)
    }
    
    private fun cleanup() {
        cleanupSwapChain()
        
        vkDestroySampler(device, textureSampler, null)
        vkDestroyImageView(device, textureImageView, null)
        vkDestroyImage(device, textureImage, null)
        vkFreeMemory(device, textureImageMemory, null)
        vkDestroyDescriptorSetLayout(device, descriptorSetLayout, null)
        vkDestroyBuffer(device, vertexBuffer, null)
        vkFreeMemory(device, vertexBufferMemory, null)
        vkDestroyBuffer(device, indexBuffer, null)
        vkFreeMemory(device, indexBufferMemory, null)
        inFlightFrames.forEach {
            vkDestroySemaphore(device, it.imageAvailableSemaphore, null)
            vkDestroySemaphore(device, it.renderFinishedSemaphore, null)
            vkDestroyFence(device, it.fence, null)
        }
        imagesInFlight.clear()
        vkDestroyCommandPool(device, commandPool, null)
        vkDestroyDevice(device, null)
        vkDestroySurfaceKHR(instance, surface, null)
        if (DEBUG) destroyDebugUtilsMessengerEXT(instance, debugMessenger, null)
        vkDestroyInstance(instance, null)
        
        window.destroy()
    }
    
    
    private fun MemoryStack.recreateSwapChain() {
        if (window.iconified) return
        
        vkDeviceWaitIdle(device)
        
        cleanupSwapChain()
        
        createSwapChain()
        createImageViews()
        createRenderPass()
        createGraphicsPipeline()
        createFramebuffers()
        createUniformBuffers()
        createDescriptorPool()
        createDescriptorSets()
        createCommandBuffers()
    }
    
    private fun cleanupSwapChain() {
        swapChainFramebuffers.forEach { vkDestroyFramebuffer(device, it, null) }
        commandBuffers.forEach { vkFreeCommandBuffers(device, commandPool, it) }
        vkDestroyPipeline(device, graphicsPipeline, null)
        vkDestroyPipelineLayout(device, pipelineLayout, null)
        vkDestroyRenderPass(device, renderPass, null)
        swapChainImageViews.forEach { vkDestroyImageView(device, it, null) }
        vkDestroySwapchainKHR(device, swapchain, null)
        uniformBuffers.forEach { vkDestroyBuffer(device, it, null) }
        uniformBuffersMemory.forEach { vkFreeMemory(device, it, null) }
        vkDestroyDescriptorPool(device, descriptorPool, null)
    }
    
    
    private fun MemoryStack.createInstance() = withStack {
        if (DEBUG && !checkValidationLayers()) throw VulkanException("Validation layers requested, but not available!")
        
        val applicationInfo = ApplicationInfo().apply {
            pApplicationName(UTF8("YAES"))
            applicationVersion(VK_MAKE_VERSION(1, 0, 0))
            pEngineName(UTF8("No Engine"))
            engineVersion(VK_MAKE_VERSION(1, 0, 0))
            apiVersion(VK_API_VERSION_1_0)
        }
        
        val instanceCreateInfo = InstanceCreateInfo().apply {
            pApplicationInfo(applicationInfo)
            ppEnabledExtensionNames(getRequiredExtensions())
            if (DEBUG) {
                ppEnabledLayerNames(asPointerBuffer(validationLayers))
                
                val createInfo = DebugUtilsMessengerCreateInfo()
                populateDebugMessengerCreateInfo(createInfo)
                pNext(createInfo.address())
            }
        }
        
        val pointer = pointers(VK_NULL_HANDLE)
        vkCreateInstance(instanceCreateInfo, null, pointer).checkResult("Failed to create instance!")
        instance = VkInstance(pointer.get(0), instanceCreateInfo)
    }
    
    
    private fun MemoryStack.setupDebugMessenger() = withStack {
        if (!DEBUG) {
            return
        }
        
        val createInfo = DebugUtilsMessengerCreateInfo()
        populateDebugMessengerCreateInfo(createInfo)
        
        val pDebugMessenger = longs(VK_NULL_HANDLE)
        createDebugUtilsMessengerEXT(instance, createInfo, null, pDebugMessenger).checkResult("Failed to set up debug messenger!")
        debugMessenger = pDebugMessenger.get(0)
    }
    
    
    private fun MemoryStack.createSurface() = withStack {
        val handle = longs(VK_NULL_HANDLE)
        glfwCreateWindowSurface(instance, window.handle, null, handle).checkResult("Failed to create surface!")
        surface = handle.get(0)
    }
    
    
    // TODO: Use ratings instead of first one
    private fun MemoryStack.pickPhysicalDevice() = withStack {
        physicalDevice = getPhysicalDevices(instance).firstOrNull { isDeviceSuitable(it) } ?: throw VulkanException("Failed to find GPUs with Vulkan support!")
    }
    
    private fun MemoryStack.isDeviceSuitable(device: VkPhysicalDevice): Boolean {
        val features = VkPhysicalDeviceFeatures.mallocStack(this)
        val properties = VkPhysicalDeviceProperties.mallocStack(this)
        vkGetPhysicalDeviceFeatures(device, features)
        vkGetPhysicalDeviceProperties(device, properties)
        
        val indices = findQueueFamilies(device)
        
        val extensionsSupported = checkDeviceExtensions(device)
        
        val swapChainAdequate = if (extensionsSupported) querySwapChainSupport(device).let { it.formats.isNotEmpty() && it.presentModes.isNotEmpty() } else false
        
        return indices.isComplete() && extensionsSupported && swapChainAdequate && features.samplerAnisotropy()
    }
    
    private fun MemoryStack.findQueueFamilies(device: VkPhysicalDevice): QueueFamilyIndices {
        val indices = QueueFamilyIndices()
        
        for ((i, queueFamily) in getQueueFamilies(device).withIndex()) {
            if (queueFamily.queueFlags() and VK_QUEUE_GRAPHICS_BIT != 0) indices.graphicsFamily = i
            
            val surfaceSupport = mallocInt(1)
            vkGetPhysicalDeviceSurfaceSupportKHR(device, i, surface, surfaceSupport)
            if (surfaceSupport.get(0) == VK_TRUE) indices.presentFamily = i
            
            if (indices.isComplete()) break
        }
        
        return indices
    }
    
    
    private fun MemoryStack.createLogicalDevice() = withStack {
        val indices = findQueueFamilies(physicalDevice)
        
        val queuePriority = mallocFloat(1).put(0, 1.0f)
        val uniqueQueueFamilies = indices.unique
        val queueCreateInfos = VkDeviceQueueCreateInfo.callocStack(uniqueQueueFamilies.size, this)
        uniqueQueueFamilies.withIndex().forEach { (i, queueFamily) ->
            queueCreateInfos.get(i).apply {
                sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                queueFamilyIndex(queueFamily)
                pQueuePriorities(queuePriority)
            }
        }
        
        val deviceFeatures = VkPhysicalDeviceFeatures.callocStack(this).apply {
            samplerAnisotropy(true)
        }
        
        val createInfo = DeviceCreateInfo().apply {
            pQueueCreateInfos(queueCreateInfos)
            pEnabledFeatures(deviceFeatures)
            ppEnabledExtensionNames(asPointerBuffer(deviceExtensions))
            if (DEBUG) ppEnabledLayerNames(asPointerBuffer(validationLayers))
        }
        
        var pointer = pointers(VK_NULL_HANDLE)
        vkCreateDevice(physicalDevice, createInfo, null, pointer).checkResult("Failed to create logical device!")
        device = VkDevice(pointer.get(0), physicalDevice, createInfo)
        
        pointer = pointers(VK_NULL_HANDLE)
        vkGetDeviceQueue(device, indices.graphicsFamily!!, 0, pointer)
        graphicsQueue = VkQueue(pointer.get(0), device)
        
        vkGetDeviceQueue(device, indices.presentFamily!!, 0, pointer)
        presentQueue = VkQueue(pointer.get(0), device)
    }
    
    
    private fun MemoryStack.createSwapChain() = withStack {
        val swapChainSupport = querySwapChainSupport(physicalDevice)
        
        val surfaceFormat = chooseSwapSurfaceFormat(swapChainSupport.formats)
        val presentMode = chooseSwapPresentMode(swapChainSupport.presentModes)
        val extent = chooseSwapExtent(swapChainSupport.capabilities)
        
        var minImageCount = swapChainSupport.capabilities.minImageCount() + 1
        if (swapChainSupport.capabilities.maxImageCount() in 1 until minImageCount) minImageCount = swapChainSupport.capabilities.maxImageCount()
        
        val indices = findQueueFamilies(physicalDevice)
        val createInfo = SwapchainCreateInfo().apply {
            surface(surface)
            minImageCount(minImageCount)
            imageFormat(surfaceFormat.format())
            imageColorSpace(surfaceFormat.colorSpace())
            imageExtent(extent)
            imageArrayLayers(1)
            imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
            
            if (indices.presentFamily != indices.graphicsFamily) {
                imageSharingMode(VK_SHARING_MODE_CONCURRENT)
                pQueueFamilyIndices(ints(indices.graphicsFamily!!, indices.presentFamily!!))
            } else {
                imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
            }
            
            preTransform(swapChainSupport.capabilities.currentTransform())
            compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
            presentMode(presentMode)
            clipped(true)
            oldSwapchain(VK_NULL_HANDLE)
        }
        
        val handle = longs(VK_NULL_HANDLE)
        vkCreateSwapchainKHR(device, createInfo, null, handle).checkResult("Failed to create swap chain!")
        swapchain = handle.get(0)
        
        val imageCount = mallocInt(1)
        vkGetSwapchainImagesKHR(device, swapchain, imageCount, null)
        val swapChainImage = callocLong(imageCount.get(0))
        vkGetSwapchainImagesKHR(device, swapchain, imageCount, swapChainImage)
        
        swapChainImages = (0 until swapChainImage.capacity()).map { swapChainImage.get(it) }
        swapChainImageFormat = surfaceFormat.format()
        swapChainExtent = VkExtent2D.create().set(extent)
    }
    
    private fun MemoryStack.querySwapChainSupport(device: VkPhysicalDevice): SwapChainSupportDetails {
        val details = SwapChainSupportDetails()
        
        details.capabilities = VkSurfaceCapabilitiesKHR.mallocStack(this)
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, surface, details.capabilities)
        
        details.formats = getSurfaceFormats(device, surface)
        
        details.presentModes = getPresentModes(device, surface)
        
        return details
    }
    
    private fun chooseSwapSurfaceFormat(availableFormats: List<VkSurfaceFormatKHR>): VkSurfaceFormatKHR {
        return availableFormats.firstOrNull { availableFormat: VkSurfaceFormatKHR -> availableFormat.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR && availableFormat.format() == VK_FORMAT_B8G8R8_SRGB } ?: availableFormats[0]
    }
    
    private fun chooseSwapPresentMode(availablePresentModes: List<Int>): Int {
        return availablePresentModes.firstOrNull { it == VK_PRESENT_MODE_MAILBOX_KHR } ?: VK_PRESENT_MODE_FIFO_KHR
    }
    
    private fun MemoryStack.chooseSwapExtent(capabilities: VkSurfaceCapabilitiesKHR): VkExtent2D {
        if (capabilities.currentExtent().width().toUInt() != UInt.MAX_VALUE) {
            return capabilities.currentExtent()
        }
        
        val width = mallocInt(1)
        val height = mallocInt(1)
        GLFW.glfwGetFramebufferSize(window.handle, width, height)
        val actualExtent = VkExtent2D.mallocStack(this).set(width.get(0), height.get(0))
        
        val minExtent = capabilities.minImageExtent()
        val maxExtent = capabilities.maxImageExtent()
        actualExtent.width(clamp(minExtent.width(), maxExtent.width(), actualExtent.width()))
        actualExtent.height(clamp(minExtent.height(), maxExtent.height(), actualExtent.height()))
        
        return actualExtent
    }
    
    
    private fun MemoryStack.createImageViews() = withStack {
        swapChainImageViews = swapChainImages.map {
            createImageView(it, swapChainImageFormat)
        }
    }
    
    private fun MemoryStack.createImageView(image: Long, format: Int): Long {
        val handle = longs(VK_NULL_HANDLE)
        
        val createInfo = ImageViewCreateInfo().apply {
            image(image)
            viewType(VK_IMAGE_VIEW_TYPE_2D)
            format(format)
            components {
                it.r(VK_COMPONENT_SWIZZLE_IDENTITY)
                it.g(VK_COMPONENT_SWIZZLE_IDENTITY)
                it.b(VK_COMPONENT_SWIZZLE_IDENTITY)
                it.a(VK_COMPONENT_SWIZZLE_IDENTITY)
            }
            subresourceRange {
                it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                it.baseMipLevel(0)
                it.levelCount(1)
                it.baseArrayLayer(0)
                it.layerCount(1)
            }
        }
        
        vkCreateImageView(device, createInfo, null, handle).checkResult("Failed to create image views!")
        return handle.get(0)
    }
    
    
    private fun MemoryStack.createRenderPass() = withStack {
        val colorAttachment = VkAttachmentDescription.callocStack(1, this)
        colorAttachment.get(0).apply {
            format(swapChainImageFormat)
            samples(VK_SAMPLE_COUNT_1_BIT)
            loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
            storeOp(VK_ATTACHMENT_STORE_OP_STORE)
            stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
            stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
            initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
        }
        
        val colorAttachmentRef = VkAttachmentReference.callocStack(1, this)
        colorAttachmentRef.get(0).apply {
            attachment(0)
            layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
        }
        
        val subpass = VkSubpassDescription.callocStack(1, this)
        subpass.get(0).apply {
            pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
            colorAttachmentCount(1)
            pColorAttachments(colorAttachmentRef)
        }
        
        val dependency = VkSubpassDependency.callocStack(1, this)
        dependency.get(0).apply {
            srcSubpass(VK_SUBPASS_EXTERNAL)
            dstSubpass(0)
            srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
            srcAccessMask(0)
            dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
            dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
        }
        
        val renderPassInfo = RenderPassCreateInfo().apply {
            pAttachments(colorAttachment)
            pSubpasses(subpass)
            pDependencies(dependency)
        }
        
        val handle = longs(VK_NULL_HANDLE)
        vkCreateRenderPass(device, renderPassInfo, null, handle).checkResult("Failed to create render pass!")
        renderPass = handle.get(0)
    }
    
    
    private fun MemoryStack.createDescriptorPool() = withStack {
        val poolSize = VkDescriptorPoolSize.callocStack(2, this)
        poolSize.get(0).apply {
            type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
            descriptorCount(swapChainImages.size)
        }
        poolSize.get(1).apply {
            type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
            descriptorCount(swapChainImages.size)
        }
        
        val poolInfo = DescriptorPoolCreateInfo().apply {
            pPoolSizes(poolSize)
            maxSets(swapChainImages.size)
        }
        
        val handle = mallocLong(1)
        vkCreateDescriptorPool(device, poolInfo, null, handle)
        descriptorPool = handle.get(0)
    }
    
    private fun MemoryStack.createDescriptorSetLayout() = withStack {
        val layoutBindings = VkDescriptorSetLayoutBinding.callocStack(2, this)
        // ubo
        layoutBindings.get(0).apply {
            binding(0)
            descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
            descriptorCount(1)
            stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
        }
        // sampler
        layoutBindings.get(1).apply {
            binding(1)
            descriptorCount(1)
            descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
            stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
        }
        
        val layoutInfo = DescriptorSetLayoutCreateInfo().apply {
            pBindings(layoutBindings)
        }
        
        val handle = mallocLong(1)
        vkCreateDescriptorSetLayout(device, layoutInfo, null, handle).checkResult("Failed to create descriptor set layout!")
        descriptorSetLayout = handle.get(0)
    }
    
    private fun MemoryStack.createDescriptorSets() = withStack {
        val layouts = mallocLong(swapChainImages.size)
        swapChainImages.indices.forEach { layouts.put(it, descriptorSetLayout) }
        
        val allocInfo = DescriptorSetAllocateInfo().apply {
            descriptorPool(descriptorPool)
            pSetLayouts(layouts)
        }
        
        val handles = mallocLong(swapChainImages.size)
        vkAllocateDescriptorSets(device, allocInfo, handles).checkResult("Failed to allocate descriptor sets!")
        
        descriptorSets = (0 until handles.capacity()).map { handles.get(it) }
        
        val bufferInfo = VkDescriptorBufferInfo.callocStack(1, this)
        bufferInfo.get(0).apply {
            offset(0)
            range(UniformBufferObject.SIZEOF.toLong())
        }
        val imageInfo = VkDescriptorImageInfo.callocStack(1, this)
        imageInfo.get(0).apply {
            imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
            imageView(textureImageView)
            sampler(textureSampler)
        }
        
        val descriptorWrite = VkWriteDescriptorSet.callocStack(2, this)
        descriptorWrite.get(0).apply {
            sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
            dstBinding(0)
            dstArrayElement(0)
            descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
            descriptorCount(1)
            pBufferInfo(bufferInfo)
        }
        descriptorWrite.get(1).apply {
            sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
            dstBinding(1)
            dstArrayElement(0)
            descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
            descriptorCount(1)
            pImageInfo(imageInfo)
        }
        
        descriptorSets.withIndex().forEach { (i, set) ->
            bufferInfo.buffer(uniformBuffers[i])
            descriptorWrite.get(0).dstSet(set)
            descriptorWrite.get(1).dstSet(set)
            vkUpdateDescriptorSets(device, descriptorWrite, null)
        }
    }
    
    
    private fun MemoryStack.createGraphicsPipeline() = withStack {
        val vertShaderSPIRV = compileShaderFile("shaders/shader.vert", ShaderKind.VERTEX_SHADER)
        val fragShaderSPIRV = compileShaderFile("shaders/shader.frag", ShaderKind.FRAGMENT_SHADER)
        
        val vertShaderModule = createShaderModule(vertShaderSPIRV)
        val fragShaderModule = createShaderModule(fragShaderSPIRV)
        
        val entryPoint = UTF8("main")
        val shaderModules = listOf(vertShaderModule to vertShaderSPIRV, fragShaderModule to fragShaderSPIRV)
        
        val shaderStages = VkPipelineShaderStageCreateInfo.callocStack(shaderModules.size, this)
        shaderModules.withIndex().forEach { (i, pair) ->
            val (module, spirv) = pair
            shaderStages.get(i).apply {
                sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                stage(spirv.kind.vulkanShaderStageBit)
                module(module)
                pName(entryPoint)
            }
        }
        
        
        val vertexInputInfo = PipelineVertexInputStateCreateInfo().apply {
            pVertexBindingDescriptions(Vertex.bindingDescription)
            pVertexAttributeDescriptions(Vertex.attributeDescription)
        }
        
        
        val inputAssembly = PipelineInputAssemblyStateCreateInfo().apply {
            topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
            primitiveRestartEnable(false)
        }
        
        
        val viewport = VkViewport.callocStack(1, this)
        viewport.get(0).apply {
            x(0.0f)
            y(0.0f)
            width(swapChainExtent.width().toFloat())
            height(swapChainExtent.height().toFloat())
            minDepth(0.0f)
            maxDepth(1.0f)
        }
        
        val scissors = VkRect2D.callocStack(1, this)
        scissors.get(0).apply {
            offset {
                it.x(0)
                it.y(0)
            }
            extent(swapChainExtent)
        }
        
        val viewportState = PipelineViewportStateCreateInfo().apply {
            viewportCount(1)
            pViewports(viewport)
            scissorCount(1)
            pScissors(scissors)
        }
        
        
        val rasterizer = PipelineRasterizationStateCreateInfo().apply {
            depthClampEnable(false)
            rasterizerDiscardEnable(false)
            polygonMode(VK_POLYGON_MODE_FILL)
            lineWidth(1.0f)
            cullMode(VK_CULL_MODE_BACK_BIT)
            frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE)
            depthBiasEnable(false)
        }
        
        
        val multisampling = PipelineMultisampleStateCreateInfo().apply {
            sampleShadingEnable(false)
            rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)
        }
        
        
        val colorBlendAttachment = VkPipelineColorBlendAttachmentState.callocStack(1, this)
        colorBlendAttachment.get(0).apply {
            colorWriteMask(VK_COLOR_COMPONENT_R_BIT or VK_COLOR_COMPONENT_G_BIT or VK_COLOR_COMPONENT_B_BIT or VK_COLOR_COMPONENT_A_BIT)
            blendEnable(false)
        }
        
        val colorBlending = PipelineColorBlendStateCreateInfo().apply {
            logicOpEnable(false)
            pAttachments(colorBlendAttachment)
        }
        
        
        val pipelineLayoutInfo = PipelineLayoutCreateInfo().apply {
            pSetLayouts(longs(descriptorSetLayout))
        }
        
        val pipelineLayoutHandle = longs(VK_NULL_HANDLE)
        vkCreatePipelineLayout(device, pipelineLayoutInfo, null, pipelineLayoutHandle).checkResult("Failed to create pipeline layout!")
        pipelineLayout = pipelineLayoutHandle.get(0)
        
        
        val pipelineInfo = VkGraphicsPipelineCreateInfo.callocStack(1, this)
        pipelineInfo.get(0).apply {
            sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
            pStages(shaderStages)
            pVertexInputState(vertexInputInfo)
            pInputAssemblyState(inputAssembly)
            pViewportState(viewportState)
            pRasterizationState(rasterizer)
            pMultisampleState(multisampling)
            pColorBlendState(colorBlending)
            layout(pipelineLayout)
            renderPass(renderPass)
            subpass(0)
            basePipelineHandle(VK_NULL_HANDLE)
            basePipelineIndex(-1)
        }
        
        val graphicsPipelineHandle = longs(VK_NULL_HANDLE)
        vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, pipelineInfo, null, graphicsPipelineHandle).checkResult("Failed to create graphics pipeline!")
        graphicsPipeline = graphicsPipelineHandle.get(0)
        
        
        vkDestroyShaderModule(device, vertShaderModule, null)
        vkDestroyShaderModule(device, fragShaderModule, null)
        
        vertShaderSPIRV.free()
        fragShaderSPIRV.free()
    }
    
    private fun MemoryStack.createShaderModule(spirv: SPIRV): Long {
        val createInfo = ShaderModuleCreateInfo().apply {
            pCode(spirv.bytecode)
        }
        
        val handle = longs(VK_NULL_HANDLE)
        vkCreateShaderModule(device, createInfo, null, handle)
        return handle.get(0)
    }
    
    
    private fun MemoryStack.createFramebuffers() = withStack {
        val attachments = longs(VK_NULL_HANDLE)
        val framebufferHandle = longs(VK_NULL_HANDLE)
        
        val framebufferInfo = FrameBufferCreateInfo().apply {
            renderPass(renderPass)
            width(swapChainExtent.width())
            height(swapChainExtent.height())
            layers(1)
        }
        
        swapChainFramebuffers = swapChainImageViews.map {
            attachments.put(0, it)
            framebufferInfo.pAttachments(attachments)
            
            vkCreateFramebuffer(device, framebufferInfo, null, framebufferHandle).checkResult("Failed to create framebuffer!")
            
            framebufferHandle.get(0)
        }
    }
    
    
    private fun MemoryStack.createCommandPool() = withStack {
        val indices = findQueueFamilies(physicalDevice)
        
        val poolInfo = CommandPoolCreateInfo().apply {
            queueFamilyIndex(indices.graphicsFamily!!)
        }
        
        val handle = longs(VK_NULL_HANDLE)
        vkCreateCommandPool(device, poolInfo, null, handle)
        commandPool = handle.get(0)
    }
    
    
    private fun MemoryStack.createTextureImage() = withStack {
        val filename = Paths.get(URI(getSystemResource ("textures/texture.png").toExternalForm())).toString()
        
        val width = mallocInt(1)
        val height = mallocInt(1)
        val channels = mallocInt(1)
        
        val pixels = stbi_load(filename, width, height, channels, STBI_rgb_alpha) ?: throw VulkanException("Failed to load texture image $filename")
        val imageSize = width.get(0) * height.get(0) * 4
        
        val stagingBuffer = mallocLong(1)
        val stagingBufferMemory = mallocLong(1)
        createBuffer(imageSize.toLong(), VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, stagingBuffer, stagingBufferMemory)
        
        val data: PointerBuffer = mallocPointer(1)
        vkMapMemory(device, stagingBufferMemory.get(0), 0, imageSize.toLong(), 0, data)
        run {
            memcpy(data.getByteBuffer(0, imageSize), pixels, imageSize.toLong())
        }
        vkUnmapMemory(device, stagingBufferMemory.get(0))
        
        stbi_image_free(pixels)
        
        val textureImageHandle: LongBuffer = mallocLong(1)
        val textureImageMemoryHandle: LongBuffer = mallocLong(1)
        createImage(width.get(0), height.get(0), VK_FORMAT_R8G8B8A8_SRGB, VK_IMAGE_TILING_OPTIMAL, VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, textureImageHandle, textureImageMemoryHandle)
        
        textureImage = textureImageHandle.get(0)
        textureImageMemory = textureImageMemoryHandle.get(0)
        
        transitionImageLayout(textureImage, VK_FORMAT_R8G8B8A8_SRGB, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
        
        copyBufferToImage(stagingBuffer.get(0), textureImage, width.get(0), height.get(0))
        
        transitionImageLayout(textureImage, VK_FORMAT_R8G8B8A8_SRGB, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
        
        vkDestroyBuffer(device, stagingBuffer.get(0), null)
        vkFreeMemory(device, stagingBufferMemory.get(0), null)
    }
    
    private fun MemoryStack.createImage(width: Int, height: Int, format: Int, tiling: Int, usage: Int, memProperties: Int, textureImage: LongBuffer, textureImageMemory: LongBuffer) {
        val imageInfo = ImageCreateInfo().apply {
            imageType(VK_IMAGE_TYPE_2D)
            extent {
                it.width(width)
                it.height(height)
                it.depth(1)
            }
            mipLevels(1)
            arrayLayers(1)
            format(format)
            tiling(tiling)
            initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            usage(usage)
            samples(VK_SAMPLE_COUNT_1_BIT)
            sharingMode(VK_SHARING_MODE_EXCLUSIVE)
        }
        
        vkCreateImage(device, imageInfo, null, textureImage).checkResult("Failed to create image!")
        
        val memRequirements = VkMemoryRequirements.mallocStack(this)
        vkGetImageMemoryRequirements(device, textureImage.get(0), memRequirements)
        
        val allocInfo = MemoryAllocateInfo().apply {
            allocationSize(memRequirements.size())
            memoryTypeIndex(findMemoryType(memRequirements.memoryTypeBits(), memProperties))
        }
        
        vkAllocateMemory(device, allocInfo, null, textureImageMemory).checkResult("Failed to allocate image memory!")
        
        vkBindImageMemory(device, textureImage.get(0), textureImageMemory.get(0), 0)
    }
    
    private fun MemoryStack.transitionImageLayout(image: Long, format: Int, oldLayout: Int, newLayout: Int) {
        val barrier = VkImageMemoryBarrier.callocStack(1, this)
        barrier.get(0).apply {
            sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
            oldLayout(oldLayout)
            newLayout(newLayout)
            srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            image(image)
            subresourceRange {
                it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                it.baseMipLevel(0)
                it.levelCount(1)
                it.baseArrayLayer(0)
                it.layerCount(1)
            }
        }
        
        val sourceStage: Int
        val destinationStage: Int
        
        if (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
            barrier.srcAccessMask(0)
            barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
            
            sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT
            destinationStage = VK_PIPELINE_STAGE_TRANSFER_BIT
        } else if (oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && newLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {
            barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
            barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
            
            sourceStage = VK_PIPELINE_STAGE_TRANSFER_BIT
            destinationStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
        } else {
            throw IllegalArgumentException("Unsupported layout transition")
        }
        
        val commandBuffer = beginSingleTimeCommands()
        
        vkCmdPipelineBarrier(commandBuffer, sourceStage, destinationStage, 0, null, null, barrier)
        
        endSingleTimeCommands(commandBuffer)
    }
    
    private fun MemoryStack.copyBufferToImage(buffer: Long, image: Long, width: Int, height: Int) {
        val region = VkBufferImageCopy.callocStack(1, this)
        region.get(0).apply {
            bufferOffset(0)
            bufferRowLength(0)
            bufferImageHeight(0)
            imageSubresource {
                it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                it.mipLevel(0)
                it.baseArrayLayer(0)
                it.layerCount(1)
            }
            imageOffset().set(0, 0, 0)
            imageExtent().set(width, height, 1)
        }
        
        val commandBuffer = beginSingleTimeCommands()
        
        vkCmdCopyBufferToImage(commandBuffer, buffer, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region)
        
        endSingleTimeCommands(commandBuffer)
    }
    
    
    private fun MemoryStack.createTextureImageView() = withStack {
        textureImageView = createImageView(textureImage, VK_FORMAT_R8G8B8A8_SRGB)
    }
    
    
    private fun MemoryStack.createTextureSampler() = withStack {
        val properties = VkPhysicalDeviceProperties.mallocStack(this)
        vkGetPhysicalDeviceProperties(physicalDevice, properties)
        
        val samplerInfo: VkSamplerCreateInfo = SamplerCreateInfo().apply {
            magFilter(VK_FILTER_LINEAR)
            minFilter(VK_FILTER_LINEAR)
            addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT)
            addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT)
            addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT)
            anisotropyEnable(true)
            maxAnisotropy(properties.limits().maxSamplerAnisotropy())
            borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK)
            unnormalizedCoordinates(false)
            compareEnable(false)
            compareOp(VK_COMPARE_OP_ALWAYS)
            mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR)
        }
        
        val textureSamplerHandle: LongBuffer = mallocLong(1)
        
        vkCreateSampler(device, samplerInfo, null, textureSamplerHandle).checkResult("Failed to create texture sampler!")
        
        textureSampler = textureSamplerHandle[0]
    }
    
    
    private fun MemoryStack.createVertexBuffer() = withStack {
        val bufferSize = (Vertex.SIZEOF * VERTICES.size).toLong()
        
        val buffer = mallocLong(1)
        val bufferMemory = mallocLong(1)
        createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, buffer, bufferMemory)
        val stagingBuffer = buffer.get(0)
        val stagingBufferMemory = bufferMemory.get(0)
        
        val data = mallocPointer(1)
        vkMapMemory(device, stagingBufferMemory, 0, bufferSize, 0, data)
        run {
            memcpy(data.getByteBuffer(0, bufferSize.toInt()), VERTICES)
        }
        vkUnmapMemory(device, stagingBufferMemory)
        
        createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT or VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VK_MEMORY_HEAP_DEVICE_LOCAL_BIT, buffer, bufferMemory)
        
        vertexBuffer = buffer.get(0)
        vertexBufferMemory = bufferMemory.get(0)
        
        copyBuffer(stagingBuffer, vertexBuffer, bufferSize)
        
        vkDestroyBuffer(device, stagingBuffer, null)
        vkFreeMemory(device, stagingBufferMemory, null)
    }
    
    private fun MemoryStack.createIndexBuffer() = withStack {
        val bufferSize = (Int.SIZE_BYTES * INDICES.size).toLong()
        
        val buffer = mallocLong(1)
        val bufferMemory = mallocLong(1)
        createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, buffer, bufferMemory)
        val stagingBuffer = buffer.get(0)
        val stagingBufferMemory = bufferMemory.get(0)
        
        val data = mallocPointer(1)
        vkMapMemory(device, stagingBufferMemory, 0, bufferSize, 0, data)
        run {
            memcpy(data.getByteBuffer(0, bufferSize.toInt()), INDICES)
        }
        vkUnmapMemory(device, stagingBufferMemory)
        
        createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT or VK_BUFFER_USAGE_INDEX_BUFFER_BIT, VK_MEMORY_HEAP_DEVICE_LOCAL_BIT, buffer, bufferMemory)
        
        indexBuffer = buffer.get(0)
        indexBufferMemory = bufferMemory.get(0)
        
        copyBuffer(stagingBuffer, indexBuffer, bufferSize)
        
        vkDestroyBuffer(device, stagingBuffer, null)
        vkFreeMemory(device, stagingBufferMemory, null)
    }
    
    private fun MemoryStack.createUniformBuffers() = withStack {
        val buffer: LongBuffer = mallocLong(1)
        val bufferMemory: LongBuffer = mallocLong(1)
        
        val list = (swapChainImages.indices).map {
            createBuffer(UniformBufferObject.SIZEOF.toLong(), VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, buffer, bufferMemory)
            buffer.get(0) to bufferMemory.get(0)
        }
        
        uniformBuffers = list.map { it.first }
        uniformBuffersMemory = list.map { it.second }
    }
    
    private fun MemoryStack.createBuffer(size: Long, usage: Int, properties: Int, buffer: LongBuffer, bufferMemory: LongBuffer) {
        val bufferInfo = BufferCreateInfo().apply {
            size(size)
            usage(usage)
            sharingMode(VK_SHARING_MODE_EXCLUSIVE)
        }
        
        vkCreateBuffer(device, bufferInfo, null, buffer).checkResult("Failed to create vertex buffer!")
        
        val memoryRequirements = VkMemoryRequirements.mallocStack(this)
        vkGetBufferMemoryRequirements(device, buffer.get(0), memoryRequirements)
        
        val allocInfo = MemoryAllocateInfo().apply {
            allocationSize(memoryRequirements.size())
            memoryTypeIndex(findMemoryType(memoryRequirements.memoryTypeBits(), properties))
        }
        
        vkAllocateMemory(device, allocInfo, null, bufferMemory).checkResult("Failed to allocate vertex buffer meomory!")
        
        vkBindBufferMemory(device, buffer.get(0), bufferMemory.get(0), 0)
    }
    
    private fun MemoryStack.beginSingleTimeCommands(): VkCommandBuffer {
        val allocInfo = CommandBufferAllocateInfo().apply {
            level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
            commandPool(commandPool)
            commandBufferCount(1)
        }
        
        val pointer = callocPointer(1)
        vkAllocateCommandBuffers(device, allocInfo, pointer).checkResult("Failed to allocate command buffers!")
        val commandBuffer = VkCommandBuffer(pointer.get(0), device)
        
        val beginInfo = CommandBufferBeginInfo().apply {
            flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
        }
        
        vkBeginCommandBuffer(commandBuffer, beginInfo)
        
        return commandBuffer
    }
    
    private fun MemoryStack.endSingleTimeCommands(commandBuffer: VkCommandBuffer) {
        vkEndCommandBuffer(commandBuffer)
        
        val submitInfo = SubmitInfo().apply {
            pCommandBuffers(pointers(commandBuffer))
        }
        
        vkQueueSubmit(graphicsQueue, submitInfo, VK_NULL_HANDLE)
        vkQueueWaitIdle(graphicsQueue)
        
        vkFreeCommandBuffers(device, commandPool, commandBuffer)
    }
    
    private fun MemoryStack.copyBuffer(srcBuffer: Long, dstBuffer: Long, size: Long) {
        val commandBuffer = beginSingleTimeCommands()
        run {
            val copyRegion = VkBufferCopy.callocStack(1, this)
            copyRegion.get(0).apply {
                size(size)
            }
            vkCmdCopyBuffer(commandBuffer, srcBuffer, dstBuffer, copyRegion)
        }
        endSingleTimeCommands(commandBuffer)
    }
    
    private fun MemoryStack.findMemoryType(typeFilter: Int, properties: Int): Int {
        val memProperties = VkPhysicalDeviceMemoryProperties.mallocStack(this)
        vkGetPhysicalDeviceMemoryProperties(physicalDevice, memProperties)
        
        for (i in 0 until memProperties.memoryTypeCount()) {
            if (typeFilter and (1 shl i) != 0 && memProperties.memoryTypes(i).propertyFlags() and properties == properties) {
                return i
            }
        }
        
        throw VulkanException("Failed to find suitable memory type!")
    }
    
    
    private fun MemoryStack.createCommandBuffers() = withStack {
        val commandBufferCount = swapChainFramebuffers.size
        
        val allocInfo = CommandBufferAllocateInfo().apply {
            commandPool(commandPool)
            level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
            commandBufferCount(commandBufferCount)
        }
        
        val pointers = callocPointer(commandBufferCount)
        vkAllocateCommandBuffers(device, allocInfo, pointers).checkResult("Failed to allocate command buffers!")
        commandBuffers = (0 until pointers.capacity()).map { VkCommandBuffer(pointers.get(it), device) }
        
        commandBuffers.withIndex().forEach { (i, command) ->
            val beginInfo = CommandBufferBeginInfo().apply {
            }
            
            vkBeginCommandBuffer(command, beginInfo).checkResult("Failed to begin command buffer recording!")
            run {
                val renderPassInfo = RenderPassBeginInfo().apply {
                    renderPass(renderPass)
                    framebuffer(swapChainFramebuffers[i])
                    renderArea {
                        it.offset { off ->
                            off.x(0)
                            off.y(0)
                        }
                        it.extent(swapChainExtent)
                    }
                    pClearValues(VkClearValue.callocStack(1, this@withStack).color { it.float32(floats(0.0f, 0.0f, 0.0f, 1.0f)) })
                }
                
                vkCmdBeginRenderPass(command, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE)
                run {
                    vkCmdBindPipeline(command, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline)
                    
                    vkCmdBindVertexBuffers(command, 0, longs(vertexBuffer), longs(0))
                    
                    vkCmdBindIndexBuffer(command, indexBuffer, 0, VK_INDEX_TYPE_UINT32)
                    
                    vkCmdBindDescriptorSets(command, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 0, longs(descriptorSets[i]), null)
                    
                    vkCmdDrawIndexed(command, INDICES.size, 1, 0, 0, 0)
                }
                vkCmdEndRenderPass(command)
            }
            vkEndCommandBuffer(command).checkResult("Failed to record command buffer!")
        }
    }
    
    
    private fun MemoryStack.createSyncObjects() = withStack {
        imagesInFlight = HashMap(swapChainImages.size)
        
        val semaphoreInfo = SemaphoreCreateInfo().apply {
        }
        
        val fenceInfo = FenceCreateInfo().apply {
            flags(VK_FENCE_CREATE_SIGNALED_BIT)
        }
        
        val imageAvailableSemaphore = longs(VK_NULL_HANDLE)
        val renderFinishedSemaphore = longs(VK_NULL_HANDLE)
        val fence = longs(VK_NULL_HANDLE)
        
        inFlightFrames = MutableList(MAX_FRAMES_IN_FLIGHT) {
            vkCreateSemaphore(device, semaphoreInfo, null, imageAvailableSemaphore).checkResult("Failed to create semaphore for frame $it!")
            vkCreateSemaphore(device, semaphoreInfo, null, renderFinishedSemaphore).checkResult("Failed to create semaphore for frame $it!")
            vkCreateFence(device, fenceInfo, null, fence).checkResult("Failed to create fence for frame $it!")
            
            Frame(imageAvailableSemaphore.get(0), renderFinishedSemaphore.get(0), fence.get(0))
        }
    }
    
    
    private fun drawFrame() = withStack {
        val thisFrame = inFlightFrames[currentFrame]
        
        vkWaitForFences(device, thisFrame.bufferFence, true, ULong.MAX_VALUE.toLong())
        
        val pImageIndex: IntBuffer = mallocInt(1)
        var result = vkAcquireNextImageKHR(device, swapchain, ULong.MAX_VALUE.toLong(), thisFrame.imageAvailableSemaphore, VK_NULL_HANDLE, pImageIndex)
        val imageIndex = pImageIndex.get(0)
        
        if (result == VK_ERROR_OUT_OF_DATE_KHR) {
            recreateSwapChain()
            return
        } else if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR) {
            throw VulkanException("Failed to acquire swap chain image! (ErrorCode: $result)")
        }
        
        if (imagesInFlight.containsKey(imageIndex)) {
            vkWaitForFences(device, imagesInFlight[imageIndex]!!.fence, true, ULong.MAX_VALUE.toLong())
        }
        
        imagesInFlight[imageIndex] = thisFrame
        
        updateUniformBuffer(imageIndex)
        
        val submitInfo: VkSubmitInfo = SubmitInfo().apply {
            waitSemaphoreCount(1)
            pWaitSemaphores(thisFrame.bufferImageAvailableSemaphore)
            pWaitDstStageMask(ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
            pSignalSemaphores(thisFrame.bufferRenderFinishedSemaphore)
            pCommandBuffers(pointers(commandBuffers[imageIndex]))
        }
        
        vkResetFences(device, thisFrame.bufferFence)
        
        vkQueueSubmit(graphicsQueue, submitInfo, thisFrame.fence).checkResult("Failed to submit draw command buffer!")
        
        val presentInfo: VkPresentInfoKHR = PresentInfo().apply {
            pWaitSemaphores(thisFrame.bufferRenderFinishedSemaphore)
            swapchainCount(1)
            pSwapchains(longs(swapchain))
            pImageIndices(pImageIndex)
        }
        
        result = vkQueuePresentKHR(presentQueue, presentInfo)
        
        if (result == VK_ERROR_OUT_OF_DATE_KHR || result == VK_SUBOPTIMAL_KHR || framebufferResized) {
            framebufferResized = false
            recreateSwapChain()
        } else if (result != VK_SUCCESS) {
            throw VulkanException("Failed to acquire swap chain image! (ErrorCode: $result)")
        }
        
        currentFrame = (currentFrame + 1) % MAX_FRAMES_IN_FLIGHT
    }
    
    private fun MemoryStack.updateUniformBuffer(imageIndex: Int) {
        val ubo = UniformBufferObject()
        
        ubo.model.rotate((GLFW.glfwGetTime() * toRadians(90.0)).toFloat(), 0.0f, 0.0f, 1.0f)
        ubo.view.lookAt(2.0f, 2.0f, 2.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f)
        ubo.proj.perspective(toRadians(45.0f), swapChainExtent.width().toFloat() / swapChainExtent.height().toFloat(), 0.1f, 10.0f)
        ubo.proj.m11(ubo.proj.m11() * -1)
        
        val data = mallocPointer(1)
        vkMapMemory(device, uniformBuffersMemory[imageIndex], 0, UniformBufferObject.SIZEOF.toLong(), 0, data)
        run {
            memcpy(data.getByteBuffer(0, UniformBufferObject.SIZEOF), ubo)
        }
        vkUnmapMemory(device, uniformBuffersMemory[imageIndex])
    }
    
    
    private fun MemoryStack.getRequiredExtensions(): PointerBuffer {
        val glfwExtensions = glfwGetRequiredInstanceExtensions()!!
        
        if (DEBUG) {
            val extensions = mallocPointer(glfwExtensions.capacity() + 1)
            
            extensions.put(glfwExtensions)
            extensions.put(UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME))
            
            return extensions.rewind()
        }
        
        return glfwExtensions
    }
    
    private fun MemoryStack.checkValidationLayers() = getLayers().map { it.layerNameString() }.containsAll(validationLayers)
    
    private fun MemoryStack.checkDeviceExtensions(device: VkPhysicalDevice) = getExtensions(device).map { it.extensionNameString() }.containsAll(deviceExtensions)
}