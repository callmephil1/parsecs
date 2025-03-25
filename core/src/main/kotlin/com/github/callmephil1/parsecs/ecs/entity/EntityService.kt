package com.github.callmephil1.parsecs.ecs.entity

import com.github.callmephil1.parsecs.ecs.collections.Bag
import com.github.callmephil1.parsecs.ecs.collections.Pool
import com.github.callmephil1.parsecs.ecs.component.ComponentService
import java.util.*

class EntityService internal constructor(
    private val componentService: ComponentService
) {

    private val createdEntities = Stack<Entity>()
    private val entities = mutableListOf<Entities>()
    private val updatedEntities = Stack<Entity>()
    private val releasedEntities = Stack<Entity>()

    private val entityBag = Bag<Entity>()
    private val pool = Pool(Entity::class.java)

    internal fun addEntities(entities: Entities) = this.entities.add(entities)

    internal fun createEntities() {
        while (!createdEntities.empty()) {
            val entity = createdEntities.pop()

            for (i in entities.indices) {
                if (entities[i].isInterested(entity))
                    entities[i].add(entity)
            }
        }
    }

    fun entities(configure: Entities.Builder.() -> Unit): Entities {
        val builder = Entities.Builder(componentService = componentService, entityService = this)
        configure(builder)
        return builder.build()
    }

    fun entity(configure: Entity.() -> Unit): Entity {
        val newEntity = obtain()
        createdEntities.add(newEntity)
        configure(newEntity)
        return newEntity
    }

    val entityCount get() = entityBag.count

    fun get(index: Int) = entityBag[index]

    internal fun obtain(): Entity {
        val entity = pool.obtain()
        entity.index = entityBag.count
        entityBag.add(entity)
        return entity
    }

    fun release(entity: Entity) = releasedEntities.push(entity)

    internal fun releaseEntities() {
        while(!releasedEntities.empty()) {
            val entity = releasedEntities.pop()

            if (entity.index == -1)
                continue

            for (i in entities.indices) {
                if (entities[i].isInterested(entity))
                    entities[i].remove(entity)
            }

            val movedEntity = entityBag.removeAt(entity.index)
            if (movedEntity != null) {
                componentService.release(entity, movedEntity)
                movedEntity.index = entity.index
            }

            entity.index = -1
            entity.componentMask.setAll(false)
            entity.version += 1u
            pool.release(entity)
        }
    }

    fun update(entity: Entity) { updatedEntities.push(entity) }

    internal fun updateEntities() {
        while(!updatedEntities.empty()) {
            val entity = updatedEntities.pop()

            if (entity.index == -1)
                continue

            for(i in entities.indices){
                if (entities[i].isInterested(entity))
                    entities[i].update(entity)
            }
        }
    }
}