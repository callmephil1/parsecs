package com.github.callmephil1.parsecs.collections

import com.github.callmephil1.parsecs.ecs.collections.Bag
import kotlin.test.Test
import kotlin.test.assertEquals

class BagTests {
    @Test
    fun `Add to bag and get should be same object`() {
        val bag = Bag<Int>()

        bag[0] = 10
        bag[10] = 100

        assertEquals(bag[0], 10)
        assertEquals(bag[3], null)
        assertEquals(bag[10], 100)
    }

    @Test
    fun `Remove object from bag returns the object`() {
        val bag = Bag<String>()

        bag[0] = "test1"
        bag[10] = "test10"

        val string = bag.removeAt(10)

        assertEquals(string, "test10")
    }

    @Test
    fun `Removing object moves last object to replace it`() {
        val bag = Bag<String>()

        for(i in 0..10)
            bag.add("index $i")

        val string = bag.removeAt(5)

        assertEquals(string, "index 10")
        assertEquals(bag[5], "index 10")
    }
}