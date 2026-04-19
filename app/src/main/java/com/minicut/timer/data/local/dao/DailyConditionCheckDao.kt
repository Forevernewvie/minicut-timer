package com.minicut.timer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.minicut.timer.data.local.entity.DailyConditionCheckEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyConditionCheckDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(check: DailyConditionCheckEntity)

    @Query(
        """
        SELECT * FROM daily_condition_checks
        WHERE dateEpochDay = :dateEpochDay
        LIMIT 1
        """,
    )
    fun observeForDate(dateEpochDay: Long): Flow<DailyConditionCheckEntity?>

    @Query(
        """
        SELECT * FROM daily_condition_checks
        WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay
        ORDER BY dateEpochDay
        """,
    )
    fun observeInRange(
        startEpochDay: Long,
        endEpochDay: Long,
    ): Flow<List<DailyConditionCheckEntity>>

    @Query("DELETE FROM daily_condition_checks")
    suspend fun deleteAll()
}
