package org.nekobasu.core

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent

private const val DIALOG_BUNDLE = "dialog_bundle"

interface DialogViewCallback {
    fun onCanceledByOutside(dialogId: DialogId)
    fun onInteractionPerformed(dialogId: DialogId, interactionId: InteractionId)
    fun onResult(dialogId: DialogId, dialogResult: DialogResult)
}

interface DialogCreator {
    fun createDialog(context: Context, dialogUpdate: DialogUpdateContract, savedInstanceState: Bundle? = null, callback: DialogViewCallback): Dialog
}

class DialogParam(val dialogCreatorClass: Class<DialogCreator>) : Param(DialogModule::class.java)

open class DialogModule(param: DialogParam) : LifecycleUiModule<DialogUpdateContract, DialogViewModel, DialogParam>(param) {

    override lateinit var context: Context
    private val _dialogCreator: DialogCreator by lazy {
        param.dialogCreatorClass.newInstance()
    }
    protected val dialogHandler = DialogLifecycleHandler()
    private var savedInstanceState: Bundle? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        context = inflater.context
        this.savedInstanceState = savedInstanceState
        val lifecycleOwner = context as LifecycleOwner
        lifecycleOwner.lifecycle.addObserver(dialogHandler)
        return null
    }

    override fun onInitView(view: View, savedInstanceState: Bundle?) {}

    override fun onViewUpdate(viewUpdate: DialogUpdateContract) {
        if (viewUpdate != NoDialog) {
            showDialog(viewUpdate)
        } else {
            dialogHandler.onStop()
            dialogHandler.reset()
        }
    }

    open fun getDialogCreator(): DialogCreator = _dialogCreator

    private fun showDialog(dialogUpdate: DialogUpdateContract) {
        val savedDialogBundle = savedInstanceState?.getBundle(DIALOG_BUNDLE)

        val dialog = getDialogCreator().createDialog(context, dialogUpdate, savedDialogBundle, object : DialogViewCallback {
            override fun onResult(dialogId: DialogId, dialogResult: DialogResult) {
                dialogHandler.reset()
                viewModel.onDialogResult(dialogId, dialogResult)
            }

            override fun onCanceledByOutside(dialogId: DialogId) {
                dialogHandler.reset()
                viewModel.onDialogRemoved(dialogId)
            }

            override fun onInteractionPerformed(dialogId: DialogId, interactionId: InteractionId) {
                dialogHandler.reset()
                viewModel.onDialogInteractionPerformed(dialogId, interactionId)
            }
        })
        dialogHandler.register(dialog)
        dialog.show()
        if (savedDialogBundle != null) {
            // Discard the already shown state
            savedInstanceState?.remove(DIALOG_BUNDLE)
        }
    }

    override fun onSave(outBundle: Bundle) {
        super.onSave(outBundle)
        dialogHandler.dialog?.onSaveInstanceState()?.let { outBundle.putBundle(DIALOG_BUNDLE, it) }
    }

    override fun getViewModelClass(params: DialogParam): Class<DialogViewModel> = DialogViewModel::class.java
}

class DialogLifecycleHandler : LifecycleObserver {

    var dialog: Dialog? = null
        private set(value) {
            if (field != null) {
                // Sending a dismiss callback to the viewModel creates an unexpected behavior.
                field?.setOnDismissListener(null)
                field?.dismiss()
            }
            field = value
        }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        dialog?.hide()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        dialog?.show()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroyView() {
        if (dialog != null) {
            dialog = null
        }
    }

    fun reset() {
        dialog = null
    }

    fun register(dialog: Dialog) {
        this.dialog = dialog
    }
}
