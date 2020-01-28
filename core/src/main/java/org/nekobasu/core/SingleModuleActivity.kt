package org.nekobasu.core

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.annotation.CallSuper
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.FragmentActivity
import java.lang.IllegalStateException

open class SingleModuleActivity : FragmentActivity() {

    companion object {
        private const val KEY_PARAMS = "activity_params"

        fun <P : Param> paramsFromActivity(activity: FragmentActivity): P? = activity.intent.getParcelableExtra(KEY_PARAMS) as? P
        fun <P : Param> addParamsToActivityIntent(intent: Intent, params: P): Intent {
            intent.putExtra(KEY_PARAMS, params)
            return intent
        }

    }

    /**
     * Override to be able to declare activities within the AndroidManifest, instead of using
     * SingleModuleActivity.addParamsToActivityIntent(intent, param).
     */
    protected open fun getInitialParam(): Param? {
        return null
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val m_mainUiModule: UiModule<*, *, *> by lazy {
        val intentParam = paramsFromActivity<Param>(this)
        val initialParam = getInitialParam()

        when {
            intentParam != null -> intentParam
            initialParam != null -> initialParam
            else -> throw IllegalStateException("Initial and intent param is not set for SingleModuleActivity, " +
                    "please override getInitialParam or start Activity with SingleModuleActivity.addParamsToActivityIntent")
        }.instanceWithParams()
    }

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        m_mainUiModule.attach(this, this, this)
        savedInstanceState?.let { m_mainUiModule.onRestore(savedInstanceState) }
        val view = m_mainUiModule.onCreateView(LayoutInflater.from(this), null, savedInstanceState)
        setContentView(view)
        if (view != null) {
            m_mainUiModule.onInitView(view, savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        m_mainUiModule.onSave(outState)
    }

    override fun onBackPressed() {
        m_mainUiModule.onBackPress()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        m_mainUiModule.deliverResult(RequestedResult(requestCode, OnActivityResult(requestCode, resultCode, data)))
    }
}
