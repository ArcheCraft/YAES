package com.archecraft.yaes.vulkan

import mu.KotlinLogging
import org.joml.Vector2fc

import org.joml.Vector3fc

import org.joml.Vector2f

import org.joml.Vector3f

import org.lwjgl.assimp.*

import org.lwjgl.assimp.Assimp.aiGetErrorString
import org.lwjgl.assimp.Assimp.aiImportFile

import java.io.File


val logger = KotlinLogging.logger {}

fun loadModel(file: File, flags: Int): Model {
    aiImportFile(file.absolutePath, flags).use { scene ->
        logger.info("Loading model ${file.path}...")
        if (scene?.mRootNode() == null) {
            throw VulkanException("Could not load model: ${aiGetErrorString()}")
        }
        
        val model = Model()
        val startTime = System.nanoTime()
        processNode(scene.mRootNode()!!, scene, model)
        
        logger.info("Model loaded in ${(System.nanoTime() - startTime) / 1e6}ms")
        return model
    }
}

private fun processNode(node: AINode, scene: AIScene, model: Model) {
    if (node.mMeshes() != null) {
        processNodeMeshes(scene, node, model)
    }
    if (node.mChildren() != null) {
        val children = node.mChildren()
        for (i in 0 until node.mNumChildren()) {
            processNode(AINode.create(children!![i]), scene, model)
        }
    }
}

private fun processNodeMeshes(scene: AIScene, node: AINode, model: Model) {
    val meshes = scene.mMeshes()!!
    val meshIndices = node.mMeshes()!!
    for (i in 0 until meshIndices.capacity()) {
        val mesh = AIMesh.create(meshes[meshIndices[i]])
        processMesh(mesh, model)
    }
}

private fun processMesh(mesh: AIMesh, model: Model) {
    processPositions(mesh, model.positions)
    processTexCoords(mesh, model.texCoords)
    processIndices(mesh, model.indices)
}

private fun processPositions(mesh: AIMesh, positions: MutableList<Vector3f>) {
    val vertices = mesh.mVertices()
    for (i in 0 until vertices.capacity()) {
        val position = vertices[i]
        positions.add(Vector3f(position.x(), position.y(), position.z()))
    }
}

private fun processTexCoords(mesh: AIMesh, texCoords: MutableList<Vector2f>) {
    val aiTexCoords = mesh.mTextureCoords(0)!!
    for (i in 0 until aiTexCoords.capacity()) {
        val coords = aiTexCoords[i]
        texCoords.add(Vector2f(coords.x(), coords.y()))
    }
}

private fun processIndices(mesh: AIMesh, indices: MutableList<Int>) {
    val aiFaces = mesh.mFaces()
    for (i in 0 until mesh.mNumFaces()) {
        val face = aiFaces[i]
        val pIndices = face.mIndices()
        for (j in 0 until face.mNumIndices()) {
            indices.add(pIndices[j])
        }
    }
}


class Model {
    val positions: MutableList<Vector3f> = mutableListOf()
    val texCoords: MutableList<Vector2f> = mutableListOf()
    val indices: MutableList<Int> = mutableListOf()
}