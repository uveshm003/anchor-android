package io.github.uveshm003.anchor.core.database

import androidx.room.withTransaction
import javax.inject.Inject

/** Runs [block] in one database transaction, so callers outside this module don't need Room. */
interface TransactionRunner {
    suspend operator fun <R> invoke(block: suspend () -> R): R
}

internal class RoomTransactionRunner @Inject constructor(private val db: AnchorDatabase) : TransactionRunner {
    override suspend fun <R> invoke(block: suspend () -> R): R = db.withTransaction(block)
}
