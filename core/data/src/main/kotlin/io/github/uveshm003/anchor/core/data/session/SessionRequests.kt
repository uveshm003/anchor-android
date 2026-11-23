package io.github.uveshm003.anchor.core.data.session

import io.github.uveshm003.anchor.core.engine.Input
import io.github.uveshm003.anchor.core.engine.Outcome

/** For the UI's own actions: submit an input and wait for its outcome (e.g. to explain a rejection). */
fun interface SessionRequests {
    suspend fun request(input: Input): Outcome
}
