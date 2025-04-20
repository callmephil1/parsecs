package com.github.callmephil1.parsecs.ecs.task

import kotlinx.coroutines.Job

abstract class Task {
    internal val dependencies = mutableListOf<Task>()

    var job: Job? = null
        internal set

    open fun onComplete() {}
    abstract suspend fun onExecute()

    fun runAfter(vararg task: Task) {
        task.forEach { dependencies.add(it) }
    }
}

internal class AnonymousTask(
    private val onExecuteFunc: suspend () -> Unit,
    private val onCompleteFunc: () -> Unit = {}
) : Task() {
    override suspend fun onExecute() {
        onExecuteFunc()
    }

    override fun onComplete() {
        onCompleteFunc()
    }
}

internal class ChainTask<T>(
    private val onExecuteFunc: suspend () -> T,
    private val onCompleteFunc: (T) -> Unit
) : Task() {
    var value: T? = null

    override suspend fun onExecute() {
        value = onExecuteFunc()
    }

    override fun onComplete() {
        onCompleteFunc(value!!)
    }
}