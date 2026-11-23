package io.github.uveshm003.anchor.core.data

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import io.github.uveshm003.anchor.core.database.AnchorDatabase
import io.github.uveshm003.anchor.core.database.TransactionRunner

internal fun inMemoryDb(): AnchorDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AnchorDatabase::class.java)
        .allowMainThreadQueries()
        .build()

internal fun AnchorDatabase.transactions() = object : TransactionRunner {
    override suspend fun <R> invoke(block: suspend () -> R): R = withTransaction(block)
}
