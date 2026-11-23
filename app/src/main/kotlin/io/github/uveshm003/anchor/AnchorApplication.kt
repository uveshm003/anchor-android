package io.github.uveshm003.anchor

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.github.uveshm003.anchor.core.data.session.SessionController
import javax.inject.Inject

@HiltAndroidApp
class AnchorApplication : Application() {
    @Inject lateinit var sessionController: SessionController

    override fun onCreate() {
        super.onCreate()
        DebugTools.install()
        // Re-runs pending commands and reconciles (re-applies an active session) before anything else.
        sessionController.start()
    }
}
