package org.nekobasu.resources

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.IntRange
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/**
 * ContextString is a char sequence wrapper that requires a Android context to resolve
 * Resources or accessing a Locale. It allows to evaluate the Strings as late as possible
 * and thus keeps the context out of unwanted areas, like a ViewModel.
 */
abstract class ContextString : CharSequence, LazyContextResource<CharSequence> {

    companion object {
        fun init(applicationContext : Context) {
            context = applicationContext.applicationContext
        }

        @SuppressLint("StaticFieldLeak")
        private var context: Context? = null
    }

    private val value: CharSequence by lazy {
        resolve(checkNotNull(context, { "ContextString is not initialized, did you forgot to call LazyString.init(Context)?" }))
    }

    override val length: Int
        get() = value.length

    override fun get(index: Int): Char = value[index]
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = value.subSequence(startIndex, endIndex)

    abstract override fun resolve(context: Context): CharSequence

    override fun toString(): String {
        return value.toString()
    }

    override fun equals(other: Any?): Boolean {
        return value == other?.toString()
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}

class FixedString(val string: CharSequence) : ContextString() {
    override fun resolve(context: Context): CharSequence {
        return string
    }
}

class ResString(@StringRes val resInt: Int) : ContextString() {
    override fun resolve(context: Context): CharSequence {
        return context.getString(resInt)
    }

    override fun equals(other: Any?): Boolean {
        return if (other is ResString) other.resInt == resInt else super.equals(other)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + resInt
        return result
    }
}

class WarningString(private val contextString: ContextString) : ContextString() {
    override fun resolve(context: Context): CharSequence = contextString.resolve(context)
}

class QuantityString(@PluralsRes val formatPattern: Int, @IntRange(from = 0) val quantity: Int, vararg val params: Any) : ContextString() {
    override fun resolve(context: Context): CharSequence {
        val resolved = params.map(resolveCharSequence(context))
        @Suppress("RemoveRedundantSpreadOperator")
        return context.resources.getQuantityString(formatPattern, quantity, *resolved.toTypedArray())
    }
}

class FunctionString(val contextToString: (Context) -> CharSequence) : ContextString() {
    override fun resolve(context: Context): CharSequence {
        return contextToString(context)
    }
}

class ConcatenatedString(vararg val elements: CharSequence, val separator: CharSequence = "") : ContextString() {
    override fun resolve(context: Context): CharSequence {
        return elements
                .map { if (it is ContextString) it.resolve(context).toString() else it.toString() }
                .reduce { acc, element -> acc + separator + element }
    }
}

class FormattedString(@StringRes val formatPattern: Int, vararg val params: Any) : ContextString() {
    override fun resolve(context: Context): CharSequence {
        val resolved = params.map(resolveCharSequence(context))
        @Suppress("RemoveRedundantSpreadOperator")
        return context.getString(formatPattern, *resolved.toTypedArray())
    }
}

fun lazyString(contextToString: (Context) -> String) = FunctionString(contextToString)
fun lazyString(vararg strings: CharSequence, separator: CharSequence = "") = ConcatenatedString(*strings, separator = separator)
fun lazyString(@StringRes stringRes: Int) = ResString(stringRes)
fun lazyString(@StringRes stringRes: Int, vararg params: Any) = FormattedString(stringRes, *params)

private fun resolveCharSequence(context: Context): (Any) -> Any =
        { (it as? ContextString)?.resolve(context) ?: it }
