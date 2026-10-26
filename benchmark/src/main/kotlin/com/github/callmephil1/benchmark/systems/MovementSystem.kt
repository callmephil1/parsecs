package com.github.callmephil1.benchmark.systems

import com.github.callmephil1.benchmark.component.PositionComponent
import com.github.callmephil1.benchmark.component.VelocityComponent
import com.github.callmephil1.parsecs.ecs.component.ComponentService
import com.github.callmephil1.parsecs.ecs.dsl.ParsecsDsl
import com.github.callmephil1.parsecs.ecs.family.Family
import com.github.callmephil1.parsecs.ecs.entity.EntityService
import com.github.callmephil1.parsecs.ecs.system.AsynchronousUpdateSystem
import com.github.callmephil1.parsecs.ecs.system.SystemDescriptor
import com.github.callmephil1.parsecs.ecs.world.World

class MovementSystem(
    override val world: World
) : ParsecsDsl, AsynchronousUpdateSystem {

    private var family: Family = family {
        all(PositionComponent::class.java, VelocityComponent::class.java)
    }



    private val positionMapper = componentService.getMapper(PositionComponent::class.java)
    private val velocityMapper = componentService.getMapper(VelocityComponent::class.java)

    override fun update(delta: Float) {
        val left = 0
        val right = 1920
        val top = 1080
        val bottom = -0

        family.forEach {

            val position = positionMapper[it]
            val velocity = velocityMapper[it]

            position.x += velocity.x
            position.y += velocity.y

            if (velocity.x > 0 && position.x > right) {
                velocity.x *= -1
            } else if (velocity.x < 0 && position.x < left) {
                velocity.x *= -1
            }

            if (velocity.y > 0 && position.y > top) {
                velocity.y *= -1
            } else if (velocity.y < 0 && position.y < bottom) {
                velocity.y *= -1
            }
        }
    }
}