package com.github.callmephil1.parsecs.ecs.entity.dsl

import com.github.callmephil1.parsecs.ecs.component.dsl.Component
import com.github.callmephil1.parsecs.ecs.entity.Entities
import com.github.callmephil1.parsecs.ecs.entity.Entity
import com.github.callmephil1.parsecs.ecs.entity.EntityRef
import com.github.callmephil1.parsecs.ecs.entity.EntityService

interface EntityDsl {
    val entityService: EntityService

    fun entities(configure: Entities.Builder.() -> Unit): Entities = entityService.entities(configure)

    fun entity(configure: Entity.() -> Unit): Entity = entityService.entity(configure)

    fun Entities.Builder.all(vararg component: Component<*>) = this.all(*component.map { it.clazz }.toTypedArray())
    fun Entities.Builder.any(vararg component: Component<*>) = this.any(*component.map { it.clazz }.toTypedArray())
    fun Entities.Builder.none(vararg component: Component<*>) = this.none(*component.map { it.clazz }.toTypedArray())

    fun Entity.update(block: Entity.() -> Unit) {
        this.block()
        entityService.update(this)
    }
    fun EntityRef.update(block: Entity.() -> Unit) = this.entity.update(block)
}
