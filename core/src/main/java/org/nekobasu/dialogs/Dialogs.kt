package org.nekobasu.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import org.nekobasu.core.*
import java.lang.IllegalStateException

data class DialogInteraction(val title: CharSequence, val interactionId: InteractionId)
// TODO add lazy resources
fun okInteraction() = DialogInteraction("OK", InteractionIds.POSITIVE)
fun cancelInteraction() = DialogInteraction("Cancel", InteractionIds.NEGATIVE)

object InteractionIds {
    val POSITIVE = InteractionId(1)
    val NEGATIVE = InteractionId(2)
    val NEUTRAL = InteractionId(3)
}

abstract class SelfCreatedDialogUpdate(override val dialogId: DialogId, val dialogCreatorClass: Class<out DialogCreator>) : DialogUpdate(dialogId)

object Dialogs {

    data class AlertDialog(override val dialogId: DialogId = DialogId.nextId(),
                           val title: CharSequence,
                           val message: CharSequence,
                           val positive: DialogInteraction? = null,
                           val negative: DialogInteraction? = null,
                           val neutral: DialogInteraction? = null,
                           val isCancelable: Boolean = true) : SelfCreatedDialogUpdate(dialogId, AlertDialogCreator::class.java)
}

open class CommonDialogCreator : DialogCreator {

    private val dialogCreatorCache = mutableMapOf<Class<*>, DialogCreator>()

    override fun createDialog(context: Context, dialogUpdate: DialogUpdateContract, savedInstanceState: Bundle?, callback: DialogViewCallback): Dialog {
        val creator = when (dialogUpdate) {
            is SelfCreatedDialogUpdate -> {
                val creator = dialogCreatorCache[dialogUpdate.dialogCreatorClass]
                if (creator == null) {
                    val newCreator = dialogUpdate.dialogCreatorClass.newInstance()
                    dialogCreatorCache[dialogUpdate.dialogCreatorClass] = newCreator
                    newCreator
                } else creator
            }
            else -> throw IllegalStateException("Can no create dialog for $dialogUpdate !")
        }
        return creator.createDialog(context, dialogUpdate, savedInstanceState, callback)
    }
}

class AlertDialogCreator : DialogCreator {
    override fun createDialog(context: Context, dialogUpdate: DialogUpdateContract, savedInstanceState: Bundle?, callback: DialogViewCallback): Dialog {
        val update = dialogUpdate as Dialogs.AlertDialog
        val dialog = AlertDialog.Builder(context).apply {
            setTitle(update.title)
            setMessage(update.message)
            update.positive?.let {
                setPositiveButton(it.title) { _, _ ->
                    callback.onInteractionPerformed(update.dialogId, it.interactionId)
                }
            }
            update.neutral?.let {
                setNeutralButton(it.title) { _, _ ->
                    callback.onInteractionPerformed(update.dialogId, it.interactionId)
                }
            }
            update.negative?.let {
                setNegativeButton(it.title) { _, _ ->
                    callback.onInteractionPerformed(update.dialogId, it.interactionId)
                }
            }
            setOnCancelListener { callback.onCanceledByOutside(update.dialogId) }
            setCancelable(update.isCancelable)
        }.create()

        return savedInstanceState?.let {
            dialog.onRestoreInstanceState(it)
            dialog
        } ?: dialog
    }
}