package io.github.uveshm003.anchor.core.triggers.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import io.github.uveshm003.anchor.core.common.dispatchers.ApplicationScope
import io.github.uveshm003.anchor.core.engine.Input
import io.github.uveshm003.anchor.core.engine.SessionInputs
import io.github.uveshm003.anchor.core.model.SessionId
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Forwards the alarm to the engine; no logic here (the engine ignores a timer for another session). */
@AndroidEntryPoint
class TimerReceiver : BroadcastReceiver() {
    @Inject lateinit var inputs: SessionInputs

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1)
        if (sessionId < 0) return
        val pending = goAsync()
        scope.launch {
            try {
                inputs.submit(Input.TimerElapsed(SessionId(sessionId)))
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_SESSION_ID = "session_id"
    }
}
