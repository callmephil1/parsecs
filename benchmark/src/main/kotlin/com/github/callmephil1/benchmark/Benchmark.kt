package com.github.callmephil1.benchmark

import com.github.callmephil1.benchmark.systems.DebugSystem
import com.github.callmephil1.benchmark.systems.MovementSystem
import com.github.callmephil1.benchmark.systems.SpawnSystem
import com.github.callmephil1.parsecs.ecs.engine
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.Thread.sleep
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.ArrayDeque
import kotlin.collections.ArrayList
import kotlin.concurrent.withLock
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

class SyncEventBus {
    val lock = Any()
    var count: Int = 0
    val job: Job = GlobalScope.launch {
        while(true) {
            var event: Int? = synchronized(lock) { events.removeLastOrNull() }

            if (event != null) {
                count += event
            }
        }
    }
    val events = ArrayDeque<Int>()

    fun post(event: Int) {
        synchronized(lock) { events.add(event) }
    }

    fun stop() {
        job.cancel()
    }
}

class LockEventBus {
    val lock = ReentrantLock()
    var count: Int = 0
    val job: Job = GlobalScope.launch {
        while(true) {
            var event: Int? = lock.withLock {
                events.removeLastOrNull()
            }

            if (event != null) {
                count += event
            }
        }
    }
    val events = Stack<Int>()

    fun post(event: Int) {
        lock.withLock { events.add(event) }
    }

    fun stop() {
        job.cancel()
    }
}

class ConcurrentEventBus {
    var count = 0
    val events = ConcurrentLinkedQueue<Int>()
    val job = GlobalScope.launch {
        while(true) {
            val event = events.poll()
            if (event != null) {
                count += event
            }
        }
    }

    fun post(event: Int) {
        events.add(event)
    }

    fun stop() {
        job.cancel()
    }
}

fun main() {
    val shuffled = (0..5_000_00).shuffled()

    val set = mutableSetOf<Int?>()
    (0..5_000_00).forEach {
        set.add(it)
    }

    val setDuration = measureNanoTime {
        var count = 0

        shuffled.forEach {
            set.remove(it)
        }
    }

    val list = mutableListOf<Int?>()
    (0..5_000_00).forEach {
        list.add(it)
    }

    val listDuration = measureNanoTime {
        var count = 0

        shuffled.forEach {
            list[it] = null
        }
    }

    println(setDuration)
    println(listDuration)

//    val syncEventBus = SyncEventBus()
//
//    val syncDuration = measureTimeMillis {
//        val jobs = ArrayList<Job>(10)
//        for (j in 0 ..< 9) {
//            jobs.add(GlobalScope.launch {
//                for (i in 0 .. 50_0000) {
//                    syncEventBus.post(i)
//                }
//            })
//        }
//
//        runBlocking {
//            jobs.joinAll()
//
//            while(syncEventBus.events.isNotEmpty()) {
//                sleep(1)
//            }
//
//            syncEventBus.stop()
//
//            println("No more events")
//        }
//    }
//
//    val lockEventBus = LockEventBus()
//
//    val lockDuration = measureTimeMillis {
//        val jobs = ArrayList<Job>(10)
//        for (j in 0 ..< 9) {
//            jobs.add(GlobalScope.launch {
//                for (i in 0 .. 50_0000) {
//                    lockEventBus.post(i)
//                }
//            })
//        }
//
//        runBlocking {
//            jobs.joinAll()
//
//            while(lockEventBus.events.isNotEmpty()) {
//                sleep(1)
//            }
//
//            lockEventBus.stop()
//
//            println("No more events")
//        }
//    }
//    println(syncDuration)
//    println(lockDuration)
}