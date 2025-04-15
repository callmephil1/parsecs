package com.github.callmephil1.parsecs.ecs.world

import com.github.callmephil1.parsecs.EventBus
import com.github.callmephil1.parsecs.ecs.di.DependencyInjection
import com.github.callmephil1.parsecs.ecs.StopWatch
import com.github.callmephil1.parsecs.ecs.component.ComponentService
import com.github.callmephil1.parsecs.ecs.dsl.Component
import com.github.callmephil1.parsecs.ecs.entity.Entity
import com.github.callmephil1.parsecs.ecs.entity.EntityService
import com.github.callmephil1.parsecs.ecs.family.Family
import com.github.callmephil1.parsecs.ecs.family.FamilyService
import com.github.callmephil1.parsecs.ecs.system.AsynchronousSequentialSystemService
import com.github.callmephil1.parsecs.ecs.system.SynchronousSystemService
import com.github.callmephil1.parsecs.ecs.system.System
import com.github.callmephil1.parsecs.ecs.system.SystemService
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.*

class World(
    private val componentService: ComponentService,
    private val entityService: EntityService,
    private val eventBus: EventBus,
    private val familyService: FamilyService,
    val name: String
) {
    private lateinit var systemService: SystemService

    private val logger = KotlinLogging.logger {  }
    private val stopWatch = StopWatch()

    private val createdEntities = Stack<Entity>()
    private val updatedEntities = Stack<Entity>()
    private val releasedEntities = Stack<Entity>()
    private val newFamilies = Stack<Family>()

    val entities get() = entityService.entities
    val systems get() = systemService.systems

    fun entity(configure: Entity.() -> Unit): Entity {
        val newEntity = entityService.obtain()
        createdEntities.push(newEntity)
        newEntity.configure()
        return newEntity
    }

    private fun createEntities() {
        while (!createdEntities.empty()) {
            val entity = createdEntities.pop()
            familyService.addEntity(entity)
        }
    }

    private fun createFamilies() {
        while (!newFamilies.empty()) {
            val family = newFamilies.pop()
            entities.forEach {
                if (family.isInterested(it!!))
                    family.add(it)
            }
        }
    }

    internal fun family(configure: Family.Builder.() -> Unit): Family {
        val newFamily = familyService.family(configure)
        newFamilies.push(newFamily)
        return newFamily
    }

    fun <T> getMapper(index: Int) = componentService.getMapper<T>(index)

    fun <T> getMapper(clazz: Class<T>) = componentService.getMapper(clazz)

    fun <T> getMapper(component: Component<T>) = componentService.getMapper(component)

    internal fun initialize(systemService: SystemService) {
        this.systemService = systemService
        systemService.engineInitialized()
    }

    private fun releaseEntities() {
        while(!releasedEntities.empty()) {
            val entity = releasedEntities.pop()

            if (entity.index == -1)
                continue

            val movedEntity = entityService.release(entity)
            componentService.release(entity, movedEntity)
            familyService.release(entity, movedEntity)

            movedEntity.index = entity.index

            entity.index = -1
            entity.tag = ""
            entity.componentMask.setAll(false)
            entity.version += 1u
        }
    }

    fun update(entity: Entity) {
        updatedEntities.push(entity)
    }

    fun update(delta: Float) {
        systemService.engineLoopStart()

        eventBus.processEvents()

        releaseEntities()
        updateEntities()
        createEntities()
        createFamilies()

        systemService.updateSystemLoop(delta)

        systemService.renderSystemLoop(delta)

        systemService.engineLoopEnd()
    }

    fun update() {
        val deltaTime = stopWatch.seconds
        stopWatch.start()
        update(deltaTime)
        stopWatch.stop()
    }

    private fun updateEntities() {
        while(!updatedEntities.empty()) {
            val entity = updatedEntities.pop()

            if (entity.index == -1)
                continue

            familyService.update(entity)
        }
    }

    class Builder {
        private val diBuilder = DependencyInjection.Builder()
        private var isAsyncSystems = false
        private val systems: MutableList<Class<*>> = mutableListOf()

        var name: String = ""

        fun asyncSystems(vararg system: Class<*>) {
            isAsyncSystems = true
            systems.clear()
            systems.addAll(system)
        }

        fun build(): World {
            diBuilder.singleton<ComponentService>()
            diBuilder.singleton<EntityService>()
            diBuilder.singleton<EventBus>()
            diBuilder.singleton<FamilyService>()

            systems.forEach {
                diBuilder.singleton(it)
            }

            var world: World? = null

            diBuilder.scoped<World> {
                return@scoped world!!
            }

            val di = diBuilder.build()

            world = World(
                componentService = di.get(),
                entityService = di.get(),
                eventBus = di.get(),
                familyService = di.get(),
                name
            )

            if (name.isNotBlank())
                worlds[name] = world

            val systemService = if (isAsyncSystems) {
                AsynchronousSequentialSystemService.Builder()
                    .build(systems.map {
                        di.get(it) as System
                    })
            } else {
                SynchronousSystemService.Builder()
                    .build(systems.map {
                        di.get(it) as System
                    })
            }

            world.initialize(systemService)

            return world
        }

        fun services(configure: DependencyInjection.Builder.() -> Unit) {
            diBuilder.configure()
        }

        fun systems(vararg system: Class<*>) {
            isAsyncSystems = false
            systems.clear()
            systems.addAll(system)
        }
    }

    companion object {
        private val worlds = mutableMapOf<String, World>()

        fun get(name: String) = worlds[name]!!
        fun getOrNull(name: String) = worlds[name]
    }
}

fun world(configure: World.Builder.() -> Unit): World {
    val builder = World.Builder()
    builder.configure()
    return builder.build()
}