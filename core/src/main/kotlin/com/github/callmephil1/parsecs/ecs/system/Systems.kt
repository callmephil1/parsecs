package com.github.callmephil1.parsecs.ecs.system

import com.github.callmephil1.parsecs.ecs.collections.DirectedAcyclicGraph
import com.github.callmephil1.parsecs.ecs.system.dsl.System as SystemDsl

class Systems internal constructor(
    val monitorSystems: MutableSet<Class<*>>,
    val renderSystems: MutableSet<Class<*>>,
    val updateExecutionSteps: List<List<Class<*>>>
) {
    class Builder internal constructor() {
        private val renderSystems: MutableSet<Class<*>> = mutableSetOf()
        private val updateDag = DirectedAcyclicGraph<Class<*>>()

        infix fun <T : UpdateSystem> add(system: Class<T>) = system.apply {
            updateDag.addWithDependents(system, emptyList())
        }
        inline fun <reified T : UpdateSystem> add() = this.add(T::class.java)

        infix fun <T : UpdateSystem> Class<T>.dependentOf(parents: List<Class<T>>) = this.apply {
            parents.forEach { it.dependsOn(listOf(this)) }
        }
        infix fun <T : UpdateSystem> SystemDsl<T>.dependentOf(parents: List<SystemDsl<T>>) = this.clazz.dependsOn(parents.map { it.clazz })
        infix fun <T : UpdateSystem> Class<T>.dependsOn(dependencies: List<Class<T>>) = this.apply {
            updateDag.addWithDependents(this, dependencies)
        }
        infix fun <T : UpdateSystem> SystemDsl<T>.dependsOn(dependencies: List<SystemDsl<T>>) = this.clazz.dependsOn(dependencies.map { it.clazz })

        infix fun <T : MonitorSystem> monitorSystems(systems: List<Class<T>>) {

        }

        infix fun <T : RenderSystem> renderSystems(systems: List<Class<T>>) {
            renderSystems.addAll(systems)
        }

        fun build(): Systems {
            return Systems(
                monitorSystems = mutableSetOf(),
                renderSystems = renderSystems,
                updateExecutionSteps = updateDag.toExecutionSteps()
            )
        }
    }
}


