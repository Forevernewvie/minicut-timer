package com.minicut.timer.testing

import com.minicut.timer.data.local.dao.CalorieEntryDao
import com.minicut.timer.data.local.dao.DailyConditionCheckDao
import com.minicut.timer.data.local.dao.MiniCutPlanDao
import com.minicut.timer.data.local.entity.CalorieEntryEntity
import com.minicut.timer.data.local.entity.DailyConditionCheckEntity
import com.minicut.timer.data.local.entity.MiniCutPlanEntity
import com.minicut.timer.data.local.query.DailyCalorieSummaryRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeMiniCutPlanDao : MiniCutPlanDao {
    val planFlow = MutableStateFlow<MiniCutPlanEntity?>(null)
    var lastUpsert: MiniCutPlanEntity? = null
    var deletePlanCalls: Int = 0

    override fun observePlan(): Flow<MiniCutPlanEntity?> = planFlow

    override suspend fun upsert(plan: MiniCutPlanEntity) {
        lastUpsert = plan
        planFlow.value = plan
    }

    override suspend fun deletePlan() {
        deletePlanCalls += 1
        planFlow.value = null
    }
}

class FakeCalorieEntryDao : CalorieEntryDao {
    private val entriesFlows = mutableMapOf<Long, MutableStateFlow<List<CalorieEntryEntity>>>()
    private val totalsFlows = mutableMapOf<Long, MutableStateFlow<Int>>()
    private val summariesFlow = MutableStateFlow<List<DailyCalorieSummaryRow>>(emptyList())
    private val allEntriesFlow = MutableStateFlow<List<CalorieEntryEntity>>(emptyList())

    var lastInserted: CalorieEntryEntity? = null
    var lastUpdated: CalorieEntryEntity? = null
    var lastDeletedId: Long? = null
    var lastFavoriteUpdate: Pair<Long, Boolean>? = null
    var deleteAllCalls: Int = 0

    override suspend fun insert(entry: CalorieEntryEntity) {
        lastInserted = entry
        upsertEntry(entry)
    }

    override suspend fun update(entry: CalorieEntryEntity) {
        lastUpdated = entry
        upsertEntry(entry)
    }

    override suspend fun deleteById(entryId: Long) {
        lastDeletedId = entryId
        entriesFlows.keys.forEach { date ->
            entriesFlows[date]?.value = entriesFlows[date].orEmpty().filterNot { it.id == entryId }
            totalsFlows[date]?.value = entriesFlows[date].orEmpty().sumOf { it.calories }
        }
        syncAllEntries()
    }

    override suspend fun updateFavorite(entryId: Long, isFavorite: Boolean) {
        lastFavoriteUpdate = entryId to isFavorite
        entriesFlows.keys.forEach { date ->
            entriesFlows[date]?.value =
                entriesFlows[date].orEmpty().map { entry ->
                    if (entry.id == entryId) entry.copy(isFavorite = isFavorite) else entry
                }
        }
        syncAllEntries()
    }

    override suspend fun deleteAll() {
        deleteAllCalls += 1
        entriesFlows.values.forEach { it.value = emptyList() }
        totalsFlows.values.forEach { it.value = 0 }
        summariesFlow.value = emptyList()
        allEntriesFlow.value = emptyList()
    }

    override fun observeEntriesForDate(dateEpochDay: Long): Flow<List<CalorieEntryEntity>> =
        entriesFlows.getOrPut(dateEpochDay) { MutableStateFlow(emptyList()) }

    override fun observeRecentEntries(limit: Int): Flow<List<CalorieEntryEntity>> =
        allEntriesFlow.map { entries -> entries.sortedByDescending { it.createdAtEpochMillis }.take(limit) }

    override fun observeFavoriteEntries(limit: Int): Flow<List<CalorieEntryEntity>> =
        allEntriesFlow.map { entries ->
            entries.filter { it.isFavorite }.sortedByDescending { it.createdAtEpochMillis }.take(limit)
        }

    override fun observeTotalForDate(dateEpochDay: Long): Flow<Int> =
        totalsFlows.getOrPut(dateEpochDay) { MutableStateFlow(0) }

    override fun observeDailySummaries(
        startEpochDay: Long,
        endEpochDay: Long,
    ): Flow<List<DailyCalorieSummaryRow>> =
        summariesFlow.map { rows ->
            rows.filter { it.dateEpochDay in startEpochDay..endEpochDay }
        }

    fun seedEntries(dateEpochDay: Long, entries: List<CalorieEntryEntity>) {
        entriesFlows.getOrPut(dateEpochDay) { MutableStateFlow(emptyList()) }.value = entries
        totalsFlows.getOrPut(dateEpochDay) { MutableStateFlow(0) }.value = entries.sumOf { it.calories }
        syncAllEntries()
    }

    fun seedTotal(dateEpochDay: Long, total: Int) {
        totalsFlows.getOrPut(dateEpochDay) { MutableStateFlow(0) }.value = total
    }

    fun seedDailySummaries(rows: List<DailyCalorieSummaryRow>) {
        summariesFlow.value = rows
    }

    private fun upsertEntry(entry: CalorieEntryEntity) {
        val flow = entriesFlows.getOrPut(entry.dateEpochDay) { MutableStateFlow(emptyList()) }
        flow.value =
            (flow.value.filterNot { it.id == entry.id } + entry)
                .sortedByDescending { it.createdAtEpochMillis }
        totalsFlows.getOrPut(entry.dateEpochDay) { MutableStateFlow(0) }.value = flow.value.sumOf { it.calories }
        syncAllEntries()
    }

    private fun syncAllEntries() {
        allEntriesFlow.value =
            entriesFlows.values
                .flatMap { it.value }
                .sortedByDescending { it.createdAtEpochMillis }
    }

    private fun <T> MutableStateFlow<List<T>>?.orEmpty(): List<T> = this?.value.orEmpty()
}

class FakeDailyConditionCheckDao : DailyConditionCheckDao {
    private val checksByDate = mutableMapOf<Long, MutableStateFlow<DailyConditionCheckEntity?>>()
    private val allChecksFlow = MutableStateFlow<List<DailyConditionCheckEntity>>(emptyList())
    var lastUpsert: DailyConditionCheckEntity? = null
    var deleteAllCalls: Int = 0

    override suspend fun upsert(check: DailyConditionCheckEntity) {
        lastUpsert = check
        checksByDate.getOrPut(check.dateEpochDay) { MutableStateFlow(null) }.value = check
        syncAllChecks()
    }

    override fun observeForDate(dateEpochDay: Long): Flow<DailyConditionCheckEntity?> =
        checksByDate.getOrPut(dateEpochDay) { MutableStateFlow(null) }

    override fun observeInRange(
        startEpochDay: Long,
        endEpochDay: Long,
    ): Flow<List<DailyConditionCheckEntity>> =
        allChecksFlow.map { checks ->
            checks.filter { it.dateEpochDay in startEpochDay..endEpochDay }
        }

    override suspend fun deleteAll() {
        deleteAllCalls += 1
        checksByDate.values.forEach { it.value = null }
        allChecksFlow.value = emptyList()
    }

    fun seedChecks(checks: List<DailyConditionCheckEntity>) {
        checksByDate.clear()
        checks.forEach { check ->
            checksByDate.getOrPut(check.dateEpochDay) { MutableStateFlow(null) }.value = check
        }
        syncAllChecks()
    }

    private fun syncAllChecks() {
        allChecksFlow.value =
            checksByDate.values
                .mapNotNull { it.value }
                .sortedBy { it.dateEpochDay }
    }
}
