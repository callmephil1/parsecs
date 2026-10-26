package com.github.callmephil1.benchmark.systems

import com.github.callmephil1.parsecs.ecs.system.MonitorSystem
import com.github.callmephil1.parsecs.ecs.system.RenderSystem
import com.github.callmephil1.parsecs.ecs.system.System
import com.github.callmephil1.parsecs.ecs.system.UpdateSystem
import com.github.callmephil1.parsecs.ext.SystemExt.nanoToMilli

data class SystemTimes(
    var startTime: Long = 0,
    var endTime: Long = 0
)

class DebugSystem : MonitorSystem, RenderSystem, UpdateSystem {

    private var currentElapsedTime = 0f
    private var engineLoopEndTime: Long = Long.MAX_VALUE
    private var engineLoopStartTime: Long = Long.MAX_VALUE
    private val systemTimes = mutableMapOf<System, SystemTimes>()
    private val targetUpdateTime = 1

    override fun worldLoopStart() {
        if (currentElapsedTime < targetUpdateTime)
            return
        engineLoopStartTime = java.lang.System.nanoTime()
    }

    override fun worldLoopEnd() {
        if (currentElapsedTime < targetUpdateTime)
            return
        engineLoopEndTime = java.lang.System.nanoTime()
    }

    override fun systemLoopStart(system: System) {
        if(system == this)
            return
        systemTimes[system] = SystemTimes(java.lang.System.nanoTime())
    }

    override fun systemLoopEnd(system: System) {
        if(system == this)
            return
        systemTimes.getValue(system).endTime = java.lang.System.nanoTime()
    }

    override fun draw(delta: Float) {
        if (currentElapsedTime < targetUpdateTime)
            return

        val fps = 1 / delta

        val systemAndTimes = systemTimes.map {
            it.key::class.java.simpleName to (it.value.endTime - it.value.startTime).nanoToMilli()
        }

        println("FPS: $fps")
        systemAndTimes.forEachIndexed { index, pair ->
            println("${pair.first}: ${pair.second}ms")
        }

        val totalTime = systemAndTimes.map { it.second }.sum()

        println("Total time: $totalTime")
    }

    override fun update(delta: Float) {
        if (currentElapsedTime >= targetUpdateTime)
            currentElapsedTime = 0f
        currentElapsedTime += delta
    }
}