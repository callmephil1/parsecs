package com.github.callmephil1.parsecs.ecs.dsl

import com.github.callmephil1.parsecs.ecs.entity.Entity
import com.github.callmephil1.parsecs.ecs.entity.EntityRef
import com.github.callmephil1.parsecs.ecs.family.Family
import com.github.callmephil1.parsecs.ecs.world.World

interface ParsecsDsl {
    val world: World

    fun family(configure: Family.Builder.() -> Unit): Family = world.family(configure)

    fun entity(configure: Entity.() -> Unit): Entity = world.entity(configure)

    fun <T : Any> Component<T>.obtain() = world.getMapper(this).obtain()

    fun Entity.allComponents() : List<Any> = this.componentMask.getSetIndicies().map {
        world.getMapper<Any>(it)[this]
    }

    operator fun <T : Any> Entity.get(component: Class<T>): T = world.getMapper(component)[this]
    operator fun <T : Any> Entity.get(component: Component<T>): T = world.getMapper(component)[this]
    operator fun <T : Any> EntityRef.get(component: Class<T>) = this.entity[component]
    operator fun <T : Any> EntityRef.get(component: Component<T>) = this.entity[component]

    fun Entity.has(component: Class<*>) = world.getMapper(component).has(this)
    fun Entity.has(component: Component<*>) = world.getMapper(component).has(this)
    fun EntityRef.has(component: Class<*>) = this.entity.has(component)
    fun EntityRef.has(component: Component<*>) = this.entity.has(component)
    fun Entity.hasAll(vararg component: Class<*>) = component.all { has(it) }
    fun Entity.hasAll(vararg component: Component<*>) = component.all { has(it) }
    fun EntityRef.hasAll(vararg component: Class<*>) = component.all { this.entity.has(it) }
    fun EntityRef.hasAll(vararg component: Component<*>) = component.all { this.entity.has(it) }

    fun <T : Any> Entity.obtainAndSet(component: Component<T>, block: (T) -> Unit = {}): T {
        val newComponent = component.obtain()
        block(newComponent)
        set(component, newComponent)
        return newComponent
    }
    fun <T : Any> EntityRef.obtainAndSet(component: Component<T>, block: (T) -> Unit = {}) = this.entity.obtainAndSet(component, block)

    fun <T : Any> Entity.remove(component: Class<T>) = world.getMapper(component).remove(this)
    fun <T : Any> Entity.remove(component: Component<T>) = world.getMapper(component).remove(this)
    fun <T : Any> EntityRef.remove(component: Class<T>) = this.entity.remove(component)
    fun <T : Any> EntityRef.remove(component: Component<T>) = this.entity.remove(component)

    operator fun <T : Any> Entity.set(component: Class<T>, value: T) { world.getMapper(component)[this] = value }
    operator fun <T : Any> Entity.set(component: Component<T>, value: T) { world.getMapper(component)[this] = value }
    operator fun <T : Any> EntityRef.set(component: Class<T>, value: T) { this.entity[component] = value }
    operator fun <T : Any> EntityRef.set(component: Component<T>, value: T) { this.entity[component] = value }

    fun Entity.update(block: Entity.() -> Unit) {
        this.block()
        world.update(this)
    }
    fun EntityRef.update(block: Entity.() -> Unit) = this.entity.update(block)

    fun Family.Builder.all(vararg component: Component<*>) = this.all(*component.map { it.clazz }.toTypedArray())
    fun Family.Builder.any(vararg component: Component<*>) = this.any(*component.map { it.clazz }.toTypedArray())
    fun Family.Builder.none(vararg component: Component<*>) = this.none(*component.map { it.clazz }.toTypedArray())
}