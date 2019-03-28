package org.nekobasu.core.test

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.widget.LinearLayout
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.Resetter
import org.robolectric.shadows.ShadowLog
import java.lang.reflect.InvocationTargetException
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.declaredFunctions

@RunWith(RobolectricTestRunner::class)
@Config(
        sdk = [25],
        shadows = []
)
abstract class RobolectricTest {
    val appContext: Context = ApplicationProvider.getApplicationContext()


    fun disableAnimations() {
        val contentResolver = ApplicationProvider.getApplicationContext<Application>().contentResolver
        Settings.Global.putInt(contentResolver, Settings.Global.WINDOW_ANIMATION_SCALE, 0)
        Settings.Global.putInt(contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 0)
        Settings.Global.putInt(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0)
    }

    @Before
    @CallSuper
    open fun setup() {
        disableAnimations()
        ShadowLog.stream = System.out
        updateLocale(getLocale())
    }

    @After
    @CallSuper
    open fun teardown() {
        resetShadows()
    }

    private fun resetShadows() {
        val classConfig = this::class.java.getAnnotation(Config::class.java) as Config
        val shadows: Array<KClass<*>> = classConfig.shadows
        resetAllShadows(shadows)
    }

    /**
     * Override this to change the locale used inside the test.
     * Default locale is en-GB.
     */
    open fun getLocale(): Locale {
        return Locale("en", "GB")
    }

    @Suppress("DEPRECATION")
    private fun updateLocale(locale: Locale) {
        Locale.setDefault(locale)
        appContext.resources.configuration.setLocale(locale)
        appContext.resources.updateConfiguration(appContext.resources.configuration, appContext.resources.displayMetrics)
    }


    private fun resetAllShadows(shadows: Array<KClass<*>>) {
        for (shadow in shadows) {
            var isReset = false
            for (shadowMethod in shadow.declaredFunctions) {
                if (shadowMethod.annotations.find { it is Resetter } != null) {
                    try {
                        shadowMethod.call()
                        isReset = true
                    } catch (e: IllegalAccessException) {
                        throw RuntimeException("Can not reset mock: $shadow", e)
                    } catch (e: InvocationTargetException) {
                        throw RuntimeException("Can not reset mock: $shadow", e)
                    }

                }
            }
            if (!isReset) {
                throw IllegalStateException("$shadow does not have a \"@Resetter public static void reset()\" method." +
                        " Need to provide one to reset the inner state")
            }
        }
    }
}

