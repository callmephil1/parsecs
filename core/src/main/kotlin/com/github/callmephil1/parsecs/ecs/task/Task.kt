package com.github.callmephil1.parsecs.ecs.task

import kotlinx.coroutines.Job

abstract class Task {
    internal val dependencies = mutableListOf<Task>()

    var job: Job? = null
        internal set
    var thrownException: Throwable? = null
        internal set

    open fun onComplete() {}
    abstract suspend fun onExecute()

    fun runAfter(vararg task: Task): Task {
        task.forEach { dependencies.add(it) }
        return this
    }
}

internal class AnonymousTask(
    private val onExecuteFunc: suspend () -> Unit,
    private val onCompleteFunc: () -> Unit
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
    private lateinit var wrapperFunc: () -> Unit

    override suspend fun onExecute() {
        val value = onExecuteFunc()
        wrapperFunc = { onCompleteFunc(value) }
    }

    override fun onComplete() {
        wrapperFunc()
    }
}