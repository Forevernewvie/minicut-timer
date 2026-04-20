package com.minicut.timer.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.minicut.timer.domain.model.CalorieAdjustmentDirection
import com.minicut.timer.domain.model.CalorieAdjustmentRecommendation
import com.minicut.timer.domain.model.DailyConditionCheck
import com.minicut.timer.domain.model.DietBreakRecommendation
import com.minicut.timer.domain.model.LeanMassProtectionGrade
import com.minicut.timer.domain.model.LeanMassProtectionScore
import com.minicut.timer.domain.model.RelapsePreventionInsight
import com.minicut.timer.domain.model.RecoveryRiskAssessment
import com.minicut.timer.domain.model.RecoveryRiskStatus
import com.minicut.timer.domain.model.StrengthTrend
import com.minicut.timer.domain.model.StrengthTrendStatus
import com.minicut.timer.domain.model.WeeklyWeightTrend
import com.minicut.timer.domain.model.WeeklyWeightTrendStatus
import com.minicut.timer.domain.rules.RelapsePreventionCatalog
import com.minicut.timer.notifications.NotificationSettings
import com.minicut.timer.notifications.ReminderCadence
import com.minicut.timer.notifications.ReminderSetting
import com.minicut.timer.notifications.ReminderSlot
import com.minicut.timer.ui.components.MiniCutCardShape
import com.minicut.timer.ui.components.MiniCutPanelShape
import com.minicut.timer.ui.components.MiniCutPillShape
import com.minicut.timer.ui.util.asKcal

@Composable
internal fun NotificationSettingsCard(
    settings: NotificationSettings,
    notificationPermissionGranted: Boolean,
    onCadenceChange: (ReminderCadence) -> Unit,
    onToggleSlot: (ReminderSlot, Boolean) -> Unit,
    onEditTime: (ReminderSlot) -> Unit,
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
            Text("리마인더", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (!notificationPermissionGranted) {
                Text(
                    "알림 권한이 꺼져 있어 시스템 알림이 보이지 않을 수 있어요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReminderCadence.entries.forEach { cadence ->
                    FilterChip(
                        selected = settings.cadence == cadence,
                        onClick = { onCadenceChange(cadence) },
                        label = { Text(cadence.displayName) },
                    )
                }
            }
            ReminderSlot.entries.forEach { slot ->
                ReminderSettingRow(
                    slot = slot,
                    setting = settings.settingFor(slot),
                    onToggle = { enabled -> onToggleSlot(slot, enabled) },
                    onEditTime = { onEditTime(slot) },
                )
            }
        }
    }
}

@Composable
private fun ReminderSettingRow(
    slot: ReminderSlot,
    setting: ReminderSetting,
    onToggle: (Boolean) -> Unit,
    onEditTime: () -> Unit,
) {
    val reminderStatusText = if (setting.enabled) "켜짐" else "꺼짐"
    Card(
        shape = MiniCutPanelShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = "${slot.displayName} 리마인더, ${setting.time.formatted()}, $reminderStatusText"
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(slot.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    if (setting.enabled) "${setting.time.formatted()} · ${slot.title}" else "현재 꺼져 있어요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = setting.enabled,
                onCheckedChange = onToggle,
            )
            OutlinedButton(onClick = onEditTime) {
                Text(setting.time.formatted())
            }
        }
    }
}

