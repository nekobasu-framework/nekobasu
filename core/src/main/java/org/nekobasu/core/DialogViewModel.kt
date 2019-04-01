package org.nekobasu.core

import java.util.*

inline class DialogId(val id : Int) {
    companion object {
        val random = Random()
        fun nextId() = DialogId(random.nextInt())
    }
}

inline class InteractionId(val id : Int)

val NoDialogId = DialogId(-1)

interface DialogResult

interface DialogUpdateContract {
    val dialogId : DialogId
}

interface DialogCallbackTarget {
    fun onDialogRemoved(dialogId: DialogId)

    fun onDialogInteractionPerformed(dialogId: DialogId, interactionId: InteractionId)

    fun onDialogResult(dialogId: DialogId, dialogResult: DialogResult)
}

interface DialogControl {
    fun cancel()
}

interface DialogCallback {
    fun onDialogRemoved()
    fun onDialogInteractionPerformed(interactionId: InteractionId)
    fun onDialogResult(dialogResult: DialogResult)
}

interface DialogChannel : Channel {
    fun showDialog(dialogUpdate: DialogUpdateContract, callback: DialogCallback = NoDialogCallback): DialogControl
}

object NoDialogCallback : DialogCallback {
    override fun onDialogRemoved() {}
    override fun onDialogInteractionPerformed(interactionId: InteractionId) {}
    override fun onDialogResult(dialogResult: DialogResult) {}
}

abstract class DialogUpdate(override val dialogId: DialogId) : DialogUpdateContract
object NoDialog : DialogUpdate(NoDialogId)



open class DialogViewModel : SingleUpdateViewModel<DialogUpdateContract>(), DialogCallbackTarget, DialogChannel {
    override val initialViewUpdate = NoDialog
    private val handlers = mutableMapOf<DialogId, DialogCallbackTarget>()

    override fun showDialog(dialogUpdate: DialogUpdateContract, callback: DialogCallback): DialogControl {
        setViewUpdate(dialogUpdate)
        handleDialogCallbacks(dialogUpdate, callback)

        return hideOnCancel(dialogUpdate)
    }

    protected fun hideOnCancel(dialogData: DialogUpdateContract): DialogControl {
        return object : DialogControl {
            override fun cancel() {
                unregisterDialogHandler(dialogData.dialogId)
                if (getViewUpdate().dialogId == dialogData.dialogId) {
                    removeDialog()
                }
            }
        }
    }

    protected fun handleDialogCallbacks(dialogUpdate: DialogUpdateContract, callback: DialogCallback) {
        val handler: DialogCallbackTarget = object : DialogCallbackTarget {
            override fun onDialogRemoved(dialogId: DialogId) {
                if (dialogUpdate.dialogId == dialogId) {
                    unregisterDialogHandler(dialogId)
                    removeDialog()
                    callback.onDialogRemoved()
                }
            }

            override fun onDialogInteractionPerformed(dialogId: DialogId, interactionId: InteractionId) {
                if (dialogUpdate.dialogId == dialogId) {
                    unregisterDialogHandler(dialogId)
                    removeDialog()
                    callback.onDialogInteractionPerformed(interactionId)
                }
            }

            override fun onDialogResult(dialogId: DialogId, dialogResult: DialogResult) {
                if (dialogUpdate.dialogId == dialogId) {
                    unregisterDialogHandler(dialogId)
                    removeDialog()
                    callback.onDialogResult(dialogResult)
                }
            }
        }
        registerDialogHandler(dialogUpdate.dialogId, handler)
    }

    protected open fun removeDialog() {
        setViewUpdate(NoDialog)
    }

    override fun onDialogRemoved(dialogId: DialogId) {
        handlers.values.toList().forEach { it.onDialogRemoved(dialogId) }
    }

    override fun onDialogInteractionPerformed(dialogId: DialogId, interactionId: InteractionId) {
        handlers.values.toList().forEach { it.onDialogInteractionPerformed(dialogId, interactionId) }
    }

    override fun onDialogResult(dialogId: DialogId, dialogResult: DialogResult) {
        handlers.values.toList().forEach { it.onDialogResult(dialogId, dialogResult) }
    }


    private fun registerDialogHandler(dialogId: DialogId, dialogHandler: DialogCallbackTarget) {
        handlers.put(dialogId, dialogHandler)
    }

    private fun unregisterDialogHandler(dialogId: DialogId) {
        handlers.remove(dialogId)
    }
}