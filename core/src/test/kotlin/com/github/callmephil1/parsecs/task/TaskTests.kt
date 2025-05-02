package com.github.callmephil1.parsecs.task

import com.github.callmephil1.parsecs.ecs.task.AnonymousTask
import com.github.callmephil1.parsecs.ecs.task.ChainTask
import com.github.callmephil1.parsecs.ecs.task.TaskDependencyNotStarted
import com.github.callmephil1.parsecs.ecs.task.TaskService
import com.github.callmephil1.parsecs.ecs.task.TaskServiceImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaskTests {

    private val taskService: TaskService = TaskServiceImpl()

    @Test
    fun `Create non-nullable chain task and should run execute and complete successfully`() {
        val task = ChainTask(
            onExecuteFunc = { "test" },
            onCompleteFunc = { assertEquals("test", it) }
        )

        runBlocking {
            task.onExecute()
        }
        task.onComplete()
    }

    @Test
    fun `Create nullable chain task and should run execute and complete successfully`() {
        val task = ChainTask<String?>(
            onExecuteFunc = { null },
            onCompleteFunc = { assertEquals(null, it) }
        )

        runBlocking {
            task.onExecute()
        }
        task.onComplete()
    }

    @Test
    fun `Starting a task before dependent throws TaskDependencyNotStarted`() {
        assertThrows<TaskDependencyNotStarted> {
            val dependent = AnonymousTask({}, {})

            val task = AnonymousTask({}, {}).runAfter(dependent)

            taskService.scheduleUnconfined(task)
        }
    }

    @Test
    fun `Starting a task with dependents should run successfully`() {
        val list = ConcurrentLinkedDeque<Int>()

        val dependent1 = AnonymousTask({ list.add(0) }, {})
        val dependent2 = AnonymousTask({ list.add(1) }, {})
        val dependent3 = AnonymousTask({ list.add(2) }, {})

        val task = AnonymousTask({
            assert(list.containsAll(listOf(0, 1, 2)))
        }, {}).runAfter(dependent1, dependent2, dependent3)

        taskService.scheduleUnconfined(dependent1)
        taskService.scheduleUnconfined(dependent2)
        taskService.scheduleUnconfined(dependent3)
        taskService.scheduleUnconfined(task)

        runBlocking {
            task.job?.join()
        }

        assertEquals(null, task.thrownException)
    }

    @Test
    fun `Starting tasks then call cancelAllJobs should cancel all running jobs`() {
        val task1 = taskService.task { delay(3000) }
        val task2 = taskService.task { delay(3000) }

        taskService.scheduleUnconfined(task1)
        taskService.scheduleUnconfined(task2)

        taskService.cancelAllTasks()

        assertTrue { task1.job?.isCancelled == true }
        assertTrue { task2.job?.isCancelled == true }
    }
}