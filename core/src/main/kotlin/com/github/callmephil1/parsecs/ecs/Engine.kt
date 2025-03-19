package com.github.callmephil1.parsecs.ecs

import com.github.callmephil1.parsecs.ecs.component.ComponentService
import com.github.callmephil1.parsecs.ecs.entity.EntityService
import com.github.callmephil1.parsecs.ecs.system.*
import com.github.callmephil1.parsecs.ecs.system.SystemService
import java.lang.System as JSystem

class Engine internal constructor(
    private val di: DependencyInjection,
    private val entityService: EntityService,
    private val systemService: SystemService
){
    private val stopWatch = StopWatch()
    private val logger = JSystem.getLogger(Engine::class.qualifiedName)

    fun update(delta: Float) {
        systemService.engineLoopStart()

        val test = Engine::class.  .constructors.forEach {

            it.
        }

        entityService.releaseEntities()
        entityService.updateEntities()
        entityService.createEntities()

        systemService.updateSystemLoop(delta)

        systemService.renderSystemLoop(delta)

        systemService.engineLoopEnd()
    }

    fun update() {
        val deltaTime = stopWatch.seconds / 1000
        stopWatch.start()
        update(deltaTime)
        stopWatch.stop()
    }

    class Builder {
        private val diBuilder = DependencyInjection.Builder()
        private val systemServiceConfig = SystemService.Configuration()

        fun build(): Engine {
            diBuilder.singleton(EntityService::class.java)
            diBuilder.singleton(ComponentService::class.java)

            systemServiceConfig.systems.forEach {
                diBuilder.singleton(it)
            }

            val di = diBuilder.build()
            val entityService = di.get(EntityService::class.java)

            val systemService = if (systemServiceConfig.asynchronous) {
                AsynchronousDAGSystemService.Builder()
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
                di,
                entityService,
                systemService
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