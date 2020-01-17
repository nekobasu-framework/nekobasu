package org.nekobasu.example

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import kotlinx.android.synthetic.main.fragment_example.view.*
import org.nekobasu.core.*

class MainParams : Param(MainUiModule::class.java)

class MainViewModel : ScreenStackViewModel() {
    override fun getInitialScreen() = ScreenUpdate(ExampleScreenParams())
}

class MainUiModule : ScreenStackFragmentModule<MainViewModel, MainParams>(MainParams()) {
    override fun mainContainerId(): Int = R.id.main_container

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.main_layout, container, false)
    }

    override fun getViewModelClass(params: MainParams): Class<MainViewModel> = MainViewModel::class.java
}

class ExampleActivity : SingleModuleActivity() {
    override fun getInitialParam(): Param? {
        return MainParams()
    }
}
class ExampleScreenParams : Param(ExampleModule::class.java)

// TODO add lazy recource extension
data class ExampleViewUpdate(val currentCounter: CharSequence)

// TODO create LiveData ViewModel
class ExampleViewModel : SingleUpdateViewModel<ExampleViewUpdate>() {
    override val initialViewUpdate: ExampleViewUpdate = ExampleViewUpdate("Loading ...")

    val handler = Handler(Looper.getMainLooper())
    val thread = Thread {
        while (true) {
            try {
                Thread.sleep(1000)
                counter++

                handler.post {
                    setViewUpdate(ExampleViewUpdate("counter $counter"))
                }
            } catch (e: Throwable) {
            }
        }
    }.apply {
        start()
    }

    var counter: Int = 0

    override fun clearViewModel() {
        try {
            thread.interrupt()
        } catch (e: Throwable) {
        }
    }

    override fun onSave(outBundle: Bundle) {
        super.onSave(outBundle)
        outBundle.putDouble("counter", counter.toDouble())
    }


    override fun onRestore(inBundle: Bundle) {
        super.onRestore(inBundle)
        if (counter == 0) counter = inBundle.getDouble("counter", 0.0).toInt()
    }
}

class ExampleModule : LifecycleUiModule<ExampleViewUpdate, ExampleViewModel, ExampleScreenParams>(ExampleScreenParams()) {
    override lateinit var context: Context
    lateinit var view: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        context = inflater.context
        return inflater.inflate(R.layout.fragment_example, container, false)
    }

    override fun onInitView(view: View, savedInstanceState: Bundle?) {
        this.view = view
        // TODO add click event
    }

    override fun onViewUpdate(viewUpdate: ExampleViewUpdate) {
        view.text.text = viewUpdate.currentCounter
    }

    override fun getViewModelClass(params: ExampleScreenParams) = ExampleViewModel::class.java
}
