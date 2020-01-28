package org.nekobasu.core

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import org.junit.Test
import org.nekobasu.core.test.GenericScreenStackParam

import org.nekobasu.core.test.RobolectricTest
import org.nekobasu.core.test.startVisible
import org.robolectric.Robolectric

private val RES_ID_TEXT_VIEW = 3

class ScreenStackComponentTest : RobolectricTest() {

    class TestModuleParam(val name: CharSequence) : Param(TestModule::class.java)

    class TestViewModel(param: TestModuleParam) : SingleUpdateViewModel<CharSequence>() {
        override val initialViewUpdate: CharSequence = param.name
    }



    class TestModule(param: TestModuleParam) : UiModule<CharSequence, TestViewModel, TestModuleParam>(param) {
        private lateinit var view: View

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            context = inflater.context
            val parent = LinearLayout(context)
            parent.id = 2
            val textView = TextView(context)
            textView.id = RES_ID_TEXT_VIEW
            parent.addView(textView)

            return parent
        }

        override fun onInitView(view: View, savedInstanceState: Bundle?) {
            this.view = view
        }

        override lateinit var context: Context

        override fun onViewUpdate(viewUpdate: CharSequence) {
            view.findViewById<TextView>(3).text = viewUpdate
        }

        override fun getViewModelClass(params: TestModuleParam) = TestViewModel::class.java
    }

    @Test
    fun `test starting a fragment from params initializes and displays fragments ViewModel`() {
        val controller = GenericScreenStackParam(listOf(TestModuleParam("firstModule"))).startVisible()

        val fragment = controller.get().supportFragmentManager.fragments.first() as SingleModuleFragment

        assertThat(fragment.m_mainUiModule.viewModel, isA<TestViewModel>())
        assertThat(fragment.view!!.findViewById<TextView>(RES_ID_TEXT_VIEW).text.toString(), equalTo("firstModule"))
    }


    @Test
    fun `test starting multiple modules shows last pushed module`() {
        val controller = GenericScreenStackParam(
                screenParams = listOf(
                        TestModuleParam("firstModule"),
                        TestModuleParam("secondModule"),
                        TestModuleParam("thirdModule")
                )
        ).startVisible()

        val firstFragment = controller.get().supportFragmentManager.fragments[0]
        assertThat(firstFragment.view!!.findViewById<TextView>(RES_ID_TEXT_VIEW).text.toString(), equalTo("thirdModule"))
    }

    @Test
    fun `test poping third module will show second module`() {
        val controller = GenericScreenStackParam(
                screenParams = listOf(
                        TestModuleParam("firstModule"),
                        TestModuleParam("secondModule"),
                        TestModuleParam("thirdModule")
                )
        ).startVisible()

        val activity = controller.get()
        activity.onBackPressed()
        Robolectric.flushForegroundThreadScheduler()

        val firstFragment = activity.supportFragmentManager.fragments[0]
        assertThat(firstFragment.view!!.findViewById<TextView>(RES_ID_TEXT_VIEW).text.toString(), equalTo("secondModule"))
    }

    @Test
    fun `test popping all modules finishes activity`() {
        val controller = GenericScreenStackParam(
                screenParams = listOf(
                        TestModuleParam("firstModule"),
                        TestModuleParam("secondModule"),
                        TestModuleParam("thirdModule")
                )
        ).startVisible()

        val activity = controller.get()
        activity.onBackPressed()
        activity.onBackPressed()
        activity.onBackPressed()
        Robolectric.flushForegroundThreadScheduler()

        assertThat(activity.isFinishing, equalTo(true))
    }

    // TODO test tasks and delivery of values
}