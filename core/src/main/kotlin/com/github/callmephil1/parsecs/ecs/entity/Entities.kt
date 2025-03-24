package com.github.callmephil1.parsecs.ecs.entity

import com.github.callmephil1.parsecs.ecs.collections.Bag
import com.github.callmephil1.parsecs.ecs.collections.Bits
import com.github.callmephil1.parsecs.ecs.component.ComponentService
import kotlinx.coroutines.*

typealias EntitiesListener = (Entity) -> Unit

class Entities internal constructor(
    private val allMask: Bits?,
    private val anyMask: Bits?,
    private val noneMask: Bits?,
    private var entityAdded: EntitiesListener?,
    private var entityRemoved: EntitiesListener?,
    private var entityUpdated: EntitiesListener?,
    val name: String
) : Iterable<Entity> {

    private val _entities = Bag<Entity>()
    val entities = _entities.toReadOnly()
    private val scope = CoroutineScope(Dispatchers.Default)

    internal fun add(entity: Entity) {
        _entities.add(entity)
        entityAdded?.invoke(entity)
    }

    internal fun isInterested(entityBits: Bits) = (allMask == null || entityBits.contains(allMask)) &&
            (anyMask == null || entityBits.intersects(anyMask)) &&
            (noneMask == null || !entityBits.intersects(noneMask))

    override fun iterator(): Iterator<Entity> = _entities.iterator() as Iterator<Entity>

    fun parallelForEach(block: (Entity) -> Unit) = runBlocking {
        val chunkSize = _entities.count / chunkCount
        val jobs = ArrayList<Job>(chunkCount)

        for(i in 0 ..< chunkCount - 1) {
            jobs.add(scope.launch {
                for(idx in i * chunkSize ..< (i + 1) * chunkSize) {
                    val entity = _entities[idx]
                    if (entity != null)
                        block(entity)
                }
            })
        }

        jobs.add(scope.launch {
            for(idx in (chunkCount - 1) * chunkSize ..< _entities.count) {
                val entity = _entities[idx]
                if (entity != null)
                    block(entity)
            }
        })

        jobs.joinAll()
    }

    internal fun remove(entity: Entity) {
        _entities.remove(entity)
        entityRemoved?.invoke(entity)
    }

    internal fun update(entity: Entity) {
        entityUpdated?.invoke(entity)
    }

    class Builder internal constructor(
        private val componentService: ComponentService,
        private val entityService: EntityService
    ) {
        private var allMask: Bits? = null
        private var anyMask: Bits? = null
        private var entityAdded: EntitiesListener? = null
        private var entityRemoved: EntitiesListener? = null
        private var entityUpdated: EntitiesListener? = null
        private var noneMask: Bits? = null
        var name: String = ""

        fun build(): Entities {
            val newEntities = Entities(allMask, anyMask, noneMask, entityAdded, entityRemoved, entityUpdated, name)
            entityService.addEntities(newEntities)
            return newEntities
        }

        fun all(vararg components: Class<*>) {
            if (allMask == null)
                allMask = Bits()

            for (i in components.indices) {
                val index = componentService.getComponentIndex(components[i])
                allMask!!.setBit(index, true)
            }
        }

        fun any(vararg components: Class<*>) {
            if (anyMask == null)
                anyMask = Bits()

            for (i in components.indices) {
                val index = componentService.getComponentIndex(components[i])
                anyMask!!.setBit(index, true)
            }
        }

        fun entityAdded(listener: EntitiesListener) {
            entityAdded = listener
        }

        fun entityRemoved(listener: EntitiesListener) {
            entityRemoved = listener
        }

        fun entityUpdated(listener: EntitiesListener) {
            entityUpdated = listener
        }

        fun none(vararg components: Class<*>) {
            if (noneMask == null)
                noneMask = Bits()

            for (i in components.indices) {
                val index = componentService.getComponentIndex(components[i])
                noneMask!!.setBit(index, true)
            }
        }
    }

    companion object {
        var chunkCount = 4
    }
}