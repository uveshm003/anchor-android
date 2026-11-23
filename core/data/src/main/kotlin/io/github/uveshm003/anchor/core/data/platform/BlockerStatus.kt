package io.github.uveshm003.anchor.core.data.platform

import android.content.ComponentName
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Whether Anchor's accessibility service (the blocker) is turned on in system settings. */
interface BlockerStatus {
    val enabled: Flow<Boolean>
}

internal class AndroidBlockerStatus @Inject constructor(@ApplicationContext private val context: Context) :
    BlockerStatus {
    private val component = ComponentName(context.packageName, BLOCKER_SERVICE_CLASS)

    override val enabled: Flow<Boolean> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(isEnabled())
            }
        }
        val uri = Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        context.contentResolver.registerContentObserver(uri, false, observer)
        trySend(isEnabled())
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }.distinctUntilChanged()

    private fun isEnabled(): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabled.orEmpty().split(':').any { ComponentName.unflattenFromString(it) == component }
    }

    private companion object {
        /** `service:blocker`'s service; named here because `core:data` must not depend on adapters. */
        const val BLOCKER_SERVICE_CLASS = "io.github.uveshm003.anchor.service.blocker.BlockerService"
    }
}
