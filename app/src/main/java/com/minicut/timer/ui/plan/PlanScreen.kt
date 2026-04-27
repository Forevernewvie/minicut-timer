package com.minicut.timer.ui.plan

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minicut.timer.data.local.NotificationPreferences
import com.minicut.timer.domain.model.ActivityLevel
import com.minicut.timer.domain.model.DeficitGuardrail
import com.minicut.timer.domain.model.DeficitRiskLevel
import com.minicut.timer.domain.model.MiniCutGoalMode
import com.minicut.timer.domain.model.TargetGuidance
import com.minicut.timer.domain.model.TargetGuidanceTone
import com.minicut.timer.domain.rules.MiniCutRules
import com.minicut.timer.ui.components.MiniCutBackdrop
import com.minicut.timer.ui.components.MiniCutBottomActionBar
import com.minicut.timer.ui.components.MiniCutCardShape
import com.minicut.timer.ui.components.MiniCutInlineFeedback
import com.minicut.timer.ui.components.MiniCutInlineFeedbackTone
import com.minicut.timer.ui.components.MiniCutPanelShape
import com.minicut.timer.ui.components.MiniCutPillShape
import com.minicut.timer.ui.components.MiniCutScreenHorizontalPadding
import com.minicut.timer.ui.components.MiniCutSectionHeader
import com.minicut.timer.ui.home.NotificationSettingsCard
import com.minicut.timer.notifications.NotificationSettings
import com.minicut.timer.notifications.ReminderSlot
import com.minicut.timer.notifications.ReminderTime
import com.minicut.timer.notifications.syncMiniCutNotifications
import com.minicut.timer.ui.util.asCompactDate
import com.minicut.timer.ui.util.asKcal
import com.minicut.timer.ui.util.miniCutRepository
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlanScreen(
    onSaved: () -> Unit,
    suggestedTargetKcal: Int? = null,
    isPrivacyOptionsRequired: Boolean,
    onPrivacyOptionsClick: () -> Unit,
) {
    val context = LocalContext.current
    val repository = context.miniCutRepository
    val viewModel: PlanViewModel = viewModel(factory = PlanViewModel.factory(repository))
    val existingPlan by viewModel.plan.collectAsStateWithLifecycle()
    var notificationSettings by remember { mutableStateOf(NotificationPreferences.load(context)) }
    val notificationPermissionGranted =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    var startDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var durationWeeks by rememberSaveable { mutableIntStateOf(4) }
    var dailyTargetKcal by rememberSaveable { mutableIntStateOf(MiniCutRules.DEFAULT_TARGET_KCAL) }
    var goalMode by rememberSaveable { mutableStateOf(MiniCutGoalMode.MassReset) }
    var activityLevel by rememberSaveable { mutableStateOf(ActivityLevel.Moderate) }
    var bodyWeightText by rememberSaveable { mutableStateOf("") }
    var acknowledgeNotStrengthPeak by rememberSaveable { mutableStateOf(false) }
    var acknowledgeNotLongTermDiet by rememberSaveable { mutableStateOf(false) }
    var acknowledgeNotMassGainAvoidance by rememberSaveable { mutableStateOf(false) }
    var showDataResetDialog by rememberSaveable { mutableStateOf(false) }
    var inlineFeedbackMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var inlineFeedbackTone by rememberSaveable { mutableStateOf(MiniCutInlineFeedbackTone.Info) }
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                syncMiniCutNotifications(context, notificationSettings)
                inlineFeedbackTone = MiniCutInlineFeedbackTone.Info
                inlineFeedbackMessage = "알림 권한을 허용했어요. 저장된 리마인더를 다시 동기화합니다."
            } else {
                inlineFeedbackTone = MiniCutInlineFeedbackTone.Caution
                inlineFeedbackMessage = "알림 권한이 없으면 리마인더가 표시되지 않을 수 있어요."
            }
        }

    LaunchedEffect(existingPlan) {
        existingPlan?.let {
            startDate = it.startDate
            durationWeeks = it.durationWeeks
            dailyTargetKcal = it.dailyTargetKcal
            goalMode = it.goalMode
            activityLevel = it.activityLevel
            bodyWeightText =
                if (it.estimatedMaintenanceKcal > 0) {
                    val estimatedWeight = it.estimatedMaintenanceKcal / it.activityLevel.kcalPerKgFactor
                    String.format(java.util.Locale.US, "%.1f", estimatedWeight)
                } else {
                    ""
                }
            acknowledgeNotStrengthPeak = true
            acknowledgeNotLongTermDiet = true
            acknowledgeNotMassGainAvoidance = true
        } ?: run {
            startDate = LocalDate.now()
            durationWeeks = 4
            dailyTargetKcal = MiniCutRules.DEFAULT_TARGET_KCAL
            goalMode = MiniCutGoalMode.MassReset
            activityLevel = ActivityLevel.Moderate
            bodyWeightText = ""
            acknowledgeNotStrengthPeak = false
            acknowledgeNotLongTermDiet = false
            acknowledgeNotMassGainAvoidance = false
        }
    }

    LaunchedEffect(suggestedTargetKcal) {
        val suggestion = suggestedTargetKcal?.takeIf { it in MiniCutRules.TARGET_OPTIONS_KCAL } ?: return@LaunchedEffect
        if (dailyTargetKcal != suggestion) {
            dailyTargetKcal = suggestion
            inlineFeedbackTone = MiniCutInlineFeedbackTone.Info
            inlineFeedbackMessage = "홈에서 제안한 ${suggestion.asKcal()}을 적용했어요. 저장하면 플랜에 반영됩니다."
        }
    }

    val endDate = remember(startDate, durationWeeks) { MiniCutRules.calculateEndDate(startDate, durationWeeks) }
    val targetGuidance = remember(dailyTargetKcal, durationWeeks) {
        MiniCutRules.targetGuidance(
            targetCalories = dailyTargetKcal,
            durationWeeks = durationWeeks,
        )
    }
    val dialogButtonColor = MaterialTheme.colorScheme.primary.toArgb()
    val hasExistingPlan = existingPlan != null
    val bodyWeightKg = bodyWeightText.toFloatOrNull()?.takeIf { it > 0f }
    val estimatedMaintenanceKcal =
        MiniCutRules.estimateMaintenanceCalories(
            bodyWeightKg = bodyWeightKg,
            activityLevel = activityLevel,
        ) ?: existingPlan?.estimatedMaintenanceKcal?.takeIf { it > 0 }
    val deficitGuardrail = remember(dailyTargetKcal, estimatedMaintenanceKcal) {
        MiniCutRules.deficitGuardrail(
            targetKcal = dailyTargetKcal,
            maintenanceKcal = estimatedMaintenanceKcal,
        )
    }
    val hasPlanChanges = remember(
        existingPlan,
        startDate,
        durationWeeks,
        dailyTargetKcal,
        goalMode,
        activityLevel,
        estimatedMaintenanceKcal,
    ) {
        existingPlan?.let {
            it.startDate != startDate ||
                it.durationWeeks != durationWeeks ||
                it.dailyTargetKcal != dailyTargetKcal ||
                it.goalMode != goalMode ||
                it.activityLevel != activityLevel ||
                it.estimatedMaintenanceKcal != (estimatedMaintenanceKcal ?: 0)
        } ?: true
    }
    val isSuitabilityConfirmed = acknowledgeNotStrengthPeak && acknowledgeNotLongTermDiet && acknowledgeNotMassGainAvoidance
    val canSavePlan = hasPlanChanges && isSuitabilityConfirmed && deficitGuardrail.canSave

    fun persistNotificationSettings(updated: NotificationSettings) {
        notificationSettings = updated
        NotificationPreferences.save(context, updated)
        syncMiniCutNotifications(context, updated)
        inlineFeedbackTone = MiniCutInlineFeedbackTone.Info
        inlineFeedbackMessage = "리마인더 설정을 저장했어요."
    }

    fun openReminderTimePicker(slot: ReminderSlot) {
        val currentTime = notificationSettings.settingFor(slot).time
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                persistNotificationSettings(
                    notificationSettings.updateSlot(slot) { it.copy(time = ReminderTime(hourOfDay = hourOfDay, minute = minute)) },
                )
            },
            currentTime.hourOfDay,
            currentTime.minute,
            true,
        ).apply {
            setOnShowListener {
                getButton(TimePickerDialog.BUTTON_POSITIVE)?.setTextColor(dialogButtonColor)
                getButton(TimePickerDialog.BUTTON_NEGATIVE)?.setTextColor(dialogButtonColor)
            }
        }.show()
    }

    if (showDataResetDialog) {
        AlertDialog(
            onDismissRequest = { showDataResetDialog = false },
            title = {
                Text(
                    text = "저장 데이터 전체 삭제",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = "음식 기록, 캘린더 집계, 현재 플랜을 모두 삭제합니다. 이 작업은 되돌릴 수 없어요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDataResetDialog = false
                        viewModel.clearAllData()
                        inlineFeedbackTone = MiniCutInlineFeedbackTone.Caution
                        inlineFeedbackMessage = "저장 데이터를 모두 삭제했어요. 필요하면 새 플랜을 바로 다시 설정해보세요."
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("전체 삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDataResetDialog = false }) {
                    Text("취소")
                }
            },
        )
    }

    MiniCutBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                SavePlanBar(
                    buttonLabel = if (hasExistingPlan) "플랜 다시 저장" else "플랜 저장하고 시작하기",
                    enabled = canSavePlan,
                    suitabilityConfirmed = isSuitabilityConfirmed,
                    deficitGuardrail = deficitGuardrail,
                    onSave = {
                        viewModel.savePlan(
                            startDate = startDate,
                            durationWeeks = durationWeeks,
                            dailyTargetKcal = dailyTargetKcal,
                            goalMode = goalMode,
                            activityLevel = activityLevel,
                            estimatedMaintenanceKcal = estimatedMaintenanceKcal ?: 0,
                        )
                        inlineFeedbackTone = MiniCutInlineFeedbackTone.Info
                        inlineFeedbackMessage = "플랜을 저장했어요. 목적에 맞는 안내와 종료 후 유지 가이드가 함께 반영됩니다."
                        onSaved()
                    },
                )
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = MiniCutScreenHorizontalPadding,
                    end = MiniCutScreenHorizontalPadding,
                    top = 20.dp,
                    bottom = 28.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    MiniCutSectionHeader(
                        kicker = startDate.asCompactDate(),
                        title = "미니컷 플랜",
                        subtitle = "짧은 기간과 하루 목표를 먼저 정하면 기록/복기가 훨씬 쉬워집니다.",
                    )
                }
                inlineFeedbackMessage?.let { message ->
                    item {
                        MiniCutInlineFeedback(
                            message = message,
                            tone = inlineFeedbackTone,
                        )
                    }
                }
                item {
                    PlanFocusCard(
                        durationWeeks = durationWeeks,
                        dailyTargetKcal = dailyTargetKcal,
                        goalMode = goalMode,
                        endDate = endDate,
                    )
                }
                item {
                    PlanSummaryCard(
                        startDate = startDate,
                        endDate = endDate,
                        durationWeeks = durationWeeks,
                        dailyTargetKcal = dailyTargetKcal,
                        goalMode = goalMode,
                        activityLevel = activityLevel,
                        estimatedMaintenanceKcal = estimatedMaintenanceKcal,
                    )
                }
                item {
                    StepCard(
                        step = "1",
                        title = "시작일 선택",
                        description = "시작일을 기준으로 종료일이 자동 계산됩니다.",
                    ) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val pickerDialog =
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, day -> startDate = LocalDate.of(year, month + 1, day) },
                                        startDate.year,
                                        startDate.monthValue - 1,
                                        startDate.dayOfMonth,
                                    )
                                pickerDialog.setOnShowListener {
                                    pickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE)?.setTextColor(dialogButtonColor)
                                    pickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)?.setTextColor(dialogButtonColor)
                                }
                                pickerDialog.show()
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        ) {
                            Text(startDate.asCompactDate(), style = MaterialTheme.typography.titleMedium)
                        }
                        SupportingText("예상 종료일은 ${endDate.asCompactDate()}입니다.")
                    }
                }
                item {
                    StepCard(
                        step = "2",
                        title = "집중 기간 선택",
                        description = "2~6주 안에서 짧게 끝내는 미니컷 원칙을 유지하세요.",
                    ) {
                        SelectionChips(
                            options = MiniCutRules.MIN_WEEKS..MiniCutRules.MAX_WEEKS,
                            selectedValue = durationWeeks,
                            onSelect = { durationWeeks = it },
                            label = { "${it}주" },
                        )
                    }
                }
                item {
                    StepCard(
                        step = "3",
                        title = "하루 목표 칼로리",
                        description = "오늘 남음/초과 계산에 사용할 기준을 고르세요.",
                    ) {
                        SelectionChips(
                            options = MiniCutRules.TARGET_OPTIONS_KCAL,
                            selectedValue = dailyTargetKcal,
                            onSelect = { dailyTargetKcal = it },
                            label = Int::asKcal,
                        )
                    }
                }
                item {
                    StepCard(
                        step = "4",
                        title = "플랜 목적 선택",
                        description = "목표에 따라 실행 강도와 종료 후 전략이 달라집니다.",
                    ) {
                        SelectionChips(
                            options = MiniCutGoalMode.entries,
                            selectedValue = goalMode,
                            onSelect = { goalMode = it },
                            label = MiniCutGoalMode::displayName,
                        )
                        SupportingText(goalMode.shortDescription)
                    }
                }
                item {
                    StepCard(
                        step = "5",
                        title = "사전 적합성 점검",
                        description = "아래 조건을 모두 확인해야 안전 모드로 플랜을 저장할 수 있어요.",
                    ) {
                        SuitabilityCheckRow(
                            checked = acknowledgeNotStrengthPeak,
                            label = "최대 근력(파워/역도) 시합 직전 컨디션이 목적은 아닙니다.",
                            onToggle = { acknowledgeNotStrengthPeak = !acknowledgeNotStrengthPeak },
                        )
                        SuitabilityCheckRow(
                            checked = acknowledgeNotLongTermDiet,
                            label = "이번 플랜은 장기 감량 전략이 아니라 2~6주 단기 개입입니다.",
                            onToggle = { acknowledgeNotLongTermDiet = !acknowledgeNotLongTermDiet },
                        )
                        SuitabilityCheckRow(
                            checked = acknowledgeNotMassGainAvoidance,
                            label = "벌크업 회피용 반복 다이어트가 아니라 목적 있는 단기 단계입니다.",
                            onToggle = { acknowledgeNotMassGainAvoidance = !acknowledgeNotMassGainAvoidance },
                        )
                        if (!isSuitabilityConfirmed) {
                            MiniCutInlineFeedback(
                                message = "3개 항목을 모두 체크하면 저장 버튼이 활성화됩니다.",
                                tone = MiniCutInlineFeedbackTone.Caution,
                            )
                        }
                    }
                }
                item {
                    StepCard(
                        step = "6",
                        title = "결핍 강도 가드레일",
                        description = "체중과 활동 수준으로 유지칼로리를 추정해 현재 목표의 안전 범위를 점검합니다.",
                    ) {
                        SelectionChips(
                            options = ActivityLevel.entries,
                            selectedValue = activityLevel,
                            onSelect = { activityLevel = it },
                            label = ActivityLevel::displayName,
                        )
                        OutlinedTextField(
                            value = bodyWeightText,
                            onValueChange = { bodyWeightText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("현재 체중(kg, 선택)") },
                            placeholder = { Text("예: 78.4") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                        if (estimatedMaintenanceKcal != null) {
                            SupportingText("추정 유지칼로리: ${estimatedMaintenanceKcal.asKcal()} (${activityLevel.displayName})")
                        }
                        DeficitGuardrailCard(guardrail = deficitGuardrail)
                    }
                }
                item {
                    TargetGuidanceCard(guidance = targetGuidance)
                }
                item {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationPermissionGranted) {
                        NotificationPermissionPromptCard(
                            onRequestPermission = {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                        )
                    }
                }
                item {
                    NotificationSettingsCard(
                        settings = notificationSettings,
                        notificationPermissionGranted = notificationPermissionGranted,
                        onCadenceChange = { cadence ->
                            persistNotificationSettings(notificationSettings.copy(cadence = cadence))
                        },
                        onToggleSlot = { slot, enabled ->
                            persistNotificationSettings(notificationSettings.updateSlot(slot) { it.copy(enabled = enabled) })
                        },
                        onEditTime = ::openReminderTimePicker,
                    )
                }
                item {
                    DataManagementCard(
                        isPrivacyOptionsRequired = isPrivacyOptionsRequired,
                        onPrivacyOptionsClick = onPrivacyOptionsClick,
                        onClearAllClick = { showDataResetDialog = true },
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationPermissionPromptCard(
    onRequestPermission: () -> Unit,
) {
    Card(
        shape = MiniCutCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("알림 권한 확인", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Android 13 이상에서는 리마인더를 받으려면 알림 권한을 한 번 허용해야 해요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onRequestPermission) {
                Text("알림 권한 허용")
            }
        }
    }
}

@Composable
private fun TargetGuidanceCard(guidance: TargetGuidance) {
    val containerColor: Color
    val accentColor: Color
    when (guidance.tone) {
        TargetGuidanceTone.Caution -> {
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
            accentColor = MaterialTheme.colorScheme.error
        }
        TargetGuidanceTone.Recommended -> {
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
            accentColor = MaterialTheme.colorScheme.primary
        }
        TargetGuidanceTone.Flexible -> {
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
            accentColor = MaterialTheme.colorScheme.tertiary
        }
    }

    Card(
        shape = MiniCutCardShape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.24f)),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(guidance.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = accentColor)
            Text(guidance.body, style = MaterialTheme.typography.bodyMedium)
            Text(guidance.footnote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlanHeroCard() {
    Card(
        shape = MiniCutCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "미니컷 플랜",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "짧고 선명하게 끝내는 미니컷 설정",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "기간과 하루 기준을 먼저 정해두면 기록·복기·남음/초과 확인이 훨씬 쉬워집니다.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SelectionChips {
                HeroBadge("2~6주 집중")
                HeroBadge("하루 목표 설정")
                HeroBadge("남음/초과 즉시 확인")
            }
        }
    }
}

@Composable
private fun HeroBadge(label: String) {
    Surface(
        shape = MiniCutPillShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        tonalElevation = 0.dp,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun PrincipleCard() {
    Card(
        shape = MiniCutCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "설계 원칙",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "짧게 끝내고, 피로가 커지기 전에 전환하는 것이 핵심입니다. 이 플랜은 빠른 감량보다 유지 가능한 개입에 맞춰 설계돼요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlanFocusCard(
    durationWeeks: Int,
    dailyTargetKcal: Int,
    goalMode: MiniCutGoalMode,
    endDate: LocalDate,
) {
    Card(
        shape = MiniCutCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "이번 플랜의 판단 기준",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${durationWeeks}주 동안 ${dailyTargetKcal.asKcal()}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${endDate.asCompactDate()}에 종료를 전제로 ${goalMode.displayName} 모드로 운영합니다. 아래 단계는 이 한 문장을 정확히 만들기 위한 최소 입력만 모았습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                )
            }
            SelectionChips {
                HeroBadge("기간 고정")
                HeroBadge("하루 기준")
                HeroBadge("안전 가드레일")
            }
        }
    }
}

@Composable
private fun DataManagementCard(
    isPrivacyOptionsRequired: Boolean,
    onPrivacyOptionsClick: () -> Unit,
    onClearAllClick: () -> Unit,
) {
    Card(
        shape = MiniCutCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "데이터 관리",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "기기에 저장된 음식 기록과 캘린더 집계, 플랜을 한 번에 초기화합니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isPrivacyOptionsRequired) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onPrivacyOptionsClick,
                ) {
                    Text("광고 개인정보 옵션 관리")
                }
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onClearAllClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
            ) {
                Text("저장 데이터 전체 삭제")
            }
        }
    }
}

