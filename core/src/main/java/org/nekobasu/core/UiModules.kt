package org.nekobasu.core

import androidx.fragment.app.Fragment

object UiModules {
    fun <P : Param> fragmentFromModule(param: P): Fragment {
        return SingleModuleFragment.newInstace(param)
    }
}