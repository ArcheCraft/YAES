package com.archecraft.yaes.libs.opensimplex

import opensimplex.OpenSimplex2F
import opensimplex.OpenSimplex2S
import kotlin.random.Random

class OpenSimplexNoise(octaveCount: Int, persistance: Double, lacuranity: Double, private val scale: Double, seed: Long, mode: OpenSimplexMode) {
    enum class OpenSimplexMode {
        SMOOTH, FAST
    }
    
    private class Octave(private val frequency: Double, private val amplitude: Double, seed: Long, private val mode: OpenSimplexMode) {
        val noiseFast = OpenSimplex2F(seed)
        val noiseSmooth = OpenSimplex2S(seed)
        
        fun get2D(x: Double, y: Double): Double {
            val sample = when (mode) {
                OpenSimplexMode.FAST   -> noiseFast.noise2(x * frequency, y * frequency)
                OpenSimplexMode.SMOOTH -> noiseSmooth.noise2(x * frequency, y * frequency)
            }
            
            return sample * amplitude
        }
        
        fun get3D(x: Double, y: Double, z: Double): Double {
            val sample = when (mode) {
                OpenSimplexMode.FAST   -> noiseFast.noise3_XZBeforeY(x * frequency, y * frequency, z * frequency)
                OpenSimplexMode.SMOOTH -> noiseSmooth.noise3_XZBeforeY(x * frequency, y * frequency, z * frequency)
            }
            
            return sample * amplitude
        }
        
        fun get4D(x: Double, y: Double, z: Double, w: Double): Double {
            val sample = when (mode) {
                OpenSimplexMode.FAST   -> noiseFast.noise4_XZBeforeYW(x * frequency, y * frequency, z * frequency, w * frequency)
                OpenSimplexMode.SMOOTH -> noiseSmooth.noise4_XZBeforeYW(x * frequency, y * frequency, z * frequency, w * frequency)
            }
            
            return sample * amplitude
        }
    }
    
    private val octaves: MutableList<Octave> = mutableListOf()
    
    init {
        var frequency = 1.0
        var amplitude = 1.0
        
        val prng = Random(seed)
        
        for (i in 0 until octaveCount) {
            octaves.add(Octave(frequency, amplitude, prng.nextLong(-100_000, 100_000), mode))
            
            frequency *= lacuranity
            amplitude *= persistance
        }
    }
    
    fun sample2D(x: Double, y: Double): Double {
        var noiseHeight = 0.0
        
        for (layer in octaves) {
            noiseHeight += layer.get2D(x / scale, y / scale)
        }
        
        return noiseHeight
    }
    
    fun sample3D(x: Double, y: Double, z: Double): Double {
        var noiseHeight = 0.0
        
        for (layer in octaves) {
            noiseHeight += layer.get3D(x / scale, y / scale, z / scale)
        }
        
        return noiseHeight
    }
    
    fun sample4D(x: Double, y: Double, z: Double, w: Double): Double {
        var noiseHeight = 0.0
        
        for (layer in octaves) {
            noiseHeight += layer.get4D(x / scale, y / scale, z / scale, w / scale)
        }
        
        return noiseHeight
    }
}