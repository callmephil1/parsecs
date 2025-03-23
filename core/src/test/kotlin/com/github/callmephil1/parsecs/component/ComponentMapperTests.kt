package com.github.callmephil1.parsecs.component

import com.github.callmephil1.parsecs.ecs.component.ComponentMapper
import com.github.callmephil1.parsecs.ecs.entity.Entity
import kotlin.test.Test

class TestComponent {
    var number: Int = -1
}

class ComponentMapperTests {
    @Test
    fun `Add and remove objects from mapper should succeed`() {
        val mapper = ComponentMapper(TestComponent::class.java, 0)

        for(i in 0 .. 10) {
            val component = mapper.obtain()
            component.number = i

            val entity = Entity()
            entity.index = i

            mapper.set(entity, component)
        }

        for(i in 2..4)
            mapper.release(i, i + 1)

        for (i in 0..2)
            mapper.obtain()
    }
}