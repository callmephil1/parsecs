package com.github.callmephil1.parsecs.entity

import com.github.callmephil1.parsecs.ecs.component.ComponentService
import com.github.callmephil1.parsecs.ecs.entity.EntityService
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class EntityServiceTests {
    private lateinit var componentService: ComponentService
    private lateinit var entityService: EntityService

    @BeforeTest
    fun setup() {
        componentService = ComponentService()
        entityService = EntityService(componentService)
    }

    @Test
    fun `Get new entity should succeed`() {
        val entity = entityService.entity {  }
        entityService.createEntities()
        assertNotEquals(0, entityService.entityCount)
        assertNotEquals(-1, entity.index)
    }

    @Test
    fun `Release entity`() {
        val entity = entityService.entity {  }
        entityService.createEntities()

        entityService.release(entity)
        entityService.releaseEntities()

        assertEquals(0, entityService.entityCount)
        assertEquals(-1, entity.index)
    }
}