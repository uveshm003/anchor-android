package io.github.uveshm003.anchor.core.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MainDispatcherRuleTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun mainDispatcher_isTheTestDispatcher() = runTest(mainDispatcherRule.testDispatcher) {
        var ran = false
        launch(Dispatchers.Main) { ran = true }

        assertFalse("StandardTestDispatcher queues work until the scheduler runs", ran)
        testScheduler.advanceUntilIdle()
        assertTrue(ran)
    }
}
