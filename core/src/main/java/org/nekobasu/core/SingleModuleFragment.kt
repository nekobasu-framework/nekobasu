package org.nekobasu.core

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.parcel.Parcelize

@Parcelize
open class Param(val moduleClass: Class<out LifecycleUiModule<*,*,*>>) : Parcelable

class SingleModuleFragment<P : Param> : Fragment(), BackPressHandling, InterUiContract {
    companion object {
        private const val KEY_PARAMS = "fragment_params"

        fun <P : Param> paramsFromFragment(fragment: Fragment): P = fragment.arguments!!.getParcelable(KEY_PARAMS) as P
        fun <P : Param> addParamsToFragment(fragment: Fragment, params: P) {
            fragment.arguments = Bundle().apply {
                putParcelable(KEY_PARAMS, params)
            }
        }

        fun <P : Param> newInstace(param: P): SingleModuleFragment<P> {
            return SingleModuleFragment<P>().apply {
                addParamsToFragment(this, param)
            }
        }
    }

    private val mainUiModule: LifecycleUiModule<*, *, P> by lazy {
        val param = paramsFromFragment<P>(this)
        param.moduleClass.newInstance() as LifecycleUiModule<*, *, P>
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mainUiModule.attach(this.viewLifecycleOwner, this)
        savedInstanceState?.let { mainUiModule.onRestore(savedInstanceState) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return mainUiModule.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainUiModule.onInitView(view, savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mainUiModule.onSave(outState)
    }

    override fun onBackPress(): Boolean {
        return mainUiModule.onBackPress()
    }

    override fun deliverResult(result: RequestedResult) {
        mainUiModule.deliverResult(result)
    }

    override fun updateParams(param: Parcelable) {
        addParamsToFragment(this, param as P)
    }
}

