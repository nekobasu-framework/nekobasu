package org.nekobasu.resources

import android.content.Context

interface LazyContextResource<T> {
    fun resolve(context: Context): T
}