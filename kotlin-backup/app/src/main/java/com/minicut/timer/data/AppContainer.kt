package com.minicut.timer.data

import android.content.Context
import androidx.room.Room
import com.minicut.timer.data.local.MiniCutDatabase
import com.minicut.timer.data.repository.MiniCutRepository

class AppContainer(context: Context) {
    private val database =
        Room.databaseBuilder(
            context,
            MiniCutDatabase::class.java,
            "mini_cut_timer.db",
        ).addMigrations(
            MiniCutDatabase.MIGRATION_3_4,
            MiniCutDatabase.MIGRATION_4_5,
            MiniCutDatabase.MIGRATION_5_6,
            MiniCutDatabase.MIGRATION_6_7,
            MiniCutDatabase.MIGRATION_7_8,
            MiniCutDatabase.MIGRATION_8_9,
        )
            .build()

    val repository = MiniCutRepository(
        planDao = database.planDao(),
        calorieEntryDao = database.calorieEntryDao(),
        dailyConditionCheckDao = database.dailyConditionCheckDao(),
    )
}
