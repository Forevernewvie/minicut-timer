package com.minicut.timer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.minicut.timer.data.local.entity.CalorieEntryEntity
import com.minicut.timer.data.local.query.DailyCalorieSummaryRow
import kotlinx.coroutines.flow.Flow

@Dao
interface CalorieEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: CalorieEntryEntity)

    @Update
    suspend fun update(entry: CalorieEntryEntity)

    @Query("DELETE FROM calorie_entries WHERE id = :entryId")
    suspend fun deleteById(entryId: Long)

    @Query("UPDATE calorie_entries SET isFavorite = :isFavorite WHERE id = :entryId")
    suspend fun updateFavorite(entryId: Long, isFavorite: Boolean)

    @Query("DELETE FROM calorie_entries")
    suspend fun deleteAll()

    @Query(
        """
        SELECT * FROM calorie_entries
        WHERE dateEpochDay = :dateEpochDay
        ORDER BY createdAtEpochMillis DESC
        """,
    )
    fun observeEntriesForDate(dateEpochDay: Long): Flow<List<CalorieEntryEntity>>

    @Query(
        """
        SELECT * FROM calorie_entries
        ORDER BY createdAtEpochMillis DESC
        LIMIT :limit
        """,
    )
    fun observeRecentEntries(limit: Int): Flow<List<CalorieEntryEntity>>

    @Query(
        """
        SELECT * FROM calorie_entries
        WHERE isFavorite = 1
        ORDER BY createdAtEpochMillis DESC
        LIMIT :limit
        """,
    )
    fun observeFavoriteEntries(limit: Int): Flow<List<CalorieEntryEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(calories), 0)
        FROM calorie_entries
        WHERE dateEpochDay = :dateEpochDay
        """,
    )
    fun observeTotalForDate(dateEpochDay: Long): Flow<Int>

    @Query(
        """
        SELECT
            dateEpochDay,
            SUM(calories) AS totalCalories,
            COUNT(*) AS entryCount
        FROM calorie_entries
        WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay
        GROUP BY dateEpochDay
        ORDER BY dateEpochDay
        """,
    )
    fun observeDailySummaries(
        startEpochDay: Long,
        endEpochDay: Long,
    ): Flow<List<DailyCalorieSummaryRow>>
}
