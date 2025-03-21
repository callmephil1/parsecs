package collections

import com.github.callmephil1.parsecs.ecs.collections.CircularReference
import com.github.callmephil1.parsecs.ecs.collections.DirectedAcyclicGraph
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

private class DAGClass
private class DAGClass2
private class DAGClass3
private class DAGClass4
private class DAGClass5

class DirectedAcyclicGraphTests {
    @Test
    fun `Create a DAG with dependencies and call toExecutionSteps`() {
        val dag = DirectedAcyclicGraph<Class<*>>()
        dag.addWithDependents(DAGClass::class.java, listOf(DAGClass2::class.java, DAGClass3::class.java))
        dag.addWithDependents(DAGClass2::class.java, listOf(DAGClass4::class.java, DAGClass5::class.java))
        dag.addWithDependents(DAGClass3::class.java, listOf(DAGClass4::class.java))
        dag.addWithDependents(DAGClass4::class.java, listOf())
        dag.addWithDependents(DAGClass5::class.java, listOf())

        val steps = dag.toExecutionSteps()

        assertEquals(listOf(DAGClass::class.java), steps[0])
        assertEquals(listOf(DAGClass2::class.java, DAGClass3::class.java), steps[1])
        assertEquals(listOf(DAGClass5::class.java, DAGClass4::class.java), steps[2])
    }

    @Test
    fun `Create a DAG with circular dependency and should throw CircularReference exception`() {
        assertThrows<CircularReference> {
            val dag = DirectedAcyclicGraph<Class<*>>()
            dag.addWithDependents(DAGClass::class.java, listOf(DAGClass2::class.java))
            dag.addWithDependents(DAGClass2::class.java, listOf(DAGClass::class.java))
        }
    }
}