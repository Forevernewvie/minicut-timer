package com.minicut.timer.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minicut.timer.domain.model.CalorieEntry
import com.minicut.timer.domain.model.CalorieAdjustmentDirection
import com.minicut.timer.domain.model.CalorieRangeStatus
import com.minicut.timer.domain.model.DailyConditionCheck
import com.minicut.timer.domain.model.EntryQuickPreset
import com.minicut.timer.domain.model.MiniCutGoalMode
import com.minicut.timer.domain.model.MiniCutPhase
import com.minicut.timer.domain.model.WeeklyCoachingSnapshot
import com.minicut.timer.domain.model.TodayMission
import com.minicut.timer.domain.model.MissionType
import com.minicut.timer.domain.model.WeeklyAdherenceReport
import com.minicut.timer.domain.rules.MiniCutRules
import com.minicut.timer.ui.components.MiniCutBackdrop
import com.minicut.timer.ui.components.MiniCutCardShape
import com.minicut.timer.ui.components.MiniCutMetricTile
import com.minicut.timer.ui.components.MiniCutPanelShape
import com.minicut.timer.ui.components.MiniCutPillShape
import com.minicut.timer.ui.components.MiniCutSectionHeader
import com.minicut.timer.ui.util.asCompactDate
import com.minicut.timer.ui.util.asDisplayDate
import com.minicut.timer.ui.util.asKcal
import com.minicut.timer.ui.util.asLabel
import com.minicut.timer.ui.util.miniCutRepository
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenPlan: (Int?) -> Unit,
) {
    val context = LocalContext.current
    val repository = context.miniCutRepository
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val today = uiState.currentDate
    var showEntrySheet by rememberSaveable { mutableStateOf(false) }
    var showCoachSheet by rememberSaveable { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<CalorieEntry?>(null) }
    var maintenanceChecks by rememberSaveable { mutableStateOf(setOf<Int>()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val weeklyReviewItemIndex =
        4 + if (uiState.planPhase == MiniCutPhase.Completed && uiState.plan != null) 1 else 0

    fun showMessage(message: String) {
        snackbarScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    MiniCutBackdrop {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                state = listState,
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 116.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    MiniCutSectionHeader(
                        kicker = today.asDisplayDate(),
                        title = "미니컷 대시보드",
                        subtitle = "오늘의 섭취와 다음 행동만 빠르게 확인하세요.",
                    )
                }
                item {
                    DailySummaryCard(
                        todayTotal = uiState.todayTotal,
                        target = uiState.todayTarget,
                        remainingCalories = uiState.remainingCalories,
                        overCalories = uiState.overCalories,
                        status = uiState.todayRangeStatus,
                        entryCount = uiState.todayEntries.size,
                        onAddMeal = { showEntrySheet = true },
                    )
                }
                item {
                    TodayMissionCard(
                        missions = uiState.todayMissions,
                        onMissionClick = { mission ->
                            when (mission.type) {
                                MissionType.FoodLog -> showEntrySheet = true
                                MissionType.CoachCheckIn -> showCoachSheet = true
                                MissionType.WeeklyReview ->
                                    snackbarScope.launch {
                                        listState.animateScrollToItem(weeklyReviewItemIndex)
                                    }
                            }
                        },
                    )
                }
                item {
                    val plan = uiState.plan
                    if (plan == null) {
                        EmptyPlanHero(onOpenPlan = { onOpenPlan(null) })
                    } else {
                        PlanOverviewCard(
                            startDate = plan.startDate.asCompactDate(),
                            endDate = plan.endDate.asCompactDate(),
                            durationWeeks = plan.durationWeeks,
                            dailyTargetKcal = plan.dailyTargetKcal,
                            goalMode = plan.goalMode,
                            phase = uiState.planPhase ?: MiniCutRules.phaseOf(plan.startDate, plan.endDate, today),
                            progress = uiState.planProgress?.progress ?: MiniCutRules.calculateProgress(plan.startDate, plan.endDate, today),
                            remainingDays = uiState.planProgress?.remainingDays ?: MiniCutRules.remainingDays(plan.startDate, plan.endDate, today),
                            dDayLabel = uiState.planProgress?.dDayLabel.orEmpty(),
                            progressHeadline = uiState.planProgress?.headline.orEmpty(),
                            progressSupporting = uiState.planProgress?.supportingText.orEmpty(),
                            daysUntilStart = ChronoUnit.DAYS.between(today, plan.startDate).toInt(),
                            onOpenPlan = { onOpenPlan(null) },
                        )
                    }
                }
                val completedPlan = uiState.plan
                if (uiState.planPhase == MiniCutPhase.Completed && completedPlan != null) {
                    item {
                        MaintenanceModeCard(
                            checkedSteps = maintenanceChecks,
                            dailyTargetKcal = completedPlan.dailyTargetKcal,
                            goalMode = completedPlan.goalMode,
                            onToggleStep = { step ->
                                maintenanceChecks =
                                    if (step in maintenanceChecks) {
                                        maintenanceChecks - step
                                    } else {
                                        maintenanceChecks + step
                                    }
                            },
                            onOpenPlan = { onOpenPlan(null) },
                        )
                    }
                }
                item {
                    WeeklyReportCard(
                        report = uiState.weeklyReport,
                        coachingSnapshot = uiState.weeklyCoachingSnapshot,
                        targetCalories = uiState.todayTarget,
                    )
                }
                item {
                    CoachSummaryCard(
                        recoveryMessage = uiState.recoveryRiskAssessment.message,
                        strengthMessage = uiState.strengthTrend.message,
                        dietBreakTitle = uiState.dietBreakRecommendation.title,
                        onOpenCoachSheet = { showCoachSheet = true },
                    )
                }
                if (uiState.favoritePresets.isNotEmpty() || uiState.recentPresets.isNotEmpty()) {
                    item {
                        QuickLogAssistCard(
                            favoritePresets = uiState.favoritePresets,
                            recentPresets = uiState.recentPresets,
                            onLogPreset = { preset ->
                                viewModel.addEntryFromPreset(preset)
                                showMessage("\"${preset.foodName}\" 프리셋으로 기록했어요")
                            },
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "오늘 기록",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "필요한 기록만 남기고 바로 수정하세요",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        FilledTonalButton(onClick = { showEntrySheet = true }) {
                            Text("추가")
                        }
                    }
                }
                if (uiState.todayEntries.isEmpty()) {
                    item {
                        EmptyMealCard(onAddMeal = { showEntrySheet = true })
                    }
                } else {
                    items(uiState.todayEntries, key = { it.id }) { entry ->
                        MealRowCard(
                            entry = entry,
                            onToggleFavorite = { viewModel.toggleFavorite(entry) },
                            onEdit = { editingEntry = entry },
                            onDelete = { viewModel.deleteEntry(entry.id) },
                        )
                    }
                }
            }
        }
    }

    if (showEntrySheet || editingEntry != null) {
        AddEntrySheet(
            initialCalories = editingEntry?.calories?.toString().orEmpty(),
            initialFoodName = editingEntry?.foodName.orEmpty(),
            initialNote = editingEntry?.note.orEmpty(),
            initialTimeLabel = editingEntry?.timeLabel.orEmpty(),
            title = if (editingEntry == null) "오늘 먹은 음식 기록" else "식사 기록 수정",
            saveLabel = if (editingEntry == null) "저장" else "수정 저장",
            onDismiss = {
                showEntrySheet = false
                editingEntry = null
            },
            onSave = { foodName, caloriesText, note, timeLabel ->
                val validation = validateMealEntryInput(foodName = foodName, caloriesText = caloriesText)
                if (!validation.isValid) {
                    showMessage(validation.firstErrorMessage ?: "입력값을 확인해주세요")
                    return@AddEntrySheet
                }

                val trimmedFoodName = foodName.trim()
                val calories = caloriesText.trim().toInt()
                val entry = editingEntry
                if (entry == null) {
                    viewModel.addEntry(calories, trimmedFoodName, note, timeLabel)
                } else {
                    viewModel.updateEntry(entry, calories, trimmedFoodName, note, timeLabel)
                }
                showEntrySheet = false
                editingEntry = null
                showMessage("식사 기록을 반영했어요")
            },
        )
    }

    if (showCoachSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCoachSheet = false },
            modifier = Modifier.navigationBarsPadding(),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    MiniCutSectionHeader(
                        kicker = "COACH CHECK-IN",
                        title = "코칭 체크인",
                        subtitle = "체중·단백질·저항운동부터 저장하고 선택 지표는 필요할 때만 더하세요.",
                    )
                }
                item {
                    BodyCompositionCheckCard(
                        todayCheck = uiState.todayConditionCheck,
                        weeklyWeightTrend = uiState.weeklyWeightTrend,
                        recoveryRiskAssessment = uiState.recoveryRiskAssessment,
                        recommendedProteinGrams = uiState.recommendedProteinGrams,
                        calorieAdjustmentRecommendation = uiState.calorieAdjustmentRecommendation,
                        onOpenPlan = { suggestedTargetKcal -> onOpenPlan(suggestedTargetKcal) },
                        onSave = { bodyWeightKg, proteinGrams, resistanceSets, mainLiftKg, relapseTrigger, copingAction, sleepHours, fatigueScore, hungerScore, moodScore, workoutPerformanceScore ->
                            viewModel.saveDailyConditionCheck(
                                bodyWeightKg = bodyWeightKg,
                                proteinGrams = proteinGrams,
                                resistanceSets = resistanceSets,
                                mainLiftKg = mainLiftKg,
                                relapseTrigger = relapseTrigger,
                                copingAction = copingAction,
                                sleepHours = sleepHours,
                                fatigueScore = fatigueScore,
                                hungerScore = hungerScore,
                                moodScore = moodScore,
                                workoutPerformanceScore = workoutPerformanceScore,
                            )
                            showMessage("코칭 체크인을 저장했어요")
                            showCoachSheet = false
                        },
                        onInvalidInput = ::showMessage,
                    )
                }
                item {
                    LeanMassProtectionCard(
                        score = uiState.leanMassProtectionScore,
                        strengthTrend = uiState.strengthTrend,
                        relapsePreventionInsight = uiState.relapsePreventionInsight,
                        dietBreakRecommendation = uiState.dietBreakRecommendation,
                        onOpenPlan = { onOpenPlan(null) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DailySummaryCard(
    todayTotal: Int,
    target: Int,
    remainingCalories: Int,
    overCalories: Int,
    status: CalorieRangeStatus,
    entryCount: Int,
    onAddMeal: () -> Unit,
) {
    val accent =
        when (status) {
            CalorieRangeStatus.NoData -> MaterialTheme.colorScheme.primary
            CalorieRangeStatus.Below -> MaterialTheme.colorScheme.tertiary
            CalorieRangeStatus.InRange -> MaterialTheme.colorScheme.primary
            CalorieRangeStatus.Above -> MaterialTheme.colorScheme.error
        }
    val detail =
        when {
            todayTotal <= 0 -> "첫 기록을 남기면 남은 칼로리가 바로 계산돼요."
            overCalories > 0 -> "${overCalories.asKcal()} 초과했어요"
            remainingCalories == 0 -> "목표를 정확히 맞췄어요"
            else -> "${remainingCalories.asKcal()} 남았어요"
        }
    val mainCalories = if (todayTotal == 0) "0 kcal" else todayTotal.asKcal()
    val statusLabel = status.asLabel()

    Card(
        shape = MiniCutCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = accent.copy(alpha = 0.12f),
                    contentColor = accent,
                    shape = MiniCutPillShape,
                ) {
                    Text(
                        text = statusLabel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = "${entryCount}건",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = mainCalories,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniCutMetricTile(
                    modifier = Modifier.weight(1f),
                    label = "목표",
                    value = target.asKcal(),
                    tint = MaterialTheme.colorScheme.primary,
                )
                MiniCutMetricTile(
                    modifier = Modifier.weight(1f),
                    label = if (overCalories > 0) "초과" else "남음",
                    value = (if (overCalories > 0) overCalories else remainingCalories).asKcal(),
                    tint = accent,
                )
            }
            Button(
                onClick = onAddMeal,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (entryCount == 0) "첫 음식 기록하기" else "음식 빠르게 추가")
            }
        }
    }
}

@Composable
private fun TodayMissionCard(
    missions: List<TodayMission>,
    onMissionClick: (TodayMission) -> Unit,
) {
    val completedCount = missions.count { it.isComplete }
    Card(
        shape = MiniCutCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("오늘의 미니컷 미션", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "오래 버티기보다 오늘 필요한 행동만 끝내세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    shape = MiniCutPillShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    Text(
                        "$completedCount/${missions.size}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            missions.forEach { mission ->
                MissionRow(
                    mission = mission,
                    onClick = { onMissionClick(mission) },
                )
            }
        }
    }
}

@Composable
private fun MissionRow(
    mission: TodayMission,
    onClick: () -> Unit,
) {
    val accent = if (mission.isComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MiniCutPanelShape,
        color = accent.copy(alpha = if (mission.isComplete) 0.10f else 0.06f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.18f)),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    if (mission.isComplete) "✓" else "○",
                    color = accent,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(mission.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        mission.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Surface(
                shape = MiniCutPillShape,
                color = accent.copy(alpha = 0.12f),
                contentColor = accent,
            ) {
                Text(
                    mission.actionLabel,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun EmptyPlanHero(onOpenPlan: () -> Unit) {
    Card(
        shape = MiniCutCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "짧고 선명하게 끝내는 미니컷",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "2~6주 플랜과 하루 목표 칼로리를 먼저 정하면 기록과 판단이 훨씬 쉬워져요.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onOpenPlan, modifier = Modifier.fillMaxWidth()) {
                Text("플랜 설정 시작")
            }
        }
    }
}

@Composable
private fun PlanOverviewCard(
    startDate: String,
    endDate: String,
    durationWeeks: Int,
    dailyTargetKcal: Int,
    goalMode: MiniCutGoalMode,
    phase: MiniCutPhase,
    progress: Float,
    remainingDays: Int,
    dDayLabel: String,
    progressHeadline: String,
    progressSupporting: String,
    daysUntilStart: Int,
    onOpenPlan: () -> Unit,
) {
    val statusText =
        when (phase) {
            MiniCutPhase.Upcoming -> "${daysUntilStart}일 후 시작"
            MiniCutPhase.Active -> "${durationWeeks}주 플랜 · ${remainingDays}일 남음"
            MiniCutPhase.Completed -> "설정한 미니컷 기간이 종료되었어요"
        }
    val progressSupportingText =
        when (phase) {
            MiniCutPhase.Upcoming -> "시작 전 준비 기간이에요"
            MiniCutPhase.Active -> "종료까지 ${remainingDays}일 남았어요"
            MiniCutPhase.Completed -> "유지 모드 체크리스트로 전환해보세요"
        }
    val progressValue by animateFloatAsState(targetValue = progress, label = "planProgress")

    Card(
        shape = MiniCutCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("미니컷 플랜", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
                shape = MiniCutPillShape,
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "$startDate ~ $endDate",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (progressHeadline.isNotBlank()) {
                        Text(
                            progressHeadline,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.tertiary,
                    shape = MiniCutPillShape,
                ) {
                    Text(
                        text = dDayLabel.ifBlank { "D-${remainingDays}" },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(
                "하루 목표 ${dailyTargetKcal.asKcal()}",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "플랜 목적 ${goalMode.displayName} · ${goalMode.shortDescription}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniCutMetricTile(
                    modifier = Modifier.weight(1f),
                    label = "시작",
                    value = startDate,
                )
                MiniCutMetricTile(
                    modifier = Modifier.weight(1f),
                    label = "종료",
                    value = endDate,
                )
                MiniCutMetricTile(
                    modifier = Modifier.weight(1f),
                    label = if (phase == MiniCutPhase.Upcoming) "시작까지" else "남은 날",
                    value = if (phase == MiniCutPhase.Upcoming) "${daysUntilStart}일" else "${remainingDays}일",
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { progressValue },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Text(
                    "진행률 ${(progressValue * 100).roundToInt()}%",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    progressSupporting.ifBlank { progressSupportingText },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedButton(onClick = onOpenPlan, modifier = Modifier.fillMaxWidth()) {
                Text("플랜 수정")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MaintenanceModeCard(
    checkedSteps: Set<Int>,
    dailyTargetKcal: Int,
    goalMode: MiniCutGoalMode,
    onToggleStep: (Int) -> Unit,
    onOpenPlan: () -> Unit,
) {
    val reverseDietPlan = MiniCutRules.reverseDietPlan(dailyTargetKcal = dailyTargetKcal, goalMode = goalMode)
    val checklist =
        listOf(
            "종료 후 첫 3~4일은 식사 리듬·수면을 먼저 안정화하세요.",
            "주 3회는 체중·컨디션을 짧게 체크해 급반등 신호를 빠르게 잡으세요.",
            "기록량을 줄여도 괜찮지만 하루 1회 기록은 유지해 요요 패턴을 방지하세요.",
        )

    Card(
        shape = MiniCutCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.24f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("플랜 종료 후 유지 모드", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                reverseDietPlan.summary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MiniCutPanelShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(reverseDietPlan.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    reverseDietPlan.steps.forEach { step ->
                        Text(
                            "• ${step.weekLabel}: ${step.targetCalories.asKcal()} · ${step.note}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        reverseDietPlan.caution,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            checklist.forEachIndexed { index, label ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MiniCutPanelShape,
                    color =
                        if (index in checkedSteps) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                    onClick = { onToggleStep(index) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(if (index in checkedSteps) "✓" else "○", color = MaterialTheme.colorScheme.primary)
                        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            OutlinedButton(onClick = onOpenPlan, modifier = Modifier.fillMaxWidth()) {
                Text("유지용 목표 다시 점검하기")
            }
        }
    }
}

@Composable
private fun WeeklyReportCard(
    report: WeeklyAdherenceReport,
    coachingSnapshot: WeeklyCoachingSnapshot,
    targetCalories: Int,
) {
    Card(
        shape = MiniCutCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("주간 복기", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "최근 7일 기준으로 기록 흐름과 목표 이내 일수를 정리했어요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniCutMetricTile(
                    modifier = Modifier.weight(1f),
                    label = "기록한 날",
                    value = "${report.loggedDays}/7일",
                )
                MiniCutMetricTile(
                    modifier = Modifier.weight(1f),
                    label = "목표 이내",
                    value = "${report.adherentDays}일",
                    tint = MaterialTheme.colorScheme.primary,
                )
                MiniCutMetricTile(
                    modifier = Modifier.weight(1f),
                    label = "평균",
                    value = if (report.averageLoggedCalories == 0) "0 kcal" else report.averageLoggedCalories.asKcal(),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }
            Text(
                "목표 ${targetCalories.asKcal()} 기준 · 초과 ${report.overTargetDays}일",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(report.focusMessage, style = MaterialTheme.typography.bodyMedium)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MiniCutPanelShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.50f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(coachingSnapshot.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(coachingSnapshot.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Text(coachingSnapshot.nextAction, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${coachingSnapshot.momentumLabel} · ${coachingSnapshot.momentumMessage}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickLogAssistCard(
    favoritePresets: List<EntryQuickPreset>,
    recentPresets: List<EntryQuickPreset>,
    onLogPreset: (EntryQuickPreset) -> Unit,
) {
    Card(
        shape = MiniCutCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("빠른 기록", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "반복되는 식사는 한 번 눌러 바로 추가하세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (favoritePresets.isEmpty() && recentPresets.isEmpty()) {
                Text(
                    "기록이 쌓이면 최근/즐겨찾기 버튼이 생겨요. 먼저 한두 끼를 기록해보세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                if (favoritePresets.isNotEmpty()) {
                    PresetSection(title = "즐겨찾기", presets = favoritePresets, onLogPreset = onLogPreset)
                }
                if (recentPresets.isNotEmpty()) {
                    PresetSection(title = "최근 기록", presets = recentPresets, onLogPreset = onLogPreset)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetSection(
    title: String,
    presets: List<EntryQuickPreset>,
    onLogPreset: (EntryQuickPreset) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            presets.forEach { preset ->
                OutlinedButton(
                    onClick = { onLogPreset(preset) },
                    modifier = Modifier.widthIn(max = 220.dp),
                ) {
                    Text(
                        text = "${preset.foodName} · ${preset.calories.asKcal()}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyMealCard(onAddMeal: () -> Unit) {
    Card(
        shape = MiniCutCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "오늘 기록이 아직 없어요",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "첫 식사부터 가볍게 남겨보세요. 음식명과 칼로리만 적어도 충분합니다.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(
                onClick = onAddMeal,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("첫 음식 기록하기")
            }
        }
    }
}

@Composable
private fun MealRowCard(
    entry: CalorieEntry,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        shape = MiniCutPanelShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), CircleShape)
                    .height(12.dp)
                    .width(12.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = entry.foodName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(entry.calories.asKcal(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Row {
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                imageVector = if (entry.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                                contentDescription = if (entry.isFavorite) "즐겨찾기 해제" else "즐겨찾기 추가",
                                tint = if (entry.isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                            )
                        }
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Rounded.Edit, contentDescription = "기록 수정")
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Rounded.Delete, contentDescription = "기록 삭제")
                        }
                    }
                }
                if (entry.timeLabel.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = MiniCutPillShape,
                    ) {
                        Text(
                            text = entry.timeLabel,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                if (entry.note.isNotBlank()) {
                    Text(
                        text = entry.note,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    entry.date.asDisplayDate(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddEntrySheet(
    initialCalories: String,
    initialFoodName: String,
    initialNote: String,
    initialTimeLabel: String,
    title: String,
    saveLabel: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit,
) {
    var caloriesText by rememberSaveable(initialCalories) { mutableStateOf(initialCalories) }
    var foodName by rememberSaveable(initialFoodName) { mutableStateOf(initialFoodName) }
    var note by rememberSaveable(initialNote) { mutableStateOf(initialNote) }
    var timeLabel by rememberSaveable(initialTimeLabel) { mutableStateOf(initialTimeLabel) }
    var foodNameError by rememberSaveable { mutableStateOf<String?>(null) }
    var caloriesError by rememberSaveable { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isImeVisible = WindowInsets.isImeVisible
    val foodFocusRequester = remember { FocusRequester() }
    val calorieFocusRequester = remember { FocusRequester() }
    val timeFocusRequester = remember { FocusRequester() }
    val noteFocusRequester = remember { FocusRequester() }

    ModalBottomSheet(
        onDismissRequest = {
            if (isImeVisible) {
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
            } else {
                onDismiss()
            }
        },
        shape = MiniCutCardShape,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                        },
                    )
                }
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            OutlinedTextField(
                value = foodName,
                onValueChange = {
                    foodName = it
                    if (foodNameError != null) foodNameError = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(foodFocusRequester),
                label = { Text("먹은 음식") },
                singleLine = true,
                isError = foodNameError != null,
                supportingText = {
                    Text(foodNameError ?: "예: 닭가슴살 샐러드")
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { calorieFocusRequester.requestFocus() },
                ),
            )
            OutlinedTextField(
                value = caloriesText,
                onValueChange = {
                    caloriesText = it.filter(Char::isDigit)
                    if (caloriesError != null) caloriesError = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(calorieFocusRequester),
                label = { Text("칼로리") },
                singleLine = true,
                isError = caloriesError != null,
                supportingText = {
                    Text(caloriesError ?: "숫자만 입력돼요")
                },
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                keyboardActions = KeyboardActions(
                    onNext = { timeFocusRequester.requestFocus() },
                ),
            )
            OutlinedTextField(
                value = timeLabel,
                onValueChange = { timeLabel = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(timeFocusRequester),
                label = { Text("식사 시간 (선택)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { noteFocusRequester.requestFocus() },
                ),
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(noteFocusRequester),
                label = { Text("추가 메모 (선택)") },
                minLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                    },
                ),
            )
            Button(
                onClick = {
                    val validation = validateMealEntryInput(foodName = foodName, caloriesText = caloriesText)
                    foodNameError = validation.foodNameError
                    caloriesError = validation.caloriesError
                    if (validation.isValid) {
                        onSave(foodName, caloriesText, note, timeLabel)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(saveLabel)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
