package io.github.uveshm003.anchor.core.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.uveshm003.anchor.core.common.dispatchers.AnchorDispatchers
import io.github.uveshm003.anchor.core.common.dispatchers.Dispatcher
import java.text.Collator
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/** A launchable app (label + package). Icons are loaded by the UI (`core:ui` `AppIcon`). */
data class InstalledApp(val packageName: String, val label: String)

interface InstalledAppsRepository {
    /** Launchable apps except Anchor, sorted by label for the current locale; refreshes on install/uninstall. */
    val apps: Flow<List<InstalledApp>>
}

internal class AndroidInstalledAppsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(AnchorDispatchers.IO) io: CoroutineDispatcher,
) : InstalledAppsRepository {
    override val apps: Flow<List<InstalledApp>> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(load())
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        trySend(load())
        awaitClose { context.unregisterReceiver(receiver) }
    }.flowOn(io)

    private fun load(): List<InstalledApp> {
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val collator = Collator.getInstance(Locale.getDefault())
        return pm.queryIntentActivities(launcher, 0)
            .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedWith { a, b -> collator.compare(a.label, b.label) }
    }
}
