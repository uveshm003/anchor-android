package io.github.uveshm003.anchor.core.data

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.uveshm003.anchor.core.data.repository.AndroidInstalledAppsRepository
import io.github.uveshm003.anchor.core.data.repository.InstalledApp
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
class InstalledAppsRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val pm = shadowOf(context.packageManager)

    private fun install(pkg: String, label: String) {
        val app = ApplicationInfo().apply {
            packageName = pkg
            nonLocalizedLabel = label
        }
        val info = ResolveInfo().apply {
            activityInfo =
                ActivityInfo().apply {
                    packageName = pkg
                    name = "$pkg.Main"
                    applicationInfo = app
                    nonLocalizedLabel =
                        label
                }
            nonLocalizedLabel = label
        }
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.addResolveInfoForIntent(launcher, info)
        pm.addActivityIfNotPresent(ComponentName(pkg, "$pkg.Main"))
        pm.addIntentFilterForActivity(
            ComponentName(pkg, "$pkg.Main"),
            IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            },
        )
    }

    @Test
    fun lists_launchable_apps_sorted_by_label_without_anchor_and_refreshes_on_changes() = runTest {
        install("com.zebra", "Zebra")
        install("com.apple", "apple")
        install(context.packageName, "Anchor")
        val repo = AndroidInstalledAppsRepository(context, UnconfinedTestDispatcher(testScheduler))

        repo.apps.test {
            assertThat(
                awaitItem(),
            ).containsExactly(InstalledApp("com.apple", "apple"), InstalledApp("com.zebra", "Zebra")).inOrder()

            install("com.mango", "Mango")
            context.sendBroadcast(
                Intent(
                    Intent.ACTION_PACKAGE_ADDED,
                    android.net.Uri.parse("package:com.mango"),
                ).setPackage(context.packageName),
            )
            ShadowLooper.idleMainLooper()
            assertThat(awaitItem().map { it.label }).containsExactly("apple", "Mango", "Zebra").inOrder()
        }
    }
}
