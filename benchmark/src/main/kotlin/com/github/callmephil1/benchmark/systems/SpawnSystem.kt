package com.github.callmephil1.benchmark.systems

import com.github.callmephil1.benchmark.component.PositionComponent
import com.github.callmephil1.benchmark.component.VelocityComponent
import com.github.callmephil1.parsecs.ecs.component.ComponentService
import com.github.callmephil1.parsecs.ecs.dsl.ParsecsDsl
import com.github.callmephil1.parsecs.ecs.entity.EntityService
import com.github.callmephil1.parsecs.ecs.system.UpdateSystem
import kotlin.random.Random

class SpawnSystem(
    override val componentService: ComponentService,
    override val entityService: EntityService,
) : UpdateSystem, ParsecsDsl {

    private val positionMapper = componentService.getMapper(PositionComponent::class.java)
    private val velocityMapper = componentService.getMapper(VelocityComponent::class.java)

    private var timeout: Float = 0f

    private fun addBunny() {
        val bunny = entity {
            val position = positionMapper.obtain()
            position.x = 0f
            position.y = 0f

            val velocity = velocityMapper.obtain()
            velocity.x = Random.nextFloat() * 10
            velocity.y = Random.nextFloat() * 10

            positionMapper[this] = position
            velocityMapper[this] = velocity
        }
    }

    override fun update(delta: Float) {
        timeout += delta

        if (delta > 2)
            println("Bunnys ${addBunny()}")
    }
}