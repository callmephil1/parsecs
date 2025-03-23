package com.github.callmephil1.parsecs

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

data class Message(
    val message: String
)

class EventBusTests {
    @Test
    fun `Add listener to EventBus and postImmediately should process immediately`() {
        val eventBus = EventBus()
        var message = ""

        eventBus.register<Message> {
            message = it.message
        }

        eventBus.postImmediately(Message("value"))

        assertEquals("value", message)
    }

    @Test
    fun `Add listener to EventBus, post messages, and process should process all messages`() {
        val eventBus = EventBus()
        val messages = mutableListOf<String>()

        eventBus.register<Message> {
            messages.add(it.message)
        }

        eventBus.post(Message("value1"))
        eventBus.post(Message("value2"))
        eventBus.post(Message("value3"))
        eventBus.post(Message("value4"))

        eventBus.processEvents()

        assertContentEquals(listOf("value1", "value2", "value3", "value4"), messages)
    }

    @Test
    fun `Add and remove a listener from the EventBus`() {
        val eventBus = EventBus()
        var message = ""

        val listener = eventBus.register<Message> { message = it.message }

        eventBus.post(Message("value"))

        eventBus.unregister(listener)

        eventBus.processEvents()

        assertEquals("", message)
    }
}