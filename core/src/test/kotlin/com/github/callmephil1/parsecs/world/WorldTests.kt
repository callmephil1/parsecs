package com.github.callmephil1.parsecs.world

import com.github.callmephil1.parsecs.ecs.system.RenderSystem
import com.github.callmephil1.parsecs.ecs.system.UpdateSystem
import com.github.callmephil1.parsecs.ecs.world.World
import com.github.callmephil1.parsecs.ecs.world.world
import kotlin.test.Test

private class TestComponent {
    var x: Int = 0
}

class TestRenderSystem : RenderSystem {
    override fun draw(delta: Float) {

    }
}

class TestUpdateSystem(
    world: World
) : UpdateSystem {

    val family = world.family {
        all(TestComponent::class.java)
    }

    override fun update(delta: Float) {

    }
}

class TestService

class TestService2(private val testService: TestService)

class WorldTests {

    @Test
    fun `Create an world builder and add systems it should build successfully`() {
        val world = world {
            systems(
                TestRenderSystem::class.java,
                TestUpdateSystem::class.java
            )
        }
    }

    @Test
    fun `Create an world builder and add services it should build successfully`() {
        val world = world {
            services {
                singleton(TestService::class.java)
                singleton(TestService2::class.java)
            }
        }
    }
}