package org.nekobasu.core

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.natpryce.hamkrest.*
import com.natpryce.hamkrest.assertion.assertThat
import org.junit.Test
import org.nekobasu.core.test.GenericScreenStackParam

import org.nekobasu.core.test.RobolectricTest
import org.nekobasu.core.test.startVisible
import org.nekobasu.dialogs.Dialogs
import org.nekobasu.dialogs.okInteraction

private const val RES_ID_TEXT_VIEW = 3

class CompositeUiModuleComponentTest : RobolectricTest() {

    class TestModuleParam(val name: CharSequence) : Param(TestModule::class.java)

    class TestViewModel(param: TestModuleParam, dialogChannel: DialogChannel) : SingleUpdateViewModel<CharSequence>() {
        override val initialViewUpdate: CharSequence = param.name

        val dialogCallback =  dialogChannel.showDialog(Dialogs.AlertDialog(
                title = "Alert",
                message = "A nice dialog",
                positive = okInteraction()),
                DialogCallbacks.onAnyAction {

                })
    }

    class TestModule(param: TestModuleParam) : CompositeUiModule<CharSequence, TestViewModel, TestModuleParam>(param) {

        val dialogModule = DialogModule(DialogParam())

        val m_supportModules: Set<UiModule<*, *, *>> = setOf(
                dialogModule
        )

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?, moduleViews: Map<UiModule<*, *, *>, View?>): View? {
            context = inflater.context
            val parent = LinearLayout(context)
            parent.id = 2
            val textView = TextView(context)
            textView.id = RES_ID_TEXT_VIEW
            parent.addView(textView)

            return parent
        }

        private lateinit var view: View

        override fun onInitView(view: View, savedInstanceState: Bundle?) {
            this.view = view
        }

        override lateinit var context: Context

        override fun onViewUpdate(viewUpdate: CharSequence) {
            view.findViewById<TextView>(3).text = viewUpdate
        }

        override fun getViewModelClass(params: TestModuleParam) = TestViewModel::class.java

        override fun onCreateViewModel(params: TestModuleParam): TestViewModel {
            return TestViewModel(param, dialogModule.viewModel)
        }
    }

    @Test
    fun `test starting the composite module with a DialogModule shows a dialog`() {
        val controller = GenericScreenStackParam(listOf(TestModuleParam("firstModule"))).startVisible()

        val fragment = controller.get().supportFragmentManager.fragments.first() as SingleModuleFragment

        val dialogModule = (fragment.m_mainUiModule as TestModule).dialogModule
        assertThat(dialogModule.dialogHandler.dialog, present())
    }

    @Test
    fun `test canceling a visible dialog removes the dialog`() {
        val controller = GenericScreenStackParam(listOf(TestModuleParam("firstModule"))).startVisible()
        val fragment = controller.get().supportFragmentManager.fragments.first() as SingleModuleFragment
        val module = (fragment.m_mainUiModule as TestModule)

        module.viewModel.dialogCallback.cancel()

        assertThat(module.dialogModule.dialogHandler.dialog, absent())
    }
}