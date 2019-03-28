package org.nekobasu.core

import android.os.Bundle
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class RequestForResultParam(moduleClass: Class<out LifecycleUiModule<*, *, *>>,
                            val requestForResultId: Int) : Param(moduleClass)

// TODO create wrapping DSL to simplify updates
// i.e. execute(Update(param)), SwapTop(param), Remove(), ClearToTop)
abstract class ScreenStackViewModel : SingleUpdateViewModel<List<ScreenUpdate>>(), BackPressHandling {

    companion object {
        private val BACK_STACK = "BACK_STACK"
        private val TASK_CACHE = "TASK_CACHE"
        private val SCREEN_ID_COUNTER = "SCREEN_ID_COUNTER"
    }


    private val cachedTasks = mutableSetOf<ScreenUpdate>()
    private val screenIdCounter = AtomicInteger()

    // try to synchronize stack in it self
    private val stack by lazy {
        Stack<ScreenUpdate>().apply {
            push(getInitialScreen())
        }
    }

    override val initialViewUpdate: List<ScreenUpdate>
    get() = stack.toList()

    protected abstract fun getInitialScreen(): ScreenUpdate

    private fun pushScreenUpdates() {
        setViewUpdate(stack.toList())
    }

    override fun onSave(outBundle: Bundle) {
        outBundle.putInt(SCREEN_ID_COUNTER, screenIdCounter.get())
        outBundle.putParcelableArray(TASK_CACHE, cachedTasks.toTypedArray())
        outBundle.putParcelableArray(BACK_STACK, stack.toTypedArray())
    }

    override fun onRestore(inBundle: Bundle) {
        with(inBundle) {
            screenIdCounter.set(getInt(SCREEN_ID_COUNTER))

            getParcelableArray(TASK_CACHE)?.let {
                cachedTasks.clear()
                it.forEach { data -> cachedTasks.add(data as ScreenUpdate) }
            }

            getParcelableArray(BACK_STACK)?.let {
                synchronized(stack) {
                    stack.clear()
                    it.forEach { data -> stack.push(data as ScreenUpdate) }
                    pushScreenUpdates()
                }
            }
        }
    }

    protected fun popOnce() {
        popUpdate(1)
    }

    protected fun popToLanding() {
        synchronized(stack) {
            popUpdate(stack.size - 1)
        }
    }

    protected fun popUpdate(times: Int) {
        synchronized(stack) {
            for (i in 1..times) {
                if (!stack.isEmpty()) {
                    stack.pop()
                }
            }
            pushScreenUpdates()
        }
    }

    protected fun popForRequest(requestForResultId: Int, result: Result) {
        synchronized(stack) {
            while (!stack.isEmpty()) {
                val currentData = stack.peek()
                if (currentData.lastRequestForResultId == requestForResultId) {
                    currentData.result = result
                    currentData.paramsUpdatedByRequest = true

                    val paramsRequestId = (currentData.params as? RequestForResultParam)?.requestForResultId
                    if (paramsRequestId != requestForResultId) {
                        break
                    }
                }
                stack.pop()
            }

            if (stack.empty()) {
                throw IllegalStateException("Failed to fin the requestId: $requestForResultId for result in the stack.")
            } else {
                pushScreenUpdates()
            }
        }
    }

    protected fun pushUpdate(params: Param, taskId: String = STANDALONE_TASK,
                             taskCacheComparator: (ScreenUpdate, ScreenUpdate) -> Boolean = ::isUpdateWithSameSignature) {
        synchronized(stack) {
            if (!stack.empty()) {
                stack.peek().lastRequestForResultId = (params as? RequestForResultParam)?.requestForResultId
                        ?: NO_RESULT_REQUESTED
                assert(taskId == STANDALONE_TASK || stack.peek().taskId != taskId) {
                    "You pushed $params to the stack on top of ${stack.peek()}. They belong to the same taskId: $taskId. " +
                            "This would corrupt the stack. Please use swapUpdate() instead, or change an update to a different taskId."
                }
            }
            var dataToPush = ScreenUpdate(params = params, taskId = taskId)
            if (taskId != STANDALONE_TASK) {
                dataToPush = cacheTask(dataToPush, taskCacheComparator)
            } else {
                dataToPush.screenId = getNextScreenId()
            }

            stack.push(dataToPush)
            pushScreenUpdates()
        }
    }

    protected fun isSameTaskOnTop(taskId: String): Boolean {
        synchronized(stack) {
            return !stack.empty() && stack.peek().isPartOfATask() && stack.peek().taskId == taskId
        }
    }

    protected fun isSameOnTop(params: Param): Boolean {
        synchronized(stack) {
            return !stack.empty() && stack.peek().params::class == params::class
        }
    }

    private fun cacheTask(update: ScreenUpdate, taskCacheComparator: (ScreenUpdate, ScreenUpdate) -> Boolean = ::isUpdateWithSameSignature): ScreenUpdate {
        val cachedData = cachedTasks.find { taskCacheComparator(it, update) }

        return when (cachedData) {
            null -> {
                update.screenId = getNextScreenId()
                cachedTasks.add(update)
                update
            }
            else -> cachedData
        }
    }

    private fun isUpdateWithSameSignature(oldScreenUpdate: ScreenUpdate, newScreenUpdate: ScreenUpdate) =
            oldScreenUpdate.params::class == newScreenUpdate.params::class
                    && oldScreenUpdate.taskId == newScreenUpdate.taskId

    protected val isUpdateWithSameSignatureAndSameParams: (ScreenUpdate, ScreenUpdate) -> Boolean =
            { oldFlowData: ScreenUpdate, newFlowData: ScreenUpdate ->
                oldFlowData.params::class.java == newFlowData.params::class.java
                        && oldFlowData.taskId == newFlowData.taskId
                        && oldFlowData.params == newFlowData.params
            }


    private fun getNextScreenId() = screenIdCounter.incrementAndGet().toString()

    protected fun swapUpdate(params: Param, taskId: String = STANDALONE_TASK,
                             comparator: (ScreenUpdate, ScreenUpdate) -> Boolean = ::isUpdateWithSameSignature) {
        synchronized(stack) {
            if (stack.isNotEmpty() && stack.pop().params == params && stack.isNotEmpty()) {
                pushScreenUpdates()
            }
            pushUpdate(params, taskId, comparator)
        }
    }

    protected fun resetUpdate(params: Param, taskId: String = STANDALONE_TASK) {
        synchronized(stack) {
            stack.clear()
            pushUpdate(params, taskId)
        }
    }

    override fun onBackPress(): Boolean {
        popOnce()
        return true
    }
}