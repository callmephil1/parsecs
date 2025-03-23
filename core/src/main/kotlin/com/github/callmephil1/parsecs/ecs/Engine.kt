package com.github.callmephil1.parsecs.ecs

import com.github.callmephil1.parsecs.EventBus
import com.github.callmephil1.parsecs.ecs.component.ComponentService
import com.github.callmephil1.parsecs.ecs.entity.EntityService
import com.github.callmephil1.parsecs.ecs.system.*
import com.github.callmephil1.parsecs.ecs.system.SystemService
import java.lang.System as JSystem

class Engine internal constructor(
    private val di: DependencyInjection,
    private val entityService: EntityService,
    private val eventBus: EventBus,
    private val systemService: SystemService
){
    private val stopWatch = StopWatch()
    private val logger = JSystem.getLogger(Engine::class.qualifiedName)

    fun update(delta: Float) {
        systemService.engineLoopStart()

        eventBus.processEvents()

        entityService.releaseEntities()
        entityService.updateEntities()
        entityService.createEntities()

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

    class Builder {
        private val diBuilder = DependencyInjection.Builder()
        private val systemServiceConfig = SystemService.Configuration()

        fun build(): Engine {
            diBuilder.singleton<EntityService>()
            diBuilder.singleton<EventBus>()
            diBuilder.singleton<ComponentService>()

            systemServiceConfig.systems.forEach {
                diBuilder.singleton(it)
            }

            val di = diBuilder.build()

            val systemService = if (systemServiceConfig.asynchronous) {
                AsynchronousSequentialSystemService.Builder()
                    .build(systemServiceConfig.systems.map {
                        di.get(it) as System
                    })
            } else {
                SynchronousSystemService.Builder()
                    .build(systemServiceConfig.systems.map {
                        di.get(it) as System
                    })
            }

            val engine = Engine(
                di = di,
                entityService = di.get(),
                eventBus = di.get(),
                systemService = systemService
            )

            systemService.engineInitialized()

            return engine
        }

        fun services(configure: DependencyInjection.Builder.() -> Unit) {
            diBuilder.configure()
        }

        fun systems(configure: SystemService.Configuration.() -> Unit) {
            systemServiceConfig.configure()
        }
    }
}

fun engine(configure: Engine.Builder.() -> Unit): Engine {
    val builder = Engine.Builder()
    builder.configure()
    return builder.build()
}