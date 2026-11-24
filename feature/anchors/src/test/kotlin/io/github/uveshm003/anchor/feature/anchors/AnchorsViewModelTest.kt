package io.github.uveshm003.anchor.feature.anchors

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.uveshm003.anchor.core.engine.AnchorHasher
import io.github.uveshm003.anchor.core.engine.Input
import io.github.uveshm003.anchor.core.model.Anchor
import io.github.uveshm003.anchor.core.model.AnchorId
import io.github.uveshm003.anchor.core.model.AnchorType
import io.github.uveshm003.anchor.core.nfc.AnchorTags
import io.github.uveshm003.anchor.core.nfc.NfcAvailability
import io.github.uveshm003.anchor.core.nfc.WriteResult
import io.github.uveshm003.anchor.core.testing.MainDispatcherRule
import io.github.uveshm003.anchor.core.testing.data.FakeAnchorRepository
import io.github.uveshm003.anchor.core.testing.data.FakeSessionInputs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnchorsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val hasher = AnchorHasher { uid -> uid.joinToString("") { "%02x".format(it) }.padEnd(64, '0') }
    private val inputs = FakeSessionInputs()
    private val desk = Anchor(AnchorId(1), "Desk", AnchorType.Nfc, "01".padEnd(64, '0'))
    private var nfc = NfcAvailability.Enabled

    private fun CoroutineScope.setup(
        repo: FakeAnchorRepository = FakeAnchorRepository(listOf(desk)),
    ): Pair<AnchorsViewModel, AnchorTags> {
        val tags = AnchorTags(hasher, inputs, this)
        return AnchorsViewModel(repo, tags) { nfc } to tags
    }

    @Test
    fun pairing_a_new_sticker_asks_for_a_name_and_saves_it() = runTest {
        val repo = FakeAnchorRepository(listOf(desk))
        val (vm, tags) = backgroundScope.setup(repo)
        vm.uiState.test {
            while (awaitItem().loading) Unit
            vm.startPairing()
            assertThat(awaitItem().pairing).isEqualTo(Pairing.Waiting)
            assertThat(tags.pairing.value).isTrue()

            tags.onTag(byteArrayOf(0x02)) { WriteResult.Written }
            testScheduler.runCurrent()
            assertThat(
                expectMostRecentItem().pairing,
            ).isEqualTo(Pairing.Found("02".padEnd(64, '0'), WriteResult.Written))
            assertThat(tags.pairing.value).isFalse()

            vm.save("  ")
            vm.save(" Bedroom ")
            val saved = expectMostRecentItem()
            assertThat(saved.pairing).isEqualTo(Pairing.Idle)
            assertThat(saved.anchors.map { it.name }).containsExactly("Desk", "Bedroom")
        }
        assertThat(inputs.submitted).isEmpty()
    }

    @Test
    fun a_sticker_that_is_already_paired_says_so() = runTest {
        val (vm, tags) = backgroundScope.setup()
        vm.uiState.test {
            while (awaitItem().loading) Unit
            vm.startPairing()
            tags.onTag(byteArrayOf(0x01))
            testScheduler.runCurrent()
            assertThat(expectMostRecentItem().pairing).isEqualTo(Pairing.AlreadyPaired("Desk"))
            vm.dismissPairing()
            assertThat(awaitItem().pairing).isEqualTo(Pairing.Idle)
        }
    }

    @Test
    fun rename_unpair_and_nfc_state() = runTest {
        val repo = FakeAnchorRepository(listOf(desk))
        val (vm, tags) = backgroundScope.setup(repo)
        vm.uiState.test {
            while (awaitItem().loading) Unit
            vm.rename(desk, " ")
            vm.rename(desk, "Office")
            assertThat(awaitItem().anchors.single().name).isEqualTo("Office")
            vm.unpair(desk)
            assertThat(awaitItem().anchors).isEmpty()

            nfc = NfcAvailability.Disabled
            vm.refreshNfc()
            assertThat(awaitItem().nfc).isEqualTo(NfcAvailability.Disabled)
        }
        // Outside pairing, a tap goes to the engine.
        tags.onTag(byteArrayOf(0x01))
        testScheduler.runCurrent()
        assertThat(inputs.submitted).containsExactly(Input.AnchorTapped("01".padEnd(64, '0')))
    }
}
