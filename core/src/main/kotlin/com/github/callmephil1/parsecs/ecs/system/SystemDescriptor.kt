package com.github.callmephil1.parsecs.ecs.system

import com.github.callmephil1.parsecs.ecs.system.System as ISystem

sealed class Dependency {
    class Group(val identifier: Any) : Dependency()
    class System(val clazz: Class<out ISystem>) : Dependency()
}

class SystemDescriptor internal constructor(
    val dependencies: List<Dependency>,
    val groups: List<Any>
) {
    class Builder internal constructor() {
        private val dependencies = mutableSetOf<Dependency>()
        private val groups = mutableSetOf<Any>()

        fun build(): SystemDescriptor {
            return SystemDescriptor(
                dependencies = dependencies.toList(),
                groups = groups.toList()
            )
        }

        fun dependsOn(vararg group: Any) {
            dependencies.addAll(group.map { Dependency.Group(it) })
        }

        fun dependsOn(vararg system: Class<out ISystem>) {
            dependencies.addAll(system.map { Dependency.System(it) })
        }

        fun groups(vararg group: Any) {
            groups.addAll(group)
        }
    }

    companion object {
        val NONE = Builder().build()
    }
}
