package com.github.callmephil1.parsecs.ecs.family

import com.github.callmephil1.parsecs.ecs.component.ComponentService
import com.github.callmephil1.parsecs.ecs.entity.Entity

class FamilyService(
    private val componentService: ComponentService
) {
    private val _families: MutableList<Family> = mutableListOf()

    internal fun addEntity(entity: Entity) {
        _families.forEach {
            if (it.isInterested(entity))
                it.add(entity)
        }
    }

    internal fun addFamily(family: Family) {
        _families.add(family)
    }

    internal fun clear() {
        _families.clear()
    }

    fun family(configure: Family.Builder.() -> Unit): Family {
        val builder = Family.Builder(componentService = componentService)
        configure(builder)
        val family = builder.build()
        _families.add(family)
        return family
    }

    fun release(entity: Entity, with: Entity?) {
        _families.forEach {
            it.remove(entity, with)
        }
    }

    fun update(entity: Entity) {
        for(i in _families.indices) {
            val family = _families[i]

            if (family.has(entity)) {
                if (family.isInterested(entity)) {
                    family.update(entity)
                } else {
                    family.remove(entity, null)
                }
            } else if (family.isInterested(entity)) {
                family.add(entity)
            }
        }
    }
}