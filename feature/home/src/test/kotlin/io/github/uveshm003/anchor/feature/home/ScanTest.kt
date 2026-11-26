package io.github.uveshm003.anchor.feature.home

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.uveshm003.anchor.core.engine.AnchorHasher
import io.github.uveshm003.anchor.core.engine.IgnoreReason
import io.github.uveshm003.anchor.core.engine.Input
import io.github.uveshm003.anchor.core.engine.Outcome
import io.github.uveshm003.anchor.core.engine.RejectReason
import io.github.uveshm003.anchor.core.model.ExitRule
import io.github.uveshm003.anchor.core.model.ModeId
import io.github.uveshm003.anchor.core.model.Session
import io.github.uveshm003.anchor.core.model.SessionId
import io.github.uveshm003.anchor.core.model.SessionSource
import io.github.uveshm003.anchor.core.qr.QrAnchorCode
import io.github.uveshm003.anchor.core.testing.MainDispatcherRule
import io.github.uveshm003.anchor.core.testing.data.FakeSessionRequests
import io.github.uveshm003.anchor.feature.home.scan.ScanState
import io.github.uveshm003.anchor.feature.home.scan.ScanViewModel
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScanTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val hasher = AnchorHasher { secret -> secret.joinToString("") { "%02x".format(it) } }
    private val requests = FakeSessionRequests()
    private val secret = ByteArray(32) { 7 }
    private val code = QrAnchorCode.encode(secret)
    private val session =
        Session(SessionId(1), ModeId(1), "Deep Work", Instant.EPOCH, SessionSource.AnchorTap, ExitRule.RequiresAnchor())

    private fun scan(outcome: Outcome): ScanState {
        requests.outcome = { outcome }
        val vm = ScanViewModel(hasher, requests)
        vm.onScanned(code)
        return vm.state.value
    }

    @Test
    fun a_scanned_qr_anchor_is_an_anchor_tap_with_the_same_hash() = runTest {
        assertThat(scan(Outcome.Ended(session))).isEqualTo(ScanState.Ended)
        assertThat(requests.requested.last()).isEqualTo(Input.AnchorTapped(hasher.hash(secret)))
        assertThat(scan(Outcome.Started(session))).isEqualTo(ScanState.Started("Deep Work"))
        assertThat(scan(Outcome.Rejected(RejectReason.RequiresAnchor("Desk")))).isEqualTo(ScanState.WrongAnchor("Desk"))
        assertThat(scan(Outcome.Ignored(IgnoreReason.UnknownAnchor))).isEqualTo(ScanState.Unknown)
        assertThat(scan(Outcome.Rejected(RejectReason.EndsWithTrigger))).isEqualTo(ScanState.Rejected)
    }

    @Test
    fun other_qr_codes_are_ignored_and_a_result_is_handled_once() = runTest {
        val vm = ScanViewModel(hasher, requests)
        vm.onScanned("https://example.com")
        assertThat(vm.state.value).isEqualTo(ScanState.NotAnAnchor)
        assertThat(requests.requested).isEmpty()

        requests.outcome = { Outcome.Ended(session) }
        vm.onScanned(code)
        vm.onScanned(code)
        assertThat(requests.requested).hasSize(1)

        vm.reset()
        assertThat(vm.state.value).isEqualTo(ScanState.Scanning)
    }
}
