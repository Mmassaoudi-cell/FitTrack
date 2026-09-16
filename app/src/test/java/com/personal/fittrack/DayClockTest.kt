package com.personal.fittrack

import com.personal.fittrack.domain.currentDay
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DayClockTest {
    @Test fun midnightAndZoneChangesRefreshToday() = runTest {
        var day = LocalDate.of(2026, 9, 15)
        var zone = ZoneId.of("America/Chicago")
        val emissions = mutableListOf<Pair<LocalDate, ZoneId>>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) { currentDay { day to zone }.take(3).toList(emissions) }
        advanceTimeBy(1001)
        assertEquals(1, emissions.size)
        day = day.plusDays(1)
        advanceTimeBy(1000)
        zone = ZoneId.of("Europe/Paris")
        advanceTimeBy(1000)
        job.join()
        assertEquals(3, emissions.size)
        assertEquals(day, emissions[1].first)
        assertEquals(zone, emissions[2].second)
    }
}
