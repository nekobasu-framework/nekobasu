package org.nekobasu.core

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment

abstract class Param(open val moduleClass: Class<out UiModule<*,*,*>>) : Parcelable

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
    val m_mainUiModule: UiModule<*, *, *> by lazy {
        val param = paramsFromFragment<Param>(this)
        param.instanceWithParams()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        savedInstanceState?.let { m_mainUiModule.onRestore(savedInstanceState) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        m_mainUiModule.attach(this.viewLifecycleOwner, this, context!!)
        return m_mainUiModule.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        m_mainUiModule.onInitView(view, savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (m_mainUiModule.isAttached) m_mainUiModule.onSave(outState)
    }

    override fun onBackPress(): Boolean {
        return m_mainUiModule.onBackPress()
    }

    override fun deliverResult(result: RequestedResult) : Boolean {
        return m_mainUiModule.deliverResult(result)
    }

    override fun updateParams(param: Parcelable) {
        addParamsToFragment(this, param as Param)
    }
}

