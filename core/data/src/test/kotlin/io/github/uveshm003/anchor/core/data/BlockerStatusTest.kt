package io.github.uveshm003.anchor.core.data

import android.app.Application
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.uveshm003.anchor.core.data.platform.AndroidBlockerStatus
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
class BlockerStatusTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun follows_the_enabled_accessibility_services_setting() = runTest {
        val component = "${context.packageName}/io.github.uveshm003.anchor.service.blocker.BlockerService"
        AndroidBlockerStatus(context).enabled.test {
            assertThat(awaitItem()).isFalse()
            Settings.Secure.putString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                "other/.X:$component",
            )
            ShadowLooper.idleMainLooper()
            assertThat(awaitItem()).isTrue()
        }
    }
}
