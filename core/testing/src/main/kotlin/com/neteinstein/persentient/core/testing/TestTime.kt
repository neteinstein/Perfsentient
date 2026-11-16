package com.neteinstein.persentient.core.testing

import com.neteinstein.persentient.core.common.DispatcherProvider
import com.neteinstein.persentient.core.common.TimeProvider
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class FakeTimeProvider(
    var instant: Instant = LocalDateTime.of(2026, 11, 16, 21, 30).toInstant(ZoneOffset.UTC),
    private val zoneId: ZoneId = ZoneOffset.UTC,
) : TimeProvider {
    override fun now(): Instant = instant

    override fun zone(): ZoneId = zoneId
}

class TestDispatcherProvider(private val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()) : DispatcherProvider {
    override val main: CoroutineDispatcher get() = dispatcher
    override val io: CoroutineDispatcher get() = dispatcher
    override val default: CoroutineDispatcher get() = dispatcher
}

/** Replaces Dispatchers.Main for ViewModel tests. */
class MainDispatcherRule(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)

    override fun finished(description: Description) = Dispatchers.resetMain()
}
