package com.minicut.timer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.minicut.timer.data.local.entity.MiniCutPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MiniCutPlanDao {
    @Query("SELECT * FROM mini_cut_plan WHERE id = 1")
    fun observePlan(): Flow<MiniCutPlanEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(plan: MiniCutPlanEntity)

    @Query("DELETE FROM mini_cut_plan")
    suspend fun deletePlan()
}
