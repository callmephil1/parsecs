package com.github.callmephil1.parsecs.collections

import com.github.callmephil1.parsecs.ecs.collections.Pool
import kotlin.test.*

private data class TestComponent(
    var number: Int = -1
)

class PoolTests {
    @Test
    fun `Obtaining object from empty pool return newly instantiated object`() {
        val pool = Pool(TestComponent::class.java)

        pool.obtain()
    }

    @Test
    fun `Releasing object to empty pool should save object`() {
        val pool = Pool(TestComponent::class.java)
        val testComponent = TestComponent(number = 10)

        pool.release(testComponent)

        val newObj = pool.obtain()

        assertEquals(newObj, testComponent)
    }

    @Test
    fun `Releasing object to full pool should ignore the object`() {
        val pool = Pool(TestComponent::class.java, 2)

        val testComponent1 = pool.obtain()
        val testComponent2 = pool.obtain()
        val testComponent3 = pool.obtain()

        pool.release(testComponent1)
        pool.release(testComponent2)
        pool.release(testComponent3)

        val testComponent4 = pool.obtain()
        val testComponent5 = pool.obtain()
        val testComponent6 = pool.obtain()

        assertSame(testComponent1, testComponent5)
        assertSame(testComponent2, testComponent4)
        assertNotSame(testComponent3, testComponent6)
    }
}