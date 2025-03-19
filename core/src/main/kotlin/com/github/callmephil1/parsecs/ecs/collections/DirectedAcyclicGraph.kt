package com.github.callmephil1.parsecs.ecs.collections

class DirectedAcyclicGraph<T> {
    private val adjacencyList = mutableMapOf<T, MutableSet<T>>()
    private val inDegrees = mutableMapOf<T, Int>()

    fun addWithDependents(item: T, dependents: List<T>) {
        val list = adjacencyList.getOrPut(item) { mutableSetOf() }
        list.addAll(dependents)
        inDegrees.putIfAbsent(item, 0)

        dependents.forEach {
            inDegrees.getOrPut(it) { 0 }
            inDegrees[it] = inDegrees[it]!! + 1
        }
    }

    fun addWithDependencies(item: T, dependencies: List<T>) {
        dependencies.forEach {
            addWithDependents(it, listOf(item))
        }
    }

    fun toExecutionSteps(): List<List<T>> {
        val deque = ArrayDeque<T>()

        for ((item, degree) in inDegrees) {
            if (degree == 0) {
                deque.add(item)
            }
        }

        val steps = mutableListOf<List<T>>()

        while(deque.isNotEmpty()) {
            val step = mutableListOf<T>()
            val size = deque.size

            repeat(size) {
                val item = deque.removeFirst()
                step.add(item)

                adjacencyList[item]?.forEach {
                    inDegrees[it] = inDegrees[it]!! - 1
                    if (inDegrees[it] == 0)
                        deque.add(it)
                }
            }

            steps.add(step)
        }

        return steps
    }
}