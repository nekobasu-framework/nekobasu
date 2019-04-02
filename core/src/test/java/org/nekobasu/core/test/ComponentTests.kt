package org.nekobasu.core.test

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import org.nekobasu.core.*
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController

fun GenericScreenStackParam.startVisible(): ActivityController<out FragmentActivity> {
    val activityController = Robolectric.buildActivity<GenericSingleModuleActivity>(GenericSingleModuleActivity::class.java, SingleModuleActivity.addParamsToActivityIntent(Intent(), this))
    return activityController.setup()
}

class GenericSingleModuleActivity : SingleModuleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_AppCompat)
    }
}

class GenericScreenStackParam(val screenParams: List<Param>) : Param(moduleClass = GenericScreenStackModule::class.java)
class GenericScreenStackViewModel(private val params: GenericScreenStackParam) : ScreenStackViewModel() {
    override fun getInitialScreen(): ScreenUpdate = ScreenUpdate(params.screenParams[0])

    init {
        for (i in (1 until params.screenParams.size)) {
            pushUpdate(params.screenParams[i])
        }
    }
}

class GenericScreenStackModule(param: GenericScreenStackParam) : ScreenStackFragmentModule<GenericScreenStackViewModel, GenericScreenStackParam>(param) {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = LinearLayout(context)
        view.id = 1

        return view
    }

    override fun mainContainerId(): Int = 1

    override fun getViewModelClass(params: GenericScreenStackParam) = GenericScreenStackViewModel::class.java
}