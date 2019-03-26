package org.nekobasu.core

import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.FragmentActivity

abstract class SingleModuleActivity<P : Param>(private val param: P) : FragmentActivity() {

    @Suppress("UNCHECKED_CAST")
    private val mainUiModule: LifecycleUiModule<*, *, P> by lazy {
        param.moduleClass.newInstance() as LifecycleUiModule<*, *, P>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainUiModule.attach(this, this)
        savedInstanceState?.let { mainUiModule.onRestore(savedInstanceState) }
        val view = mainUiModule.onCreateView(LayoutInflater.from(this), null, savedInstanceState)
        setContentView(view)
        if (view != null) {
            mainUiModule.onInitView(view, savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mainUiModule.onSave(outState)
    }

    override fun onBackPressed() {
        mainUiModule.onBackPress()
    }
}