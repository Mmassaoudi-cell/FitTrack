package com.personal.fittrack.domain

import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

/** Rechecks the date and zone on collection and while the app stays open. */
fun currentDay(now: () -> Pair<LocalDate, ZoneId> = { LocalDate.now() to ZoneId.systemDefault() }) = flow {
    while (true) {
        emit(now())
        delay(1_000)
    }
}.distinctUntilChanged()
