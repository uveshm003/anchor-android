package io.github.uveshm003.anchor.core.nfc

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.uveshm003.anchor.core.engine.AnchorHasher
import io.github.uveshm003.anchor.core.engine.Input
import io.github.uveshm003.anchor.core.testing.data.FakeSessionInputs
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnchorTagsTest {
    private val inputs = FakeSessionInputs()
    private val hasher = AnchorHasher { uid -> uid.joinToString("") { "%02x".format(it) }.padEnd(64, '0') }

    @Test
    fun taps_outside_pairing_go_to_the_engine_as_hashes_and_never_write() = runTest {
        val tags = AnchorTags(hasher, inputs, backgroundScope)
        var wrote = false

        tags.onTag(byteArrayOf(0x04, 0x1A)) { WriteResult.Written.also { wrote = true } }
        testScheduler.runCurrent()

        assertThat(wrote).isFalse()
        assertThat(inputs.submitted).containsExactly(Input.AnchorTapped("041a".padEnd(64, '0')))
    }

    @Test
    fun taps_while_pairing_are_scanned_with_the_write_result() = runTest {
        val tags = AnchorTags(hasher, inputs, backgroundScope)
        tags.setPairing(true)
        assertThat(tags.pairing.value).isTrue()

        tags.scanned.test {
            tags.onTag(byteArrayOf(0x04)) { WriteResult.NotWritable }
            assertThat(awaitItem()).isEqualTo(ScannedTag("04".padEnd(64, '0'), WriteResult.NotWritable))
        }
        assertThat(inputs.submitted).isEmpty()
    }

    @Test
    fun the_ndef_message_has_the_uri_and_an_application_record() {
        val records = NdefAnchorRecord.message("io.github.uveshm003.anchor").records

        assertThat(records[0].toUri().toString()).isEqualTo("io.github.uveshm003.anchor://tap")
        assertThat(String(records[1].payload)).isEqualTo("io.github.uveshm003.anchor")
    }
}
