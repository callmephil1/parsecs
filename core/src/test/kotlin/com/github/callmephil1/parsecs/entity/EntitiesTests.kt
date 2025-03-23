package com.github.callmephil1.parsecs.entity

import com.github.callmephil1.parsecs.ecs.collections.Bits
import com.github.callmephil1.parsecs.ecs.component.ComponentMapper
import com.github.callmephil1.parsecs.ecs.component.ComponentService
import com.github.callmephil1.parsecs.ecs.entity.Entity
import com.github.callmephil1.parsecs.ecs.entity.EntityService
import org.junit.jupiter.api.BeforeAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestComponent
class TestComponent2
class TestComponent3
class TestComponent4
class ParallelTestComponent {
    var value: Int = 0
}

class EntitiesTests {

    @Test
    fun `Entities with all components when checking if interested it should succeed`() {
        val entities = entityService.entities {
            all(TestComponent::class.java, TestComponent2::class.java)
        }

        val matchBits = Bits().apply {
            set(componentService.getComponentIndex(TestComponent::class.java), true)
            set(componentService.getComponentIndex(TestComponent2::class.java), true)
        }

        val mismatchBits = Bits().apply {
            set(componentService.getComponentIndex(TestComponent2::class.java), true)
            set(componentService.getComponentIndex(TestComponent4::class.java), true)
        }

        val mismatchBits2 = Bits().apply {
            set(componentService.getComponentIndex(TestComponent3::class.java), true)
            set(componentService.getComponentIndex(TestComponent4::class.java), true)
        }

        assertTrue(entities.isInterested(matchBits))
        assertFalse(entities.isInterested(mismatchBits))
        assertFalse(entities.isInterested(mismatchBits2))
    }

    @Test
    fun `Entities with any components when checking if interested it should succeed`() {
        val entities = entityService.entities {
            any(TestComponent::class.java, TestComponent2::class.java)
        }

        val matchBits = Bits().apply {
            set(componentService.getComponentIndex(TestComponent::class.java), true)
            set(componentService.getComponentIndex(TestComponent3::class.java), true)
        }

        val matchBits2 = Bits().apply {
            set(componentService.getComponentIndex(TestComponent2::class.java), true)
            set(componentService.getComponentIndex(TestComponent4::class.java), true)
        }

        val matchBits3 = Bits().apply {
            set(componentService.getComponentIndex(TestComponent::class.java), true)
            set(componentService.getComponentIndex(TestComponent2::class.java), true)
        }

        val mismatchBits = Bits().apply {
            set(componentService.getComponentIndex(TestComponent3::class.java), true)
            set(componentService.getComponentIndex(TestComponent4::class.java), true)
        }

        assertTrue(entities.isInterested(matchBits))
        assertTrue(entities.isInterested(matchBits2))
        assertTrue(entities.isInterested(matchBits3))
        assertFalse(entities.isInterested(mismatchBits))
    }

    @Test
    fun `Entities with none components when checking if interested it should succeed`() {
        val entities = entityService.entities {
            none(TestComponent::class.java, TestComponent2::class.java)
        }

        val matchBits = Bits().apply {
            set(componentService.getComponentIndex(TestComponent4::class.java), true)
            set(componentService.getComponentIndex(TestComponent3::class.java), true)
        }

        val mismatchBits = Bits().apply {
            set(componentService.getComponentIndex(TestComponent::class.java), true)
            set(componentService.getComponentIndex(TestComponent2::class.java), true)
        }

        val mismatchBits2 = Bits().apply {
            set(componentService.getComponentIndex(TestComponent::class.java), true)
            set(componentService.getComponentIndex(TestComponent3::class.java), true)
        }

        assertTrue(entities.isInterested(matchBits))
        assertFalse(entities.isInterested(mismatchBits))
        assertFalse(entities.isInterested(mismatchBits2))
    }

    @Test
    fun `Performing a parallelForEach should process all non-null items`() {
        val entities = entityService.entities {
            all(ParallelTestComponent::class.java)
        }

        for (i in 0 .. 1000) {
            val entity = Entity()
            entity.index = i
            parallelTestComponentMapper[entity] = parallelTestComponentMapper.obtain()

            entities.add(entity)
        }

        entities.parallelForEach {
            parallelTestComponentMapper[it].value += 1
        }

        entities.forEach {
            assertEquals(parallelTestComponentMapper[it].value, 1)
        }
    }

    companion object {
        private lateinit var componentService: ComponentService
        private lateinit var entityService: EntityService

        private lateinit var testComponentMapper: ComponentMapper<TestComponent>
        private lateinit var testComponentMapper2: ComponentMapper<TestComponent2>
        private lateinit var testComponentMapper3: ComponentMapper<TestComponent3>
        private lateinit var testComponentMapper4: ComponentMapper<TestComponent4>
        private lateinit var parallelTestComponentMapper: ComponentMapper<ParallelTestComponent>

        @JvmStatic
        @BeforeAll
        fun setUp() {
            componentService = ComponentService()
            testComponentMapper = componentService.getMapper(TestComponent::class.java)
            testComponentMapper2 = componentService.getMapper(TestComponent2::class.java)
            testComponentMapper3 = componentService.getMapper(TestComponent3::class.java)
            testComponentMapper4 = componentService.getMapper(TestComponent4::class.java)
            parallelTestComponentMapper = componentService.getMapper(ParallelTestComponent::class.java)

            entityService = EntityService(componentService)
        }
    }
}