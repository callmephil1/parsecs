package com.github.callmephil1.parsecs.ecs.task

import kotlinx.coroutines.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class TaskDependencyNotStarted : Exception("Started a task before all it's dependencies had started.")

internal enum class Phase {
    PreUpdate,
    Update,
    PostUpdate,
    Draw,
    PostDraw
}

interface TaskService {

    fun cancelAllTasks()

    fun <T> chainTask(onComplete: (T) -> Unit, onExecute: suspend () -> T): Task

    fun scheduleToDraw(task: Task, context: CoroutineContext = Dispatchers.Default)

    fun scheduleToUpdate(task: Task, context: CoroutineContext = Dispatchers.Default)

    fun scheduleUnconfined(task: Task, context: CoroutineContext = Dispatchers.Default)

    fun task(onComplete: () -> Unit = {}, onExecute: suspend () -> Unit): Task
}

class TaskServiceImpl : TaskService {
    private var phase: Phase = Phase.PreUpdate
    private val scope = CoroutineScope(EmptyCoroutineContext)

    private val lock = ReentrantLock()
    private val runningLock = ReentrantLock()

    private val drawTasks: MutableList<Task> = mutableListOf()
    private val updateTasks: MutableList<Task> = mutableListOf()

    private val runningDrawTasks: MutableList<Task> = mutableListOf()
    private val runningUpdateTasks: MutableList<Task> = mutableListOf()

    override fun cancelAllTasks() {
        drawTasks.clear()
        updateTasks.clear()
        scope.cancel("'cancelAllTasks' was called and canceled all current and future tasks")
    }

    override fun <T> chainTask(onComplete: (T) -> Unit, onExecute: suspend () -> T): Task = ChainTask(
        onExecuteFunc = onExecute,
        onCompleteFunc = onComplete,
    )

    private fun getDependencyJobs(task: Task): List<Job> {
        val jobs = task.dependencies.map { it.job }
        if (jobs.any { it == null })
            throw TaskDependencyNotStarted()
        return jobs.requireNoNulls()
    }

    internal fun joinDrawTasks() = runBlocking {
        phase = Phase.PostDraw

        var index = 0
        while (index < runningDrawTasks.size) {
            val task = runningDrawTasks[index]
            task.job!!.join()
            index += 1
        }

        index = 0
        while (index < runningDrawTasks.size) {
            val task = runningDrawTasks[index]
            task.onComplete()
            index += 1
        }

        runningDrawTasks.clear()
    }

    internal fun joinUpdateTasks() = runBlocking {
        phase = Phase.PostUpdate

        var index = 0
        while (true) {
            val size = runningLock.withLock { runningUpdateTasks.size }
            if (index >= size)
                break

            val task = runningUpdateTasks[index]
            task.job!!.join()
            index += 1
        }

        index = 0
        while (true) {
            val size = runningLock.withLock { runningUpdateTasks.size }
            if (index >= size)
                break

            val task = runningUpdateTasks[index]
            task.onComplete()
            index += 1
        }

        runningLock.withLock { runningUpdateTasks.clear() }
    }

    override fun scheduleToDraw(task: Task, context: CoroutineContext) {
        if (task.job != null)
            throw IllegalArgumentException("Tried to schedule a task that is already scheduled")

        if (phase == Phase.Draw) {
            task.job = taskExecutor(task, context)
            runningLock.withLock { runningDrawTasks.add(task) }

        } else {
            task.job = taskExecutor(task, context, CoroutineStart.LAZY)
            lock.withLock { drawTasks.add(task) }
        }
    }

    override fun scheduleUnconfined(task: Task, context: CoroutineContext) {
        if (task.job != null)
            throw IllegalArgumentException("Tried to schedule a task that is already scheduled")
        task.job = taskExecutor(task, context)
    }

    override fun scheduleToUpdate(task: Task, context: CoroutineContext) {
        if (task.job != null)
            throw IllegalArgumentException("Tried to schedule a task that is already scheduled")

        if (phase == Phase.Update) {
            task.job = taskExecutor(task, context)
            runningLock.withLock { runningUpdateTasks.add(task) }

        } else {
            task.job = taskExecutor(task, context, CoroutineStart.LAZY)
            lock.withLock { updateTasks.add(task) }
        }
    }

    internal fun startDrawTasks() = lock.withLock {
        phase = Phase.Draw

        while (true) {
            val task = drawTasks.removeLastOrNull() ?: break

            task.job!!.start()
            runningLock.withLock { runningDrawTasks.add(task) }
        }
    }

    internal fun startUpdateTasks() {
        phase = Phase.Update

        while (true) {
            val task = updateTasks.removeLastOrNull() ?: break

            task.job!!.start()
            runningLock.withLock { runningUpdateTasks.add(task) }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun taskExecutor(task: Task, context: CoroutineContext, start: CoroutineStart = CoroutineStart.DEFAULT) : Job {
        val dependencies = getDependencyJobs(task)

        val job = scope.launch(
            context = context,
            start = start
        ) {
            dependencies.forEach { it.join() }
            task.onExecute()
        }
        job.invokeOnCompletion { throwable ->
            task.thrownException = throwable
        }

        return job
    }

    override fun task(onComplete: () -> Unit, onExecute: suspend () -> Unit): Task = AnonymousTask(onExecute, onComplete)
}