package com.github.callmephil1.parsecs.ecs.collections

import kotlin.math.min

class Bits(
    initialSize: Int = 1
) {

    private var chunks: Array<Int>
    private var count = 0

    init {
        chunks = Array(initialSize) { 0 }
    }

    fun clear() = setAll(false)

    fun clone(): Bits {
        val newBits = Bits(chunks.size * Int.SIZE_BITS)
        chunks.copyInto(newBits.chunks)
        return newBits
    }

    fun contains(mask: Bits): Boolean {
        val maxSize = if (chunks.size >= mask.chunks.size) chunks.size else mask.chunks.size

        for (i in 0 ..< maxSize) {
            val maskChunk = if (i < mask.chunks.size) mask.chunks[i] else 0
            val thisChunk = if (i < chunks.size) chunks[i] else 0

            if ((thisChunk and maskChunk) != maskChunk)
                return false
        }
        return true
    }

    private fun ensureCapcity(target: Int) {
        if (target <= count)
            return

        val newCount = (target / 32) + 1
        count = 32 * newCount
        val newArray = Array(newCount) { 0 }
        chunks.copyInto(newArray)
        chunks = newArray
    }

    operator fun get(index: Int) = getBit(index)

    fun getBit(index: Int): Boolean {
        if (index > chunks.size * Int.SIZE_BITS)
            return false

        val chunkIndex = getChunkIndex(index)
        val chunk = chunks[chunkIndex]
        val mask = 1 shl (index) % Int.SIZE_BITS
        return (chunk and mask) != 0
    }

    fun getSetIndicies(): List<Int> {
        val indices = mutableListOf<Int>()

        for(i in 0 ..< count) {
            if (get(i)) {
                indices.add(i)
            }
        }

        return indices
    }

    private fun getChunkIndex(index: Int) = index / (Int.SIZE_BITS)

    fun intersects(other: Bits): Boolean {
        val end = min(chunks.size, other.chunks.size) - 1
        for(i in 0 .. end) {
            if (chunks[i] and other.chunks[i] != 0)
                return true
        }
        return false
    }

    fun isEmpty(): Boolean {
        for (i in 0 .. chunks.size) {
            if (chunks[i] != 0)
                return false
        }
        return true
    }

    infix fun or(other: Bits): Bits {
        return if (chunks.size >= other.chunks.size)
            or(this, other)
        else
            or(other, this)
    }

    private fun or(larger: Bits, small: Bits): Bits {
        val newBits = larger.clone()

        for(i in 0 ..< small.chunks.size) {
            newBits.chunks[i] = small.chunks[i] or larger.chunks[i]
        }

        return newBits
    }

    operator fun set(index: Int, value: Boolean) = setBit(index, value)

    fun setAll(value: Boolean) {
        val chunk = if (value) Int.MAX_VALUE else 0

        for (i in chunks.indices)
            chunks[i] = chunk
    }

    fun setBit(index: Int, value: Boolean) {
        ensureCapcity(index)

        val chunkIndex = getChunkIndex(index)
        var chunk = chunks[chunkIndex]
        val mask = 1 shl (index % Int.SIZE_BITS)

        chunk = if (value)
            chunk or mask
        else
            chunk xor mask

        chunks[chunkIndex] = chunk
    }
}