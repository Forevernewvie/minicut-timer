package com.minicut.timer.ui.util

import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

fun millisUntilNextDay(now: ZonedDateTime = ZonedDateTime.now()): Long {
    val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
    return Duration.between(now, nextMidnight).toMillis() + 1_000L
}

fun currentDateTickerFlow(
    initialDate: LocalDate = LocalDate.now(),
    nowProvider: () -> ZonedDateTime = { ZonedDateTime.now() },
): Flow<LocalDate> =
    flow {
        emit(initialDate)
        while (currentCoroutineContext().isActive) {
            delay(millisUntilNextDay(nowProvider()))
            emit(nowProvider().toLocalDate())
        }
    }.distinctUntilChanged()
