package org.nekobasu.core

import java.lang.IllegalStateException

fun Param.instanceWithParams() : LifecycleUiModule<*,*,*> {
    val primaryConstructor = this.moduleClass.constructors.first()
    return when {
        primaryConstructor.parameterTypes.isEmpty() -> primaryConstructor.newInstance()
        Param::class.java.isAssignableFrom(primaryConstructor.parameterTypes[0]) -> primaryConstructor.newInstance(this)
        else -> throw IllegalStateException("Can not create module, need an empty primary constructor or a single argument Param constructor.")
    } as LifecycleUiModule<*, *, *>
}