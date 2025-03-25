package com.github.callmephil1.parsecs.ecs.component

import com.github.callmephil1.parsecs.ecs.component.dsl.Component
import com.github.callmephil1.parsecs.ecs.entity.Entity
import io.github.oshai.kotlinlogging.KotlinLogging

class ComponentService internal constructor() {
    private var mapperCount = 0
    private val componentMappers = mutableListOf<ComponentMapper<*>>()
    private val componentMapperIndices = mutableMapOf<Class<*>, Int>()
    private val logger = KotlinLogging.logger { }

    fun <T> getMapper(index: Int) = componentMappers[index] as ComponentMapper<T>

    fun <T> getMapper(clazz: Class<T>): ComponentMapper<T> {
        val index = componentMapperIndices[clazz]

        if (index != null)
            return componentMappers[index] as ComponentMapper<T>

        val newMapper = ComponentMapper(
            clazz = clazz,
            index = mapperCount++
        )
        componentMapperIndices[clazz] = newMapper.index
        componentMappers.add(newMapper)
        return newMapper
    }

    fun <T> getMapper(component: Component<T>): ComponentMapper<T> {
        if (component.index == -1) {
            val mapper = getMapper(component.clazz)
            component.index = mapper.index
        }
        return componentMappers[component.index] as ComponentMapper<T>
    }

    internal fun <T> getComponentIndex(clazz: Class<T>) = getMapper(clazz).index

    internal fun release(entity: Entity, with: Entity) {
        logger.debug { "Releasing $entity with $with" }

        for (i in componentMappers.indices)
            componentMappers[i].release(entity = entity, with = with)
    }
}