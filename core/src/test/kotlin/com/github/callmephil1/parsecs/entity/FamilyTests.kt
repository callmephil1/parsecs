package com.github.callmephil1.parsecs.entity

import com.github.callmephil1.parsecs.ecs.component.ComponentMapper
import com.github.callmephil1.parsecs.ecs.component.ComponentService
import com.github.callmephil1.parsecs.ecs.entity.Entity
import com.github.callmephil1.parsecs.ecs.entity.EntityService
import org.junit.jupiter.api.BeforeAll
import kotlin.test.*

private class TestComponent(
    var value: Int = 10
)
private class TestComponent2
private class TestComponent3
private class TestComponent4
private class ParallelTestComponent {
    var value: Int = 0
}

class FamilyTests {

    @Test
    fun `Entities with all components when checking if interested it should succeed`() {
        val entities = entityService.family {
            all(TestComponent::class.java, TestComponent2::class.java)
        }

        val matchEntity = Entity().apply {
            this.componentMask[componentService.getComponentIndex(TestComponent::class.java)] = true
            this.componentMask[componentService.getComponentIndex(TestComponent2::class.java)] = true
        }

        val mismatchEntity = Entity().apply {
            this.componentMask[componentService.getComponentIndex(TestComponent2::class.java)] = true
            this.componentMask[componentService.getComponentIndex(TestComponent4::class.java)] = true
        }

        val mismatchEntity2 = Entity().apply {
            this.componentMask[componentService.getComponentIndex(TestComponent3::class.java)] = true
            this.componentMask[componentService.getComponentIndex(TestComponent4::class.java)] = true
        }

        assertTrue(entities.isInterested(matchEntity))
        assertFalse(entities.isInterested(mismatchEntity))
        assertFalse(entities.isInterested(mismatchEntity2))
    }

    @Test
    fun `Entities with any components when checking if interested it should succeed`() {
        val entities = entityService.family {
            any(TestComponent::class.java, TestComponent2::class.java)
        }

        val matchEntity = Entity().apply {
            this.componentMask[componentService.getComponentIndex(TestComponent::class.java)] = true
            this.componentMask[componentService.getComponentIndex(TestComponent3::class.java)] = true
        }

        val matchEntity2 = Entity().apply {
            this.componentMask[componentService.getComponentIndex(TestComponent2::class.java)] = true
            this.componentMask[componentService.getComponentIndex(TestComponent4::class.java)] = true
        }

        val matchEntity3 = Entity().apply {
            this.componentMask[componentService.getComponentIndex(TestComponent::class.java)] = true
            this.componentMask[componentService.getComponentIndex(TestComponent2::class.java)] = true
        }

        val mismatchEntity = Entity().apply {
            this.componentMask[componentService.getComponentIndex(TestComponent3::class.java)] = true
            this.componentMask[componentService.getComponentIndex(TestComponent4::class.java)] = true
        }

        assertTrue(entities.isInterested(matchEntity))
        assertTrue(entities.isInterested(matchEntity2))
        assertTrue(entities.isInterested(matchEntity3))
        assertFalse(entities.isInterested(mismatchEntity))
    }

    @Test
    fun `Entities with none components when checking if interested it should succeed`() {
        val entities = entityService.family {
            none(TestComponent::class.java, TestComponent2::class.java)
        }

        val matchEntity = Entity().apply {
            this.componentMask[componentService.getComponentIndex(TestComponent4::class.java)] = true
            this.componentMask[componentService.getComponentIndex(TestComponent3::class.java)] = true
        }

        val mismatchEntity = Entity().apply {
            this.componentMask[componentService.getComponentIndex(TestComponent::class.java)] = true
            this.componentMask[componentService.getComponentIndex(TestComponent2::class.java)] = true
        }

        val mismatchEntity2 = Entity().apply {
            this.componentMask[componentService.getComponentIndex(TestComponent::class.java)] = true
            this.componentMask[componentService.getComponentIndex(TestComponent3::class.java)] = true
        }

        assertTrue(entities.isInterested(matchEntity))
        assertFalse(entities.isInterested(mismatchEntity))
        assertFalse(entities.isInterested(mismatchEntity2))
    }

    @Test
    fun `Entities with a predicate should succeed`() {
        val entities = entityService.family {
            all(TestComponent::class.java)

            predicate {
                it.index == 0
            }
        }

        val matchEntity = Entity().apply {
            index = 0
            val testComponent = testComponentMapper.obtain()
            testComponent.value = 5
            testComponentMapper[this] = testComponent
            this.componentMask[componentService.getComponentIndex(TestComponent::class.java)] = true
        }

        val mismatchEntity = Entity().apply {
            index = 1
            val testComponent = testComponentMapper.obtain()
            testComponent.value = 10
            testComponentMapper[this] = testComponent
            this.componentMask[componentService.getComponentIndex(TestComponent::class.java)] = true
        }

        assertTrue(entities.isInterested(matchEntity))
        assertFalse(entities.isInterested(mismatchEntity))
    }

    @Test
    fun `Performing a parallelForEach should process all non-null items`() {
        val entities = entityService.family {
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