package org.nekobasu.resources

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes

abstract class ContextIcon : LazyContextResource<Drawable>

class ResIcon(@DrawableRes private val drawableRes: Int) : ContextIcon() {
    override fun resolve(context: Context) = context.resources.getDrawable(drawableRes)
    override fun equals(other: Any?): Boolean = other is ResIcon && other.drawableRes == drawableRes
    override fun hashCode(): Int {
        return drawableRes
    }
}

fun lazyIcon(@DrawableRes drawableRes: Int) = ResIcon(drawableRes)
