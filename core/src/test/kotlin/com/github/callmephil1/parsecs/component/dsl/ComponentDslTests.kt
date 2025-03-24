package com.github.callmephil1.parsecs.component.dsl

import com.github.callmephil1.parsecs.ecs.component.ComponentService
import com.github.callmephil1.parsecs.ecs.component.dsl.Component
import com.github.callmephil1.parsecs.ecs.component.dsl.ComponentDsl
import com.github.callmephil1.parsecs.ecs.entity.EntityService
import com.github.callmephil1.parsecs.ecs.entity.dsl.EntityDsl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

class ComponentDslTests : ComponentDsl, EntityDsl {
    override val componentService = ComponentService()
    override val entityService = EntityService(componentService)

    @Test
    fun `Using the entity dsl when adding components then all should be returned in allComponents`() {
        val entity1 = entity {
            obtainAndSet(DslTestComponent1)
            obtainAndSet(DslTestComponent2)
        }

        val allComponents = entity1.allComponents()
        assertEquals(2, allComponents.size)
        assertTrue(allComponents[0] is DslTestComponent1)
        assertTrue(allComponents[1] is DslTestComponent2)
    }
}