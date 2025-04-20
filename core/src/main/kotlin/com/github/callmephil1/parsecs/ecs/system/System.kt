package com.github.callmephil1.parsecs.ecs.system

interface System {
    val descriptor: SystemDescriptor
        get() = SystemDescriptor.NONE

    fun systemDescriptor(configure: SystemDescriptor.Builder.() -> Unit): SystemDescriptor {
        val builder = SystemDescriptor.Builder()
        builder.configure()
        return builder.build()
    }

    fun worldInitialized() {}
}

interface MonitorSystem: System {
    fun systemLoopStart(system: System) {}
    fun systemLoopEnd(system: System) {}

    fun worldLoopStart() {}
    fun worldLoopEnd() {}
}

interface RenderSystem: System {
    fun draw(delta: Float)
}

interface UpdateSystem: System {
    fun update(delta: Float)
}

interface AsynchronousUpdateSystem : UpdateSystem