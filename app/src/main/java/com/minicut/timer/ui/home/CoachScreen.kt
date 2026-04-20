package com.minicut.timer.ui.home

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minicut.timer.data.local.NotificationPreferences
import com.minicut.timer.notifications.NotificationSettings
import com.minicut.timer.notifications.ReminderSlot
import com.minicut.timer.notifications.ReminderTime
import com.minicut.timer.notifications.syncMiniCutNotifications
import com.minicut.timer.ui.components.MiniCutBackdrop
import com.minicut.timer.ui.components.MiniCutSectionHeader
import com.minicut.timer.ui.util.asDisplayDate
import com.minicut.timer.ui.util.miniCutRepository
import kotlinx.coroutines.launch

@Composable
fun CoachScreen(
    onOpenPlan: (Int?) -> Unit,
) {
    val context = LocalContext.current
    val repository = context.miniCutRepository
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var notificationSettings by remember { mutableStateOf(NotificationPreferences.load(context)) }
    val notificationPermissionGranted =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    val dialogButtonColor = androidx.compose.material3.MaterialTheme.colorScheme.primary.toArgb()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()

    fun showMessage(message: String) {
        snackbarScope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun persistNotificationSettings(updated: NotificationSettings) {
        notificationSettings = updated
        NotificationPreferences.save(context, updated)
        syncMiniCutNotifications(context, updated)
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
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 116.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    MiniCutSectionHeader(
                        kicker = uiState.currentDate.asDisplayDate(),
                        title = "코칭 허브",
                        subtitle = "회복, 근손실 방어, 브레이크 판단을 한곳에서 관리하세요.",
                    )
                }
                item {
                    BodyCompositionCheckCard(
                        todayCheck = uiState.todayConditionCheck,
                        weeklyWeightTrend = uiState.weeklyWeightTrend,
                        recoveryRiskAssessment = uiState.recoveryRiskAssessment,
                        recommendedProteinGrams = uiState.recommendedProteinGrams,
                        calorieAdjustmentRecommendation = uiState.calorieAdjustmentRecommendation,
                        onOpenPlan = onOpenPlan,
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
            }
        }
    }
}
