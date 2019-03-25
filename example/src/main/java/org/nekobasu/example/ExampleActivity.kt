package org.nekobasu.example

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import kotlinx.android.synthetic.main.fragment_example.view.*
import org.nekobasu.core.*

class ExampleActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)

        if (supportFragmentManager.findFragmentByTag("test")  == null) {
            supportFragmentManager
                    .beginTransaction()
                    .addToBackStack(null)
                    .replace(R.id.main_container, UiModules.fragmentFromModule(ExampleParams()), "test")
                    .commit()
        }
    }
}

class ExampleParams : Param(ExampleModule::class.java)

// TODO add lazy recource extension
data class ExampleViewUpdate(val currentCounter : CharSequence)

// TODO create LiveData ViewModel
class ExampleViewModel(params: ExampleParams) : SingleUpdateViewModel<ExampleViewUpdate>() {
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
            } catch (e : Throwable) {}
        }
    }.apply {
        start()
    }

    var counter : Int = 0

    override fun clear() {
        try {
            thread.interrupt()
        } catch (e : Throwable) {}
    }
}

class ExampleModule : LifecycleUiModule<ExampleViewUpdate, ExampleViewModel, ExampleParams>(ExampleParams()) {
    lateinit var view : View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_example, container, false)
    }

    override fun onInitView(view: View, savedInstanceState: Bundle?) {
        this.view = view
       // TODO add click event
    }

    override fun onViewUpdate(viewUpdate: ExampleViewUpdate) {
        view.text.text = viewUpdate.currentCounter
    }

    override fun getViewModelClass(params: ExampleParams) = ExampleViewModel::class.java
}
