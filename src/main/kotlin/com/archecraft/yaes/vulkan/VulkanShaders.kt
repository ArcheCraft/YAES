package com.archecraft.yaes.vulkan

import com.archecraft.games.yaes.lib.util.orThrow
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.system.NativeResource
import org.lwjgl.util.shaderc.Shaderc.*
import org.lwjgl.vulkan.VK10.*
import java.net.URI
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Paths


fun compileShaderFile(shaderFile: String, shaderKind: ShaderKind) = compileShaderAbsoluteFile(ClassLoader.getSystemResource(shaderFile).toExternalForm(), shaderKind)

fun compileShaderAbsoluteFile(shaderFile: String, shaderKind: ShaderKind): SPIRV {
    val source = Files.readString(Paths.get(URI(shaderFile)))
    return compileShader(shaderFile, source, shaderKind)
}

fun compileShader(filename: String, source: String, shaderKind: ShaderKind): SPIRV {
    val compiler = shaderc_compiler_initialize()
    if (compiler == NULL) throw VulkanException("Failed to create shader compiler!")
    
    val result = shaderc_compile_into_spv(compiler, source, shaderKind.shadercShaderKind, filename, "main", NULL)
    if (result == NULL) throw VulkanException("Failed to compile $filename into SPIR_V!")
    if (shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success) throw VulkanException("""
        Failed to compile shader $filename into SPIR-V:
            ${shaderc_result_get_error_message(result)}
    """.trimIndent())
    
    shaderc_compiler_release(compiler)
    return SPIRV(result, shaderc_result_get_bytes(result) ?: throw VulkanException("Failed to retrieve compiled SPIR-V for $filename"), shaderKind)
}


enum class ShaderKind(val shadercShaderKind: Int, val vulkanShaderStageBit: Int) {
    VERTEX_SHADER(shaderc_glsl_vertex_shader, VK_SHADER_STAGE_VERTEX_BIT),
    GEOMETRY_SHADER(shaderc_glsl_geometry_shader, VK_SHADER_STAGE_GEOMETRY_BIT),
    FRAGMENT_SHADER(shaderc_glsl_fragment_shader, VK_SHADER_STAGE_FRAGMENT_BIT)
}

class SPIRV(val handle: Long, bytecode: ByteBuffer, val kind: ShaderKind) : NativeResource {
    private var _bytecode: ByteBuffer? = bytecode
    val bytecode get() = _bytecode.orThrow()
    
    override fun free() {
        shaderc_result_release(handle)
        _bytecode = null
    }
}