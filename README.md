[![Release](https://img.shields.io/github/v/release/nekobasu-framework/nekobasu)](https://github.com/nekobasu-framework/nekobasu/releases)

# nekobasu
The nekobasu Android app framework for fast, stable, scalable and clean apps. This [article](https://proandroiddev.com/android-architecture-components-mvvm-part-1-1bd138959535) describes the core concept. 

## Install

Add jitpack to the `project/build.gradle` repositoreis:
```gradle
    repositories {
        maven {
            url "https://jitpack.io"
            name "jitpack.io"
        }
    }
```

Add dependency to the `project/app/build.gradle` dependencies:
```gradle
    implementation 'com.github.nekobasu-framework:nekobasu:LAST_VERSION'
```

## Example

Every feature is divided in a `ViewModel`, `UiModule` and `Params`. The ViewModel describes how a view should look like (`ViewUpdate`), the UiModule is rendering the `ViewUpdate` and the `Parmas` are describing which and how a `UiModule` should be presented. It uses androidx and the Android Architecture Components:  

MainActivity.kt
```kotlin
// The routing main module
class MainViewModel : ScreenStackViewModel() {
    override fun getInitialScreen() = ScreenUpdate(ExampleScreenParams())
}

// An example screen that is wrapped within a Fragment:
class ExampleModule : UiModule<ExampleViewUpdate, ExampleViewModel, ExampleScreenParams>(ExampleScreenParams()) {
    ...

    override fun onViewUpdate(viewUpdate: ExampleViewUpdate) {
        view.text.text = viewUpdate.currentCounter
    }
}

// The ViewModel that is constantly creating a view update
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
}
```
