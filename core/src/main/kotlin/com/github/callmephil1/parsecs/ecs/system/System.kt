package com.github.callmephil1.parsecs.ecs.system

interface System {
    fun engineInitialized() {}
}

interface MonitorSystem: System {
    fun engineLoopStart() {}
    fun engineLoopEnd() {}

    fun systemLoopStart(system: System) {}
    fun systemLoopEnd(system: System) {}
}

interface RenderSystem: System {
    fun draw(delta: Float)
}

interface UpdateSystem: System {
    fun update(delta: Float)
}

interface AsynchronousUpdateSystem : UpdateSystem {
    val dependsOn: List<Class<*>>
}