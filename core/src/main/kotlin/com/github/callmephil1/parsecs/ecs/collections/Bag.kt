package com.github.callmephil1.parsecs.ecs.collections

class Bag<T>(initialSize: Int = 1) : Iterable<T?> {

    val capacity get() = data.size
    var count: Int = 0
        private set
    private var data: Array<Any?> = Array(initialSize) { null }

    fun add(value: T?) {
        ensureCapacity(count + 1)
        data[count++] = value
    }

    private fun ensureCapacity(target: Int) {
        if (target >= data.size) {
            val newData = Array<Any?>((target * 1.5).toInt() + 1) { null }
            data.copyInto(newData)
            data = newData
        }
    }

    operator fun get(index: Int): T? = data[index] as T?

    fun indexOf(item: T?): Int {
        for (i in 0 ..< count) {
            if(item === data[i])
                return i
        }
        return -1
    }

    override fun iterator(): Iterator<T?> = BagIterator(this)

    fun move(from: Int, to: Int): T? {
        ensureCapacity(to)

        if (from >= count) {
            data[to] = null
            return null

        } else {
            val movingItem = data[from]
            data[to] = movingItem
            data[from] = null
            return movingItem as T?
        }
    }

    fun remove(element: T): T? {
        val index = indexOf(element)
        if (index == -1)
            return null

        return removeAt(index)
    }

    fun removeAt(index: Int): T? {
        val element = move(count - 1, index)
        count--
        return element
    }

    operator fun set(index: Int, value: T?) {
        ensureCapacity(index)
        if (index >= count)
            count = index + 1
        data[index] = value
    }

    inner class BagIterator(
        private val bag: Bag<T>
    ) : Iterator<T?> {

        private var index: Int = -1

        override fun hasNext() = ++index < bag.count

        override fun next(): T? = bag[index]
    }
}
