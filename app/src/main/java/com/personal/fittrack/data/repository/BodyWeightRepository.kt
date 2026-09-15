package com.personal.fittrack.data.repository

import com.personal.fittrack.data.db.dao.BodyWeightDao
import com.personal.fittrack.data.db.entity.BodyWeightEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class BodyWeightRepository(private val dao: BodyWeightDao) {
    fun observeAll(): Flow<List<BodyWeightEntity>> = dao.observeAll()
    fun observeLatest(): Flow<BodyWeightEntity?> = dao.observeLatest()

    suspend fun logWeight(weightKg: Double, date: LocalDate = LocalDate.now()) {
        dao.insert(BodyWeightEntity(dateEpochDay = date.toEpochDay(), weightKg = weightKg))
    }

    suspend fun delete(id: Long) = dao.delete(id)
}
