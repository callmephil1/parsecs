package com.github.callmephil1.parsecs.entity.dsl

import com.github.callmephil1.parsecs.ecs.component.ComponentService
import com.github.callmephil1.parsecs.ecs.component.dsl.Component
import com.github.callmephil1.parsecs.ecs.component.dsl.ComponentDsl
import com.github.callmephil1.parsecs.ecs.entity.EntityService
import com.github.callmephil1.parsecs.ecs.entity.dsl.EntityDsl
import kotlin.test.Test
import kotlin.test.assertEquals

private class DslTestComponent1 {
    companion object : Component<DslTestComponent1>() {
        override val clazz = DslTestComponent1::class.java
    }
}
private class DslTestComponent2 {
    companion object : Component<DslTestComponent2>() {
        override val clazz = DslTestComponent2::class.java
    }
}
private class DslTestComponent3 {
    companion object : Component<DslTestComponent3>() {
        override val clazz = DslTestComponent3::class.java
    }
}

class EntitiesDslTests : ComponentDsl, EntityDsl {

    override val componentService: ComponentService = ComponentService()
    override val entityService: EntityService = EntityService(componentService)

    private val entities1 = entities {
        name = "1"
        all(DslTestComponent1, DslTestComponent3)
    }

    private val entities2 = entities {
        name = "2"
        all(DslTestComponent2, DslTestComponent3)
    }

    @Test
    fun `When creating an entity it should get added to the appropriate entities`() {
        val entity1 = entity {
            tag = "1"
            obtainAndSet(DslTestComponent1)
            obtainAndSet(DslTestComponent3)
        }

        val entity2 = entity {
            tag = "2"
            obtainAndSet(DslTestComponent2)
            obtainAndSet(DslTestComponent3)
        }

        entityService.createEntities()

        assertEquals(1, entities1.entities.count)
        assertEquals(entity1, entities1.entities[0])
        assertEquals(1, entities2.entities.count)
        assertEquals(entity2, entities2.entities[0])
    }
}