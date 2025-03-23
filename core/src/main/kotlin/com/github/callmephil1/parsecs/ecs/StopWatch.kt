package com.github.callmephil1.parsecs.ecs

class StopWatch {
    private var startTime: Long = 0
    private var stopTime: Long = 0

    val seconds: Float
        get() = (stopTime - startTime) / 1_000_000_000f

    fun start() {
        startTime = System.nanoTime()
        stopTime = startTime
    }

    fun stop() {
        stopTime = System.nanoTime()
    }
}