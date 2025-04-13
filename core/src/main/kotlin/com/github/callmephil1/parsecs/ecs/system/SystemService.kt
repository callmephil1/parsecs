package com.github.callmephil1.parsecs.ecs.system

import com.github.callmephil1.parsecs.ecs.ds.DirectedAcyclicGraph
import kotlinx.coroutines.*

interface SystemService {
    fun engineInitialized()
    fun engineLoopEnd()
    fun engineLoopStart()
    fun renderSystemLoop(delta: Float)
    fun updateSystemLoop(delta: Float)

    class Configuration {
        var asynchronous: Boolean = false
        internal var systems: List<Class<*>> = emptyList()

        fun systems(vararg system: Class<*>) {
            systems = system.toList()
        }
    }
}

internal class AsynchronousSystemService(
    private val monitorSystems: List<MonitorSystem>,
    private val renderSystems: List<RenderSystem>,
    private val updateSystemDependencyGraph: Map<AsynchronousUpdateSystem, Set<AsynchronousUpdateSystem>>,
    private val updateExecutionSteps: List<List<AsynchronousUpdateSystem>>
) : SystemService {
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun engineInitialized() {
        mutableSetOf<System>().also {
            it.addAll(monitorSystems)
            it.addAll(renderSystems)
            it.addAll(updateSystemDependencyGraph.keys)
        }.forEach {
            it.engineInitialized()
        }
    }

    override fun engineLoopEnd() {
        monitorSystems.forEach { it.engineLoopEnd() }
    }

    override fun engineLoopStart() {
        monitorSystems.forEach { it.engineLoopStart() }
    }

    override fun renderSystemLoop(delta: Float) {
        renderSystems.forEach { renderer ->
            monitorSystems.forEach { it.systemLoopStart(renderer) }
            renderer.draw(delta)
            monitorSystems.forEach { it.systemLoopEnd(renderer) }
        }
    }

    override fun updateSystemLoop(delta: Float) {
        val jobs = ArrayList<Job>(updateSystemDependencyGraph.keys.size)
        val systemJobs = mutableMapOf<AsynchronousUpdateSystem, Job>()

        for (step in updateExecutionSteps) {
            for (system in step) {
                systemJobs[system] = scope.launch {
                    updateSystemDependencyGraph[system]!!.map { systemJobs[it]!! }.joinAll()
                    monitorSystems.forEach { it.systemLoopStart(system) }
                    system.update(delta)
                    monitorSystems.forEach { it.systemLoopEnd(system) }
                }
            }
        }

        runBlocking {
            jobs.joinAll()
        }
    }

    internal class Builder {
        internal fun build(systems: List<System>): AsynchronousSystemService {
            val monitorSystems = systems.filterIsInstance<MonitorSystem>()
            val renderSystems = systems.filterIsInstance<RenderSystem>()
            val updateSystems = systems.filterIsInstance<AsynchronousUpdateSystem>()

            val dag = DirectedAcyclicGraph<AsynchronousUpdateSystem>()
            updateSystems.forEach { system ->

                val systemDeps = system.descriptor.dependencies.filterIsInstance<Dependency.System>().map { it.clazz }
                dag.addWithDependencies(
                    system,
                    systemDeps.mapNotNull { dep ->
                        updateSystems.firstOrNull { it != system && dep.isInstance(it) }
                    }
                )

                val groupDeps = system.descriptor.dependencies.filterIsInstance<Dependency.Group>().map { it.name }
                dag.addWithDependencies(
                    system,
                    groupDeps.map { group ->
                        updateSystems.filter { it != system && it.descriptor.groups.contains(group) }
                    }.flatten()
                )
            }

            return AsynchronousSystemService(
                monitorSystems = monitorSystems,
                renderSystems = renderSystems,
                updateSystemDependencyGraph = dag.getDependencyGraph(),
                updateExecutionSteps = dag.toExecutionSteps()
            )
        }
    }
}

