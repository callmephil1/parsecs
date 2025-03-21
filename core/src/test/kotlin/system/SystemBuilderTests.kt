package system

import com.github.callmephil1.parsecs.ecs.collections.CircularReference
import com.github.callmephil1.parsecs.ecs.system.AsynchronousSequentialSystemService
import com.github.callmephil1.parsecs.ecs.system.AsynchronousUpdateSystem
import com.github.callmephil1.parsecs.ecs.system.SystemDescriptor
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertContentEquals

private object SystemA : AsynchronousUpdateSystem {
    override val descriptor: SystemDescriptor = systemDescriptor {}

    override fun update(delta: Float) {}
}

private object SystemB : AsynchronousUpdateSystem {
    override val descriptor: SystemDescriptor = systemDescriptor {
        dependsOn(SystemA::class.java)
    }

    override fun update(delta: Float) {}
}

private object SystemC : AsynchronousUpdateSystem {
    override val descriptor: SystemDescriptor = systemDescriptor {
        groups("groupa")
    }

    override fun update(delta: Float) {}
}

private object SystemD : AsynchronousUpdateSystem {
    override val descriptor: SystemDescriptor = systemDescriptor {
        groups("groupa")
        dependsOn(SystemB::class.java)
    }

    override fun update(delta: Float) {}
}

private object SystemE : AsynchronousUpdateSystem {
    override val descriptor: SystemDescriptor = systemDescriptor {
        dependsOn("groupa")
    }

    override fun update(delta: Float) {}
}

private object SystemCircurlarA : AsynchronousUpdateSystem {
    override val descriptor: SystemDescriptor = systemDescriptor {
        dependsOn(SystemCircurlarB::class.java)
    }

    override fun update(delta: Float) {}
}

private object SystemCircurlarB : AsynchronousUpdateSystem {
    override val descriptor: SystemDescriptor = systemDescriptor {
        dependsOn(SystemCircurlarA::class.java)
    }

    override fun update(delta: Float) {}
}


class SystemBuilderTests {
    @Test
    fun `Build an AsynchronousDAGSystemService with systems and groups and steps are arranged correctly`() {
        val service = AsynchronousSequentialSystemService.Builder().build(
            listOf(
                SystemA,
                SystemB,
                SystemC,
                SystemD,
                SystemE
            )
        )
        val steps = service.updateExecutionSteps

        assert(steps.size == 4)
        assertContentEquals(steps[0], listOf(SystemA, SystemC))
        assertContentEquals(steps[1], listOf(SystemB))
        assertContentEquals(steps[2], listOf(SystemD))
        assertContentEquals(steps[3], listOf(SystemE))
    }

    @Test
    fun `When building an AsynchronousDAGSystemService if there is a circular reference then exception should be thrown`() {
        assertThrows<CircularReference> {
            AsynchronousSequentialSystemService.Builder().build(
                listOf(
                    SystemCircurlarA,
                    SystemCircurlarB
                )
            )
        }
    }
}