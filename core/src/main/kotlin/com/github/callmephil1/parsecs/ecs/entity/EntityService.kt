package com.github.callmephil1.parsecs.ecs.entity

import com.github.callmephil1.parsecs.ecs.collections.MutableBag
import com.github.callmephil1.parsecs.ecs.collections.Pool
import io.github.oshai.kotlinlogging.KotlinLogging

class EntityService internal constructor() {
    private val logger = KotlinLogging.logger {}

    internal val entities = MutableBag<Entity>()
    private val pool = Pool(Entity::class.java)

    fun clear() {
        entities.clear()
        pool.clear()
    }

    val entityCount get() = entities.count

    fun get(index: Int) = entities[index]

    internal fun obtain(): Entity {
        val entity = pool.obtain()
        entity.index = entities.count
        entities.add(entity)
        return entity
    }

    fun release(entity: Entity): Entity
    {
        val movedEntity = entities.removeAt(entity.index)!!
        pool.release(entity)
        return movedEntity
    }

    fun release(entityRef: EntityRef) = release(entityRef.entity)
}