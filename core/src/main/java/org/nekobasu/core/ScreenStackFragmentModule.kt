package org.nekobasu.core

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

abstract class ScreenStackFragmentModule<V : ScreenStackViewModel, P : Param>(
        param: P,
        private val fragmentCreator: (ScreenUpdate) -> Fragment = {
            UiModules.fragmentFromModule(it.params)
        }
) : LifecycleUiModule<List<ScreenUpdate>, V, P>(param) {

    override lateinit var context: FragmentActivity
    private lateinit var fragmentManager: FragmentManager
    private var oldScreenUpdateStack: List<ScreenUpdate> = emptyList()
    private val taskTagsCache: MutableSet<String> = mutableSetOf()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        context = inflater.context as FragmentActivity
        fragmentManager = context.supportFragmentManager
        return null
    }

    override fun onInitView(view: View, savedInstanceState: Bundle?) {}

    override fun onViewUpdate(viewUpdate: List<ScreenUpdate>) {
        if (viewUpdate.isEmpty()) {
            context.finish()
        } else {
            showFragments(
                    newStack = viewUpdate,
                    oldStack = oldScreenUpdateStack,
                    fragmentCreator = fragmentCreator)
        }
        oldScreenUpdateStack = viewUpdate
    }


    fun showFragments(newStack: List<ScreenUpdate>, oldStack: List<ScreenUpdate>, fragmentCreator: (ScreenUpdate) -> Fragment) {
        val transaction = fragmentManager.beginTransaction().apply { disallowAddToBackStack() }
        val newStackTasks = newStack.map { it.taskId }
        val newStackTags = newStack.map { it.tag() }

        for (screen in oldStack) {
            val existingFragment = findByTag(screen.tag())
            if (existingFragment != null) {
                if (!newStackTags.contains(screen.tag()) && (!screen.isPartOfATask() || !newStackTasks.contains(screen.taskId))) {
                    transaction.remove(existingFragment)
                    transaction.cleanUpTask(screen.taskId)
                } else if (!existingFragment.isDetached) {
                    transaction.detach(existingFragment)
                    handleScreenGroup(screen)
                }
            }
        }

        for (screen in newStack) {
            val existingFragment = findByTag(screen.tag())
            if (screen == newStack.last()) {
                if (existingFragment != null) {
                    if (existingFragment.isDetached) {
                        transaction.attach(existingFragment)
                        setResultIfNeeded(screen, existingFragment)
                    }
                } else {
                    val newFragment = fragmentCreator(screen)
                    transaction.add(mainContainerId(), newFragment, screen.tag())
                    setResultIfNeeded(screen, newFragment)
                }
            } else {
                if (existingFragment != null && !existingFragment.isDetached) {
                    transaction.detach(existingFragment)
                }
            }
        }

        transaction.commitNow()
    }

    private fun handleScreenGroup(screenData: ScreenUpdate) {
        if (screenData.isPartOfATask()) {
            taskTagsCache.add(screenData.tag())
        }
    }

    private fun FragmentTransaction.cleanUpTask(taskId: String) {
        if (taskId != STANDALONE_TASK) {
            taskTagsCache.filter { getTaskIdFromTag(it) == taskId }.forEach { tag ->
                findByTag(tag)?.let { remove(it) }
            }
            taskTagsCache.removeAll { getTaskIdFromTag(it) == taskId }
        }
    }

    private fun setResultIfNeeded(screenUpdate: ScreenUpdate, fragment: Fragment) {
        if (screenUpdate.paramsUpdatedByRequest) {
            screenUpdate.paramsUpdatedByRequest = false
            val lastRequestForResultId = screenUpdate.lastRequestForResultId
            screenUpdate.lastRequestForResultId = NO_RESULT_REQUESTED

            val interUiContract = fragment as InterUiContract
            interUiContract.deliverResult(RequestedResult(lastRequestForResultId, screenUpdate.result))
            interUiContract.updateParams(params)
        }
    }

    private fun findByTag(tag: String) = fragmentManager.findFragmentByTag(tag)

    private fun getTaskIdFromTag(tag: String?) = tag?.substringAfter("#", STANDALONE_TASK)
            ?: STANDALONE_TASK

    @IdRes
    protected abstract fun mainContainerId(): Int

    override fun onBackPress(): Boolean {
        val topFragment = fragmentManager.fragments.lastOrNull()
        val customBack = (topFragment as? BackPressHandling)?.onBackPress() ?: false
        if (!customBack) {
            viewModel.onBackPress()
        }
        return true
    }
}