internal class AsynchronousSequentialSystemService internal constructor(
    private val monitorSystems: List<MonitorSystem>,
    private val renderSystems: List<RenderSystem>,
    val updateExecutionSteps: List<List<AsynchronousUpdateSystem>>
) : SystemService {

    private val scope = CoroutineScope(Dispatchers.Default)

    override fun engineInitialized() {
        mutableSetOf<System>().also {
            it.addAll(monitorSystems)
            it.addAll(renderSystems)
            it.addAll(updateExecutionSteps.flatten())
        }.forEach {
            it.engineInitialized()
        }
    }

    override fun engineLoopEnd() {
        monitorSystems.forEach { it.engineLoopEnd() }
    }

    override fun engineLoopStart() {
        monitorSystems.forEach { it.engineLoopStart() }
    }

    override fun renderSystemLoop(delta: Float) {
        renderSystems.forEach { renderer ->
            monitorSystems.forEach { it.systemLoopStart(renderer) }
            renderer.draw(delta)
            monitorSystems.forEach { it.systemLoopEnd(renderer) }
        }
    }

    override fun updateSystemLoop(delta: Float) {
        for(step in updateExecutionSteps) {
            val list = ArrayList<Job>(step.size)

            for(system in step) {
                list.add(scope.launch {
                    monitorSystems.forEach { it.systemLoopStart(system) }
                    system.update(delta)
                    monitorSystems.forEach { it.systemLoopEnd(system) }
                })
            }
            runBlocking {
                list.joinAll()
            }
        }
    }

    internal class Builder {
        internal fun build(systems: List<System>): AsynchronousSequentialSystemService {
            val monitorSystems = systems.filterIsInstance<MonitorSystem>()
            val renderSystems = systems.filterIsInstance<RenderSystem>()
            val updateSystems = systems.filterIsInstance<AsynchronousUpdateSystem>()

            val dag = DirectedAcyclicGraph<AsynchronousUpdateSystem>()
            updateSystems.forEach { system ->

                val systemDeps = system.descriptor.dependencies.filterIsInstance<Dependency.System>().map { it.clazz }
                dag.addWithDependencies(
                    system,
                    systemDeps.mapNotNull { dep ->
                        updateSystems.firstOrNull { it != system && dep.isInstance(it) }
                    }
                )

                val groupDeps = system.descriptor.dependencies.filterIsInstance<Dependency.Group>().map { it.name }
                dag.addWithDependencies(
                    system,
                    groupDeps.map { group ->
                        updateSystems.filter { it != system && it.descriptor.groups.contains(group) }
                    }.flatten()
                )
            }

            return AsynchronousSequentialSystemService(
                monitorSystems = monitorSystems,
                renderSystems = renderSystems,
                updateExecutionSteps = dag.toExecutionSteps()
            )
        }
    }
}

internal class SynchronousSystemService internal constructor(
    private val systems: List<System>,
    private val monitorSystems: List<MonitorSystem>,
    private val renderSystems: List<RenderSystem>,
    private val updateSystems: List<UpdateSystem>
) : SystemService {

    override fun engineInitialized() {
        systems.forEach {
            it.engineInitialized()
        }
    }

    override fun engineLoopEnd() {
        monitorSystems.forEach { it.engineLoopEnd() }
    }

    override fun engineLoopStart() {
        monitorSystems.forEach { it.engineLoopStart() }
    }

    override fun renderSystemLoop(delta: Float) {
        renderSystems.forEach { renderer ->
            monitorSystems.forEach { it.systemLoopStart(renderer) }
            renderer.draw(delta)
            monitorSystems.forEach { it.systemLoopEnd(renderer) }
        }
    }

    override fun updateSystemLoop(delta: Float) {
        updateSystems.forEach { updater ->
            monitorSystems.forEach { it.systemLoopStart(updater) }
            updater.update(delta)
            monitorSystems.forEach { it.systemLoopEnd(updater) }
        }
    }

    internal class Builder {
        internal fun build(systems: List<System>): SynchronousSystemService {
            val monitorSystems = systems.filterIsInstance<MonitorSystem>()
            val renderSystems = systems.filterIsInstance<RenderSystem>()
            val updateSystems = systems.filterIsInstance<UpdateSystem>()

            return SynchronousSystemService(
                systems = systems,
                monitorSystems = monitorSystems,
                renderSystems = renderSystems,
                updateSystems = updateSystems
            )
        }
    }
}