package com.github.callmephil1.parsecs.ecs.system.dsl

import com.github.callmephil1.parsecs.ecs.system.System

abstract class System<T : System> {
    abstract val clazz: Class<T>
}