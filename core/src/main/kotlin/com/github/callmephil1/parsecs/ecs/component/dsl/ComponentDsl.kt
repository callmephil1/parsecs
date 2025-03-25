package com.github.callmephil1.parsecs.ecs.component.dsl

import com.github.callmephil1.parsecs.ecs.component.ComponentService
import com.github.callmephil1.parsecs.ecs.entity.Entity

interface ComponentDsl {
    val componentService: ComponentService

    fun <T : Any> Component<T>.obtain() = componentService.getMapper(this).obtain()

    fun Entity.allComponents() : List<Any> = this.componentMask.getSetIndicies().map {
            componentService.getMapper<Any>(it)[this]
        }

    operator fun <T : Any> Entity.get(component: Class<T>): T = componentService.getMapper(component)[this]
    operator fun <T : Any> Entity.get(component: Component<T>): T = componentService.getMapper(component)[this]
    fun <T : Any> Entity.has(component: Class<T>) = componentService.getMapper(component).has(this)
    fun <T : Any> Entity.has(component: Component<T>) = componentService.getMapper(component).has(this)
    fun <T : Any> Entity.obtainAndSet(component: Component<T>, block: (T) -> Unit = {}): T {
        val newComponent = component.obtain()
        block(newComponent)
        set(component, newComponent)
        return newComponent
    }
    fun <T : Any> Entity.remove(component: Class<T>) = componentService.getMapper(component).remove(this)
    fun <T : Any> Entity.remove(component: Component<T>) = componentService.getMapper(component).remove(this)
    operator fun <T : Any> Entity.set(component: Class<T>, value: T) { componentService.getMapper(component)[this] = value }
    operator fun <T : Any> Entity.set(component: Component<T>, value: T) { componentService.getMapper(component)[this] = value }
}
