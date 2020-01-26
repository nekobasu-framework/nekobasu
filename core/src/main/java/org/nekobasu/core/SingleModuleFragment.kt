package org.nekobasu.core

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import kotlinx.android.parcel.Parcelize

abstract class Param(val moduleClass: Class<out LifecycleUiModule<*,*,*>>) : Parcelable

open class SingleModuleFragment : Fragment(), BackPressHandling, InterUiContract {
    companion object {
        private const val KEY_PARAMS = "fragment_params"

        fun <P : Param> paramsFromFragment(fragment: Fragment): P = fragment.arguments!!.getParcelable<P>(KEY_PARAMS) as P
        fun <P : Param> addParamsToFragment(fragment: Fragment, params: P) {
            fragment.arguments = Bundle().apply {
                putParcelable(KEY_PARAMS, params)
            }
        }

        fun <P : Param> newInstance(param: P): SingleModuleFragment {
            return SingleModuleFragment().apply {
                addParamsToFragment(this, param)
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val mainUiModule: LifecycleUiModule<*, *, *> by lazy {
        val param = paramsFromFragment<Param>(this)
        param.instanceWithParams()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        savedInstanceState?.let { mainUiModule.onRestore(savedInstanceState) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mainUiModule.attach(this.viewLifecycleOwner, this, context!!)
        return mainUiModule.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainUiModule.onInitView(view, savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mainUiModule.isAttached) mainUiModule.onSave(outState)
    }

    override fun onBackPress(): Boolean {
        return mainUiModule.onBackPress()
    }

    override fun deliverResult(result: RequestedResult) {
        mainUiModule.deliverResult(result)
    }

    override fun updateParams(param: Parcelable) {
        addParamsToFragment(this, param as Param)
    }
}

