package com.github.callmephil1.parsecs.entity

import com.github.callmephil1.parsecs.ecs.component.ComponentMapper
import com.github.callmephil1.parsecs.ecs.component.ComponentService
import com.github.callmephil1.parsecs.ecs.entity.Entity
import com.github.callmephil1.parsecs.ecs.entity.EntityService
import com.github.callmephil1.parsecs.ecs.family.FamilyService
import com.github.callmephil1.parsecs.ecs.world.World
import com.github.callmephil1.parsecs.ecs.world.world
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
    fun `Families with all components when checking if interested it should succeed`() {
        val family = familyService.family {
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

        assertTrue(family.isInterested(matchEntity))
        assertFalse(family.isInterested(mismatchEntity))
        assertFalse(family.isInterested(mismatchEntity2))
    }

    @Test
    fun `Families with any components when checking if interested it should succeed`() {
        val family = familyService.family {
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

        assertTrue(family.isInterested(matchEntity))
        assertTrue(family.isInterested(matchEntity2))
        assertTrue(family.isInterested(matchEntity3))
        assertFalse(family.isInterested(mismatchEntity))
    }

    @Test
    fun `Families with none components when checking if interested it should succeed`() {
        val family = familyService.family {
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

        assertTrue(family.isInterested(matchEntity))
        assertFalse(family.isInterested(mismatchEntity))
        assertFalse(family.isInterested(mismatchEntity2))
    }

    @Test
    fun `Families with a predicate should succeed`() {
        val family = familyService.family {
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

        assertTrue(family.isInterested(matchEntity))
        assertFalse(family.isInterested(mismatchEntity))
    }

    @Test
    fun `Performing a parallelForEach should process all non-null items`() {
        val family = familyService.family {
            all(ParallelTestComponent::class.java)
        }

        for (i in 0 .. 1000) {
            val entity = Entity()
            entity.index = i
            parallelTestComponentMapper[entity] = parallelTestComponentMapper.obtain()

            family.add(entity)
        }

        family.parallelForEach {
            parallelTestComponentMapper[it].value += 1
        }

        family.forEach {
            assertEquals(parallelTestComponentMapper[it].value, 1)
        }
    }

    companion object {
        private lateinit var componentService: ComponentService
        private lateinit var entityService: EntityService
        private lateinit var familyService: FamilyService
        private lateinit var world: World

        private lateinit var testComponentMapper: ComponentMapper<TestComponent>
        private lateinit var testComponentMapper2: ComponentMapper<TestComponent2>
        private lateinit var testComponentMapper3: ComponentMapper<TestComponent3>
        private lateinit var testComponentMapper4: ComponentMapper<TestComponent4>
        private lateinit var parallelTestComponentMapper: ComponentMapper<ParallelTestComponent>

        @JvmStatic
        @BeforeAll
        fun setUp() {
            componentService = ComponentService()
            entityService = EntityService()
            familyService = FamilyService(componentService)
            testComponentMapper = componentService.getMapper(TestComponent::class.java)
            testComponentMapper2 = componentService.getMapper(TestComponent2::class.java)
            testComponentMapper3 = componentService.getMapper(TestComponent3::class.java)
            testComponentMapper4 = componentService.getMapper(TestComponent4::class.java)
            parallelTestComponentMapper = componentService.getMapper(ParallelTestComponent::class.java)

            world = world {  }
        }
    }
}