package io.github.uveshm003.anchor

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AnchorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DebugTools.install()
    }
}
