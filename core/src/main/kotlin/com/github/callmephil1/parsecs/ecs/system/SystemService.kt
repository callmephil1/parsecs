package com.github.callmephil1.parsecs.ecs.system

import com.github.callmephil1.parsecs.ecs.ds.DirectedAcyclicGraph
import kotlinx.coroutines.*

internal interface SystemService {
    val systems: List<System>

    fun worldInitialized()
    fun worldLoopEnd()
    fun worldLoopStart()
    fun renderSystemLoop(delta: Float)
    fun updateSystemLoop(delta: Float)
}

internal class AsynchronousSystemService(
    override val systems: List<System>,
    private val monitorSystems: List<MonitorSystem>,
    private val renderSystems: List<RenderSystem>,
    private val updateSystemDependencyGraph: Map<AsynchronousUpdateSystem, Set<AsynchronousUpdateSystem>>,
    private val updateExecutionSteps: List<List<AsynchronousUpdateSystem>>
) : SystemService {

    private val scope = CoroutineScope(Dispatchers.Default)

    override fun worldInitialized() {
        mutableSetOf<System>().also {
            it.addAll(monitorSystems)
            it.addAll(renderSystems)
            it.addAll(updateSystemDependencyGraph.keys)
        }.forEach {
            it.worldInitialized()
        }
    }

    override fun worldLoopEnd() {
        monitorSystems.forEach { it.worldLoopEnd() }
    }

    override fun worldLoopStart() {
        monitorSystems.forEach { it.worldLoopStart() }
    }

    override fun renderSystemLoop(delta: Float) {
        renderSystems.forEach { renderer ->
            monitorSystems.forEach { it.systemLoopStart(renderer) }
            renderer.draw(delta)
            monitorSystems.forEach { it.systemLoopEnd(renderer) }
        }
    }

    override fun updateSystemLoop(delta: Float) {
        val systemJobs = mutableMapOf<AsynchronousUpdateSystem, Job>()

        for (step in updateExecutionSteps) {
            for (system in step) {
                systemJobs[system] = scope.launch {
                    updateSystemDependencyGraph[system]!!.forEach { systemJobs[it]!!.join() }
                    monitorSystems.forEach { it.systemLoopStart(system) }
                    system.update(delta)
                    monitorSystems.forEach { it.systemLoopEnd(system) }
                }
            }
        }

        runBlocking {
            systemJobs.values.joinAll()
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

                val groupDeps = system.descriptor.dependencies.filterIsInstance<Dependency.Group>().map { it.identifier }
                dag.addWithDependencies(
                    system,
                    groupDeps.map { group ->
                        updateSystems.filter { it != system && it.descriptor.groups.contains(group) }
                    }.flatten()
                )
            }

            return AsynchronousSystemService(
                systems = systems,
                monitorSystems = monitorSystems,
                renderSystems = renderSystems,
                updateSystemDependencyGraph = dag.getDependencyGraph(),
                updateExecutionSteps = dag.toExecutionSteps()
            )
        }
    }
}

internal class AsynchronousSequentialSystemService internal constructor(
    override val systems: List<System>,
    private val monitorSystems: List<MonitorSystem>,
    private val renderSystems: List<RenderSystem>,
    val updateExecutionSteps: List<List<AsynchronousUpdateSystem>>
) : SystemService {

    private val scope = CoroutineScope(Dispatchers.Default)

    override fun worldInitialized() {
        mutableSetOf<System>().also {
            it.addAll(monitorSystems)
            it.addAll(renderSystems)
            it.addAll(updateExecutionSteps.flatten())
        }.forEach {
            it.worldInitialized()
        }
    }

    override fun worldLoopEnd() {
        monitorSystems.forEach { it.worldLoopEnd() }
    }

    override fun worldLoopStart() {
        monitorSystems.forEach { it.worldLoopStart() }
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

                val groupDeps = system.descriptor.dependencies.filterIsInstance<Dependency.Group>().map { it.identifier }
                dag.addWithDependencies(
                    system,
                    groupDeps.map { group ->
                        updateSystems.filter { it != system && it.descriptor.groups.contains(group) }
                    }.flatten()
                )
            }

            return AsynchronousSequentialSystemService(
                systems = systems,
                monitorSystems = monitorSystems,
                renderSystems = renderSystems,
                updateExecutionSteps = dag.toExecutionSteps()
            )
        }
    }
}

internal class SynchronousSystemService internal constructor(
    override val systems: List<System>,
    private val monitorSystems: List<MonitorSystem>,
    private val renderSystems: List<RenderSystem>,
    private val updateSystems: List<UpdateSystem>
) : SystemService {

    override fun worldInitialized() {
        systems.forEach {
            it.worldInitialized()
        }
    }

    override fun worldLoopEnd() {
        monitorSystems.forEach { it.worldLoopEnd() }
    }

    override fun worldLoopStart() {
        monitorSystems.forEach { it.worldLoopStart() }
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

        internal fun buildGraph(systems: List<System>): SynchronousSystemService {
            val monitorSystems = systems.filterIsInstance<MonitorSystem>()
            val renderSystems = systems.filterIsInstance<RenderSystem>()
            val updateSystems = systems.filterIsInstance<UpdateSystem>()

            val dag = DirectedAcyclicGraph<UpdateSystem>()
            updateSystems.forEach { system ->

                val systemDeps = system.descriptor.dependencies.filterIsInstance<Dependency.System>().map { it.clazz }
                dag.addWithDependencies(
                    system,
                    systemDeps.mapNotNull { dep ->
                        updateSystems.firstOrNull { it != system && dep.isInstance(it) }
                    }
                )

                val groupDeps = system.descriptor.dependencies.filterIsInstance<Dependency.Group>().map { it.identifier }
                dag.addWithDependencies(
                    system,
                    groupDeps.map { group ->
                        updateSystems.filter { it != system && it.descriptor.groups.contains(group) }
                    }.flatten()
                )
            }

            return SynchronousSystemService(
                systems,
                monitorSystems = monitorSystems,
                renderSystems = renderSystems,
                updateSystems = dag.toExecutionSteps().flatten()
            )
        }
    }
}