@Composable
internal fun LeanMassProtectionCard(
    score: LeanMassProtectionScore,
    strengthTrend: StrengthTrend,
    relapsePreventionInsight: RelapsePreventionInsight,
    dietBreakRecommendation: DietBreakRecommendation,
    onOpenPlan: () -> Unit,
) {
    val accent =
        when (score.grade) {
            LeanMassProtectionGrade.Excellent -> MaterialTheme.colorScheme.primary
            LeanMassProtectionGrade.Good -> MaterialTheme.colorScheme.primary
            LeanMassProtectionGrade.Moderate -> MaterialTheme.colorScheme.tertiary
            LeanMassProtectionGrade.Low -> MaterialTheme.colorScheme.error
            LeanMassProtectionGrade.NoData -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    Card(
        shape = MiniCutCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("근손실 방어 점수", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Surface(
                color = accent.copy(alpha = 0.12f),
                contentColor = accent,
                shape = MiniCutPillShape,
            ) {
                Text(
                    "점수 ${score.score}/100 · 단백질 달성 ${score.proteinHitDays}일 · 저항운동 달성 ${score.resistanceHitDays}일",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                score.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val strengthAccent =
                when (strengthTrend.status) {
                    StrengthTrendStatus.Up -> MaterialTheme.colorScheme.primary
                    StrengthTrendStatus.Stable -> MaterialTheme.colorScheme.tertiary
                    StrengthTrendStatus.Down -> MaterialTheme.colorScheme.error
                    StrengthTrendStatus.NoData -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MiniCutPillShape,
                color = strengthAccent.copy(alpha = 0.10f),
            ) {
                val changeText = strengthTrend.changePercent?.let { "${it}%" } ?: "데이터 대기"
                Text(
                    "핵심 리프트 추세 ${changeText} · ${strengthTrend.message}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = strengthAccent,
                )
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MiniCutPanelShape,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.24f)),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        dietBreakRecommendation.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        dietBreakRecommendation.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (dietBreakRecommendation.shouldSuggest) {
                        OutlinedButton(
                            onClick = onOpenPlan,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("플랜에서 유지 전환 체크")
                        }
                    }
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MiniCutPanelShape,
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.24f)),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "재발 방지 툴킷",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (relapsePreventionInsight.recurringTrigger != null) {
                        Text(
                            "반복 트리거: ${relapsePreventionInsight.recurringTrigger} (${relapsePreventionInsight.triggerCount}회)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        relapsePreventionInsight.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    relapsePreventionInsight.recommendedAction?.let { action ->
                        Text(
                            "권장 대응: $action",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BodyCompositionCheckCard(
    todayCheck: DailyConditionCheck?,
    weeklyWeightTrend: WeeklyWeightTrend,
    recoveryRiskAssessment: RecoveryRiskAssessment,
    recommendedProteinGrams: Int?,
    calorieAdjustmentRecommendation: CalorieAdjustmentRecommendation,
    onOpenPlan: (Int?) -> Unit,
    onSave: (Float?, Int?, Int?, Float?, String?, String?, Float?, Int?, Int?, Int?, Int?) -> Unit,
    onInvalidInput: (String) -> Unit,
) {
    val saveKey = todayCheck?.updatedAt?.toString().orEmpty()
    var bodyWeightText by rememberSaveable(saveKey) {
        mutableStateOf(
            todayCheck?.bodyWeightKg?.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() }.orEmpty(),
        )
    }
    var proteinText by rememberSaveable(saveKey) { mutableStateOf(todayCheck?.proteinGrams?.toString().orEmpty()) }
    var resistanceSetsText by rememberSaveable(saveKey) { mutableStateOf(todayCheck?.resistanceSets?.toString().orEmpty()) }
    var mainLiftKgText by rememberSaveable(saveKey) { mutableStateOf(todayCheck?.mainLiftKg?.toString().orEmpty()) }
    var relapseTrigger by rememberSaveable(saveKey) { mutableStateOf(todayCheck?.relapseTrigger.orEmpty()) }
    var copingAction by rememberSaveable(saveKey) { mutableStateOf(todayCheck?.copingAction.orEmpty()) }
    var sleepHoursText by rememberSaveable(saveKey) { mutableStateOf(todayCheck?.sleepHours?.toString().orEmpty()) }
    var fatigueScoreText by rememberSaveable(saveKey) { mutableStateOf(todayCheck?.fatigueScore?.toString().orEmpty()) }
    var hungerScoreText by rememberSaveable(saveKey) { mutableStateOf(todayCheck?.hungerScore?.toString().orEmpty()) }
    var moodScoreText by rememberSaveable(saveKey) { mutableStateOf(todayCheck?.moodScore?.toString().orEmpty()) }
    var workoutPerformanceScoreText by rememberSaveable(saveKey) { mutableStateOf(todayCheck?.workoutPerformanceScore?.toString().orEmpty()) }

    val trendColor =
        when (weeklyWeightTrend.status) {
            WeeklyWeightTrendStatus.InRange -> MaterialTheme.colorScheme.primary
            WeeklyWeightTrendStatus.NoData -> MaterialTheme.colorScheme.onSurfaceVariant
            WeeklyWeightTrendStatus.TooSlow -> MaterialTheme.colorScheme.tertiary
            WeeklyWeightTrendStatus.TooFast -> MaterialTheme.colorScheme.error
            WeeklyWeightTrendStatus.GainOrStall -> MaterialTheme.colorScheme.error
        }
    val trendRateText = weeklyWeightTrend.ratePercentPerWeek?.let { "주당 ${String.format("%.2f", it)}%" } ?: "속도 계산 대기"
    val recoveryAccent =
        when (recoveryRiskAssessment.status) {
            RecoveryRiskStatus.NoData -> MaterialTheme.colorScheme.onSurfaceVariant
            RecoveryRiskStatus.Stable -> MaterialTheme.colorScheme.primary
            RecoveryRiskStatus.Watch -> MaterialTheme.colorScheme.tertiary
            RecoveryRiskStatus.High -> MaterialTheme.colorScheme.error
        }

    Card(
        shape = MiniCutCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("근손실 방어 체크인", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "체중·단백질·저항운동 세트를 함께 기록하면 감량 속도와 근손실 리스크를 바로 점검할 수 있어요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                color = trendColor.copy(alpha = 0.12f),
                contentColor = trendColor,
                shape = MiniCutPillShape,
            ) {
                Text(
                    "$trendRateText · ${weeklyWeightTrend.message}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = recoveryAccent.copy(alpha = 0.10f),
                shape = MiniCutPillShape,
            ) {
                Text(
                    "회복 리스크 · ${recoveryRiskAssessment.message}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = recoveryAccent,
                )
            }
            if (recommendedProteinGrams != null) {
                Text(
                    "권장 단백질: ${recommendedProteinGrams}g (최근 체중 × 2.0g)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            val recommendationAccent =
                when (calorieAdjustmentRecommendation.direction) {
                    CalorieAdjustmentDirection.Keep -> MaterialTheme.colorScheme.onSurfaceVariant
                    CalorieAdjustmentDirection.Increase -> MaterialTheme.colorScheme.error
                    CalorieAdjustmentDirection.Decrease -> MaterialTheme.colorScheme.tertiary
                }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MiniCutPanelShape,
                color = recommendationAccent.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, recommendationAccent.copy(alpha = 0.28f)),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        calorieAdjustmentRecommendation.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = recommendationAccent,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "현재 ${calorieAdjustmentRecommendation.currentTargetKcal.asKcal()} → 제안 ${calorieAdjustmentRecommendation.suggestedTargetKcal.asKcal()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        calorieAdjustmentRecommendation.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (calorieAdjustmentRecommendation.actionable) {
                        OutlinedButton(
                            onClick = { onOpenPlan(calorieAdjustmentRecommendation.suggestedTargetKcal) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("플랜 화면에서 제안값 확인")
                        }
                    }
                }
            }
            OutlinedTextField(
                value = bodyWeightText,
                onValueChange = { bodyWeightText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("오늘 체중(kg)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = proteinText,
                    onValueChange = { proteinText = it.filter(Char::isDigit) },
                    modifier = Modifier.weight(1f),
                    label = { Text("단백질(g)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    value = resistanceSetsText,
                    onValueChange = { resistanceSetsText = it.filter(Char::isDigit) },
                    modifier = Modifier.weight(1f),
                    label = { Text("저항운동 세트") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                )
            }
            OutlinedTextField(
                value = mainLiftKgText,
                onValueChange = { mainLiftKgText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("핵심 리프트(kg, 선택)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            )
            Text(
                "폭식/이탈 트리거",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RelapsePreventionCatalog.triggerOptions.forEach { trigger ->
                    FilterChip(
                        selected = relapseTrigger == trigger,
                        onClick = { relapseTrigger = if (relapseTrigger == trigger) "" else trigger },
                        label = { Text(trigger) },
                    )
                }
            }
            Text(
                "대응 루틴",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RelapsePreventionCatalog.copingActionOptions.forEach { action ->
                    FilterChip(
                        selected = copingAction == action,
                        onClick = { copingAction = if (copingAction == action) "" else action },
                        label = { Text(action) },
                    )
                }
            }
            OutlinedTextField(
                value = sleepHoursText,
                onValueChange = { sleepHoursText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("수면 시간(시간, 선택)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = fatigueScoreText,
                    onValueChange = { fatigueScoreText = it.filter(Char::isDigit) },
                    modifier = Modifier.weight(1f),
                    label = { Text("피로(1~5)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    value = hungerScoreText,
                    onValueChange = { hungerScoreText = it.filter(Char::isDigit) },
                    modifier = Modifier.weight(1f),
                    label = { Text("허기(1~5)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = moodScoreText,
                    onValueChange = { moodScoreText = it.filter(Char::isDigit) },
                    modifier = Modifier.weight(1f),
                    label = { Text("기분(1~5)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    value = workoutPerformanceScoreText,
                    onValueChange = { workoutPerformanceScoreText = it.filter(Char::isDigit) },
                    modifier = Modifier.weight(1f),
                    label = { Text("수행감(1~5)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                )
            }
            Button(
                onClick = {
                    val validation =
                        validateConditionCheckInput(
                            bodyWeightText = bodyWeightText,
                            proteinText = proteinText,
                            resistanceSetsText = resistanceSetsText,
                            mainLiftKgText = mainLiftKgText,
                            relapseTrigger = relapseTrigger,
                            copingAction = copingAction,
                            sleepHoursText = sleepHoursText,
                            fatigueScoreText = fatigueScoreText,
                            hungerScoreText = hungerScoreText,
                            moodScoreText = moodScoreText,
                            workoutPerformanceScoreText = workoutPerformanceScoreText,
                        )
                    if (!validation.isValid) {
                        onInvalidInput(validation.errorMessage ?: "입력값을 확인해주세요.")
                        return@Button
                    }
                    onSave(
                        validation.bodyWeightKg,
                        validation.proteinGrams,
                        validation.resistanceSets,
                        validation.mainLiftKg,
                        validation.relapseTrigger,
                        validation.copingAction,
                        validation.sleepHours,
                        validation.fatigueScore,
                        validation.hungerScore,
                        validation.moodScore,
                        validation.workoutPerformanceScore,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("체크인 저장")
            }
        }
    }
}
