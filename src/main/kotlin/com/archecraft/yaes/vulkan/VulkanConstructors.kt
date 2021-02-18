@file:Suppress("HasPlatformType", "FunctionName")

package com.archecraft.yaes.vulkan

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR
import org.lwjgl.vulkan.VK10.*


fun MemoryStack.ApplicationInfo() = VkApplicationInfo.callocStack(this).sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)

fun MemoryStack.SubmitInfo() = VkSubmitInfo.callocStack(this).sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)

fun MemoryStack.PresentInfo() = VkPresentInfoKHR.callocStack(this).sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)


fun MemoryStack.InstanceCreateInfo() = VkInstanceCreateInfo.callocStack(this).sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)

fun MemoryStack.DebugUtilsMessengerCreateInfo() = VkDebugUtilsMessengerCreateInfoEXT.callocStack(this).sType(EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)

fun MemoryStack.DeviceCreateInfo() = VkDeviceCreateInfo.callocStack(this).sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)

fun MemoryStack.SwapchainCreateInfo() = VkSwapchainCreateInfoKHR.callocStack(this).sType(KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)

fun MemoryStack.ImageViewCreateInfo() = VkImageViewCreateInfo.callocStack(this).sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)

fun MemoryStack.ShaderModuleCreateInfo() = VkShaderModuleCreateInfo.callocStack(this).sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)

fun MemoryStack.PipelineVertexInputStateCreateInfo() = VkPipelineVertexInputStateCreateInfo.callocStack(this).sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)

fun MemoryStack.PipelineInputAssemblyStateCreateInfo() = VkPipelineInputAssemblyStateCreateInfo.callocStack(this).sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)

fun MemoryStack.PipelineViewportStateCreateInfo() = VkPipelineViewportStateCreateInfo.callocStack(this).sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)

fun MemoryStack.PipelineRasterizationStateCreateInfo() = VkPipelineRasterizationStateCreateInfo.callocStack(this).sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)

fun MemoryStack.PipelineMultisampleStateCreateInfo() = VkPipelineMultisampleStateCreateInfo.callocStack(this).sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)

fun MemoryStack.PipelineColorBlendStateCreateInfo() = VkPipelineColorBlendStateCreateInfo.callocStack(this).sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)

fun MemoryStack.PipelineLayoutCreateInfo() = VkPipelineLayoutCreateInfo.callocStack(this).sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)

fun MemoryStack.RenderPassCreateInfo() = VkRenderPassCreateInfo.callocStack(this).sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)

fun MemoryStack.FrameBufferCreateInfo() = VkFramebufferCreateInfo.callocStack(this).sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)

fun MemoryStack.CommandPoolCreateInfo() = VkCommandPoolCreateInfo.callocStack(this).sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)

fun MemoryStack.SemaphoreCreateInfo() = VkSemaphoreCreateInfo.callocStack(this).sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)

fun MemoryStack.FenceCreateInfo() = VkFenceCreateInfo.callocStack(this).sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)

fun MemoryStack.BufferCreateInfo() = VkBufferCreateInfo.callocStack(this).sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)

fun MemoryStack.DescriptorSetLayoutCreateInfo() = VkDescriptorSetLayoutCreateInfo.callocStack(this).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)

fun MemoryStack.DescriptorPoolCreateInfo() = VkDescriptorPoolCreateInfo.callocStack(this).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)

fun MemoryStack.ImageCreateInfo() = VkImageCreateInfo.callocStack(this).sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)

fun MemoryStack.SamplerCreateInfo() = VkSamplerCreateInfo.callocStack(this).sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)


fun MemoryStack.CommandBufferAllocateInfo() = VkCommandBufferAllocateInfo.callocStack(this).sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)

fun MemoryStack.MemoryAllocateInfo() = VkMemoryAllocateInfo.callocStack(this).sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)

fun MemoryStack.DescriptorSetAllocateInfo() = VkDescriptorSetAllocateInfo.callocStack(this).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)


fun MemoryStack.CommandBufferBeginInfo() = VkCommandBufferBeginInfo.callocStack(this).sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)

fun MemoryStack.RenderPassBeginInfo() = VkRenderPassBeginInfo.callocStack(this).sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)