package com.github.callmephil1.parsecs.ecs.entity

import com.github.callmephil1.parsecs.ecs.collections.Bag
import com.github.callmephil1.parsecs.ecs.collections.Pool
import com.github.callmephil1.parsecs.ecs.component.ComponentService
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.*

class EntityService internal constructor(
    private val componentService: ComponentService
) {

    private val logger = KotlinLogging.logger {}

    private val createdEntities = Stack<Entity>()
    private val families = mutableListOf<Family>()
    private val updatedEntities = Stack<Entity>()
    private val releasedEntities = Stack<Entity>()

    private val entityBag = Bag<Entity>()
    private val pool = Pool(Entity::class.java)

    internal fun addFamily(family: Family) = this.families.add(family)

    internal fun createEntities() {
        while (!createdEntities.empty()) {
            val entity = createdEntities.pop()

            for (i in families.indices) {
                val family = families[i]
                if (family.isInterested(entity)) {
                    logger.debug { "Adding $entity to $family" }
                    family.add(entity)
                }
            }
        }
    }

    fun family(configure: Family.Builder.() -> Unit): Family {
        val builder = Family.Builder(componentService = componentService, entityService = this)
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

        logger.debug { "Obtained new entity $entity" }

        return entity
    }

    fun release(entity: Entity) = releasedEntities.push(entity)

    fun release(entityRef: EntityRef) = release(entityRef.entity)

    internal fun releaseEntities() {
        while(!releasedEntities.empty()) {
            val entity = releasedEntities.pop()

            logger.debug { "Releasing entity $entity" }

            if (entity.index == -1)
                continue

            for (i in families.indices) {
                val family = families[i]
                if (family.has(entity)) {
                    logger.debug { "Removing $entity from $family" }
                    family.remove(entity)
                }
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

            for(i in families.indices) {
                val family = families[i]

                if (family.has(entity)) {
                    if (family.isInterested(entity)) {
                        family.update(entity)
                    } else {
                        family.remove(entity)
                    }
                } else if (family.isInterested(entity)) {
                    family.add(entity)
                }
            }
        }
    }
}