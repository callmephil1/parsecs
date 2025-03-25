package com.github.callmephil1.parsecs.ecs.component

import com.github.callmephil1.parsecs.ecs.collections.Bag
import com.github.callmephil1.parsecs.ecs.collections.Pool
import com.github.callmephil1.parsecs.ecs.entity.Entity
import com.github.callmephil1.parsecs.ecs.entity.EntityRef

class ComponentMapper<T> internal constructor(
    val clazz: Class<T>,
    val index: Int
) {

    private val components = Bag<T>()
    private val pool: Pool<T> = Pool(clazz)

    operator fun get(index: Int) = components[index]!!

    operator fun get(entity: Entity) = get(entity.index)

    operator fun get(entityRef: EntityRef) = get(entityRef.entity.index)

    fun getOrNull(index: Int) = components[index]

    fun getOrNull(entity: Entity) = components[entity.index]

    fun getOrNull(entityRef: EntityRef) = components[entityRef.entity.index]

    fun has(entity: Entity) = entity.componentMask.getBit(index)

    fun has(entityRef: EntityRef) = has(entityRef.entity)

    fun obtain() = pool.obtain()

    internal fun release(entity: Int, with: Int) {
        if (entity < components.count)
            pool.release(components[entity])

        components.move(with, entity)
    }

    internal fun release(entity: Entity, with: Entity) = release(entity.index, with.index)

    fun remove(entity: Entity) {
        val component = components.removeAt(entity.index)
        pool.release(component)
    }

    operator fun set(entity: Entity, value: T) {
        components[entity.index] = value
        entity.componentMask.setBit(index, true)
    }
}