@Composable
private fun PlanSummaryCard(
    startDate: LocalDate,
    endDate: LocalDate,
    durationWeeks: Int,
    dailyTargetKcal: Int,
    goalMode: MiniCutGoalMode,
    activityLevel: ActivityLevel,
    estimatedMaintenanceKcal: Int?,
) {
    Card(
        shape = MiniCutCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "현재 설정 요약",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "저장 즉시 홈과 캘린더에서 같은 목표로 남음/초과를 계산합니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SummaryHighlight(label = "집중 기간", value = "${durationWeeks}주")
            SummaryRow(label = "시작일", value = startDate.asCompactDate())
            SummaryRow(label = "종료일", value = endDate.asCompactDate())
            SummaryRow(label = "플랜 목적", value = goalMode.displayName)
            SummaryRow(label = "활동 수준", value = activityLevel.displayName)
            if (estimatedMaintenanceKcal != null) {
                SummaryRow(label = "추정 유지", value = estimatedMaintenanceKcal.asKcal())
            }
            SummaryRow(label = "하루 목표", value = dailyTargetKcal.asKcal())
        }
    }
}

@Composable
private fun SummaryHighlight(label: String, value: String) {
    Surface(
        shape = MiniCutPillShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun StepCard(
    step: String,
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        shape = MiniCutCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier.padding(top = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        shape = MiniCutPillShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = step,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun <T> SelectionChips(
    options: Iterable<T>,
    selectedValue: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
) {
    SelectionChips {
        options.forEach { option ->
            val optionLabel = label(option)
            val isSelected = selectedValue == option
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(option) },
                modifier = Modifier.semantics {
                    selected = isSelected
                    contentDescription = "$optionLabel ${selectionStateLabel(isSelected)}"
                },
                label = { Text(optionLabel) },
            )
        }
    }
}

private fun selectionStateLabel(isSelected: Boolean): String =
    if (isSelected) "선택됨" else "선택 안 됨"

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectionChips(
    content: @Composable FlowRowScope.() -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
private fun SupportingText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun DeficitGuardrailCard(guardrail: DeficitGuardrail) {
    val accent =
        when (guardrail.level) {
            DeficitRiskLevel.Safe -> MaterialTheme.colorScheme.primary
            DeficitRiskLevel.Caution -> MaterialTheme.colorScheme.tertiary
            DeficitRiskLevel.High -> MaterialTheme.colorScheme.error
            DeficitRiskLevel.Unknown -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MiniCutPanelShape,
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.24f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                guardrail.title,
                style = MaterialTheme.typography.titleSmall,
                color = accent,
                fontWeight = FontWeight.Bold,
            )
            guardrail.maintenanceKcal?.let { maintenance ->
                val deficitText =
                    guardrail.deficitKcal?.let { "${it.asKcal()} / ${guardrail.deficitPercent ?: 0f}%" }
                        ?: "계산 대기"
                Text(
                    "유지 ${maintenance.asKcal()} · 결핍 ${deficitText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                guardrail.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SavePlanBar(
    buttonLabel: String,
    enabled: Boolean,
    suitabilityConfirmed: Boolean,
    deficitGuardrail: DeficitGuardrail,
    onSave: () -> Unit,
) {
    MiniCutBottomActionBar {
        Button(
            onClick = onSave,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            shape = MiniCutPillShape,
            contentPadding = PaddingValues(vertical = 14.dp),
        ) {
            Text(buttonLabel, style = MaterialTheme.typography.titleMedium)
        }
        Text(
            text =
                when {
                    !suitabilityConfirmed -> "사전 적합성 3개 항목을 체크하면 저장할 수 있어요."
                    !deficitGuardrail.canSave -> "결핍 강도가 높아 잠겨 있어요. 목표를 상향하거나 유지칼로리를 다시 확인하세요."
                    enabled -> "저장 후 오늘 기록과 캘린더가 같은 기준을 사용합니다."
                    else -> "현재 설정은 이미 저장된 플랜과 동일합니다."
                },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SuitabilityCheckRow(
    checked: Boolean,
    label: String,
    onToggle: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MiniCutPillShape,
        color = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
        onClick = onToggle,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (checked) "✓" else "○", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(
                label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
