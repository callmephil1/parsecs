package com.github.callmephil1.parsecs

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class EventBusListener<T> (
    val clazz: Class<T>,
    val lambda: (T) -> Unit
)

class EventBus {
    private val events1 = ArrayDeque<Any>(100)
    private val events2 = ArrayDeque<Any>(100)
    private var events = events1
    private val listeners = mutableMapOf<Class<*>, MutableList<EventBusListener<*>>>()
    private val lock = ReentrantLock()

    fun <T : Any> post(event: T) {
        lock.withLock {
            events.addLast(event)
        }
    }

    fun <T : Any> postImmediately(event: T) {
        listeners[event::class.java]?.forEach { (it as EventBusListener<T>).lambda(event) }
    }

    internal fun processEvents() {
        val eventsToProcess = if (events == events1) {
            events = events2
            events1
        } else {
            events = events1
            events2
        }

        while(eventsToProcess.isNotEmpty()) {
            val event = eventsToProcess.removeFirst()
            listeners[event::class.java]?.forEach { (it as EventBusListener<Any>).lambda(event) }
        }
    }

    fun <T : Any> register(eventType: Class<T>, listener: (T) -> Unit): EventBusListener<T> {
        val newListener = EventBusListener(
            eventType,
            listener
        )

        lock.withLock {
            listeners.getOrPut(eventType) { mutableListOf() }.add(newListener)
        }

        return newListener
    }

    inline fun <reified T : Any> register(noinline listener: (T) -> Unit) = register(T::class.java, listener)

    fun unregister(listener: EventBusListener<*>): Unit = lock.withLock { listeners[listener.clazz]?.remove(listener) }

    companion object {
        val global = EventBus()
    }
}