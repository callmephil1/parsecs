package com.github.callmephil1.parsecs.ecs.entity

import com.github.callmephil1.parsecs.ecs.collections.Bits

class Entity {
    internal val componentMask = Bits()
    var index = -1
        internal set
    var version: UInt = 0u
        internal set
    var tag: String = ""

    fun ref(): EntityRef = EntityRef(this)

    override fun toString() = "Entity(index: $index, version: $version, tag: '$tag', hashCode: ${hashCode()})"
}