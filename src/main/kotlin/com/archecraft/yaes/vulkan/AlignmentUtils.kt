package com.archecraft.yaes.vulkan

import org.joml.Vector4f

import org.joml.Matrix4f

import org.joml.Vector3f

import org.joml.Vector2f

import java.util.HashMap
import kotlin.reflect.KClass


fun sizeof(obj: Any?): Int {
    return if (obj == null) 0 else SIZEOF_CACHE.getOrDefault(obj.javaClass, 0)
}

fun alignof(obj: Any?): Int {
    return if (obj == null) 0 else ALIGNOF_CACHE.getOrDefault(obj.javaClass, Integer.BYTES)
}

fun alignas(offset: Int, alignment: Int): Int {
    return if (offset % alignment == 0) offset else (offset - 1 or alignment - 1) + 1
}

private val SIZEOF_CACHE: MutableMap<Class<*>, Int> by lazy {
    val map = mutableMapOf<Class<*>, Int>()
    
    map[Byte::class.java] = Byte.SIZE_BYTES
    map[Char::class.java] = Char.SIZE_BYTES
    map[Short::class.java] = Short.SIZE_BYTES
    map[Int::class.java] = Int.SIZE_BYTES
    map[Float::class.java] = Float.SIZE_BYTES
    map[Long::class.java] = Byte.SIZE_BYTES
    map[Double::class.java] = Double.SIZE_BYTES
    
    map[Vector2f::class.java] = 2 * Float.SIZE_BYTES
    map[Vector3f::class.java] = 3 * Float.SIZE_BYTES
    map[Vector4f::class.java] = 4 * Float.SIZE_BYTES
    map[Matrix4f::class.java] = 16 * Float.SIZE_BYTES
    
    map
}

private val ALIGNOF_CACHE: MutableMap<Class<*>, Int> by lazy {
    val map = SIZEOF_CACHE.toMutableMap()
    
    map[Matrix4f::class.java] = map[Vector4f::class.java]!!
    
    map
}