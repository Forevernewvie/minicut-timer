package com.minicut.timer.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.minicut.timer.domain.model.CalorieEntry
import com.minicut.timer.domain.rules.MiniCutRules
import com.minicut.timer.ui.components.MiniCutBackdrop
import com.minicut.timer.ui.components.MiniCutEmptyState
import com.minicut.timer.ui.components.MiniCutInlineFeedback
import com.minicut.timer.ui.components.MiniCutInlineFeedbackTone
import com.minicut.timer.ui.components.MiniCutMetricTile
import com.minicut.timer.ui.components.MiniCutPanelShape
import com.minicut.timer.ui.components.MiniCutPillShape
import com.minicut.timer.ui.components.MiniCutScreenHorizontalPadding
import com.minicut.timer.ui.components.MiniCutSectionHeader
import com.minicut.timer.ui.util.asCompactDate
import com.minicut.timer.ui.util.asCompactKcal
import com.minicut.timer.ui.util.asDisplayDate
import com.minicut.timer.ui.util.asDisplayMonth
import com.minicut.timer.ui.util.asKcal
import com.minicut.timer.ui.util.asMealHeadline
import com.minicut.timer.ui.util.miniCutRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen() {
    val context = LocalContext.current
    val repository = context.miniCutRepository
    val viewModel: CalendarViewModel = viewModel(factory = CalendarViewModel.factory(repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var inlineFeedbackMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var inlineFeedbackTone by rememberSaveable { mutableStateOf(MiniCutInlineFeedbackTone.Info) }

    val summaryMap = remember(uiState.summaries) { uiState.summaries.associateBy { it.date } }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }
    val orderedDays = remember(firstDayOfWeek) { orderedDaysOfWeek(firstDayOfWeek) }
    val monthRange = rememberCalendarMonthRange()
    val calendarState = rememberCalendarState(
        startMonth = monthRange.start,
        endMonth = monthRange.end,
        firstVisibleMonth = uiState.month,
        firstDayOfWeek = firstDayOfWeek,
    )
    val scope = rememberCoroutineScope()
    val currentMonth = remember(uiState.currentDate) { YearMonth.from(uiState.currentDate) }

    LaunchedEffect(uiState.month) {
        if (calendarState.firstVisibleMonth.yearMonth != uiState.month) {
            calendarState.animateScrollToMonth(uiState.month)
        }
    }

    LaunchedEffect(calendarState) {
        snapshotFlow { calendarState.firstVisibleMonth.yearMonth }
            .collect { visibleMonth ->
                if (visibleMonth != uiState.month) viewModel.setMonth(visibleMonth)
            }
    }

    MiniCutBackdrop {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = MiniCutScreenHorizontalPadding,
                    end = MiniCutScreenHorizontalPadding,
                    top = 16.dp,
                    bottom = 108.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    MiniCutSectionHeader(
                        kicker = uiState.currentDate.asDisplayDate(),
                        title = "캘린더 로그",
                        subtitle = "기록한 날을 빠르게 훑고, 날짜를 눌러 하루 식사 로그를 바로 정리하세요.",
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
                    MonthOverviewRow(
                        loggedDaysCount = uiState.monthLoggedDaysCount,
                        monthTotalCalories = uiState.monthTotalCalories,
                        selectedDate = uiState.selectedDate,
                        selectedDayTotalCalories = uiState.selectedDayTotalCalories,
                    )
                }
                if (uiState.monthLoggedDaysCount == 0) {
                    item {
                        CalendarEmptyMonthCard(
                            month = uiState.month,
                            isCurrentMonth = uiState.month == currentMonth,
                        )
                    }
                }
                item {
                    CalendarBoard(
                        month = uiState.month,
                        currentMonth = currentMonth,
                        orderedDays = orderedDays,
                        calendarState = calendarState,
                        summaryMap = summaryMap,
                        selectedDate = uiState.selectedDate,
                        currentDate = uiState.currentDate,
                        onPreviousMonth = {
                            scope.launch { calendarState.animateScrollToMonth(uiState.month.minusMonths(1)) }
                        },
                        onNextMonth = {
                            scope.launch { calendarState.animateScrollToMonth(uiState.month.plusMonths(1)) }
                        },
                        onJumpToCurrentMonth = {
                            scope.launch { calendarState.animateScrollToMonth(currentMonth) }
                        },
                        onSelectDay = { day ->
                            if (day.position == DayPosition.MonthDate) {
                                inlineFeedbackMessage = null
                                viewModel.selectDate(day.date)
                            } else {
                                inlineFeedbackTone = MiniCutInlineFeedbackTone.Caution
                                inlineFeedbackMessage = "이번 달 칸만 선택할 수 있어요. 월 이동 후 해당 날짜를 눌러주세요."
                            }
                        },
                    )
                }
            }
        }
    }

    uiState.selectedDate?.let { selectedDate ->
        CalendarEntriesBottomSheet(
            selectedDate = selectedDate,
            selectedEntries = uiState.selectedEntries,
            selectedDayTotalCalories = uiState.selectedDayTotalCalories,
            targetCalories = uiState.plan?.dailyTargetKcal ?: MiniCutRules.DEFAULT_TARGET_KCAL,
            onDismiss = viewModel::clearSelection,
            onDeleteEntry = { entryId ->
                viewModel.deleteEntry(entryId)
                inlineFeedbackTone = MiniCutInlineFeedbackTone.Info
                inlineFeedbackMessage = "해당 날짜의 식사 기록을 삭제했어요."
            },
        )
    }
}

private data class CalendarMonthRange(
    val start: YearMonth,
    val end: YearMonth,
)

@Composable
private fun rememberCalendarMonthRange(): CalendarMonthRange =
    remember {
        val currentMonth = YearMonth.now()
        CalendarMonthRange(
            start = currentMonth.minusMonths(CALENDAR_MONTH_RANGE_PADDING),
            end = currentMonth.plusMonths(CALENDAR_MONTH_RANGE_PADDING),
        )
    }

private const val CALENDAR_MONTH_RANGE_PADDING = 24L

@Composable
private fun MonthOverviewRow(
    loggedDaysCount: Int,
    monthTotalCalories: Int,
    selectedDate: LocalDate?,
    selectedDayTotalCalories: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MiniCutMetricTile(
            label = "기록한 날",
            value = "${loggedDaysCount}일",
            supporting = if (loggedDaysCount == 0) "아직 로그가 없어요" else "기록 습관이 쌓이고 있어요",
            modifier = Modifier.weight(1f),
        )
        MiniCutMetricTile(
            label = if (selectedDate == null) "월 누적" else "선택한 날",
            value = if (selectedDate == null) monthTotalCalories.asKcal() else selectedDayTotalCalories.asKcal(),
            supporting = if (selectedDate == null) "이번 달 섭취 합계" else selectedDate.asCompactDate(),
            modifier = Modifier.weight(1f),
            tint = if (selectedDate == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun CalendarBoard(
    month: YearMonth,
    currentMonth: YearMonth,
    orderedDays: List<DayOfWeek>,
    calendarState: com.kizitonwose.calendar.compose.CalendarState,
    summaryMap: Map<LocalDate, com.minicut.timer.domain.model.DailyCalorieSummary>,
    selectedDate: LocalDate?,
    currentDate: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToCurrentMonth: () -> Unit,
    onSelectDay: (CalendarDay) -> Unit,
) {
    Surface(
        shape = MiniCutPanelShape,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "이전 달")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = month.asDisplayMonth(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (month == currentMonth) "이번 달 기록" else "좌우로 넘겨 다른 달도 확인하세요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "다음 달")
                }
            }
            if (month != currentMonth) {
                FilledTonalButton(
                    onClick = onJumpToCurrentMonth,
                    shape = MiniCutPillShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) {
                    Text("이번 달로 돌아가기")
                }
            }
            WeekdayHeader(orderedDays)
            HorizontalCalendar(
                state = calendarState,
                userScrollEnabled = true,
                dayContent = { day ->
                    CalendarDayCell(
                        day = day,
                        summaryCalories = summaryMap[day.date]?.totalCalories,
                        isToday = day.date == currentDate,
                        isSelected = day.date == selectedDate,
                        onClick = { onSelectDay(day) },
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarEntriesBottomSheet(
    selectedDate: LocalDate,
    selectedEntries: List<CalorieEntry>,
    selectedDayTotalCalories: Int,
    targetCalories: Int,
    onDismiss: () -> Unit,
    onDeleteEntry: (Long) -> Unit,
) {
    val (statusText, statusColor) = selectedDaySummaryStatus(
        targetCalories = targetCalories,
        consumedCalories = selectedDayTotalCalories,
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = selectedDate.asCompactDate(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "하루 식사 로그와 목표 대비 상태를 확인하세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Card(
                shape = MiniCutPanelShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("그날 요약", fontWeight = FontWeight.Bold)
                    Text(
                        text = "총 ${selectedDayTotalCalories.asKcal()} / 목표 ${targetCalories.asKcal()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = statusText,
                        color = statusColor,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "식사 기록 ${selectedEntries.size}건",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (selectedEntries.isEmpty()) {
                MiniCutEmptyState(
                    title = "이 날은 아직 기록이 없어요",
                    body = "빈 날도 실패가 아니라 흐름을 확인하는 정보입니다. 오늘 기록 탭에서 식사를 추가하면 캘린더에 바로 표시돼요.",
                    actionLabel = "확인",
                    onAction = onDismiss,
                )
            } else {
                selectedEntries.forEach { entry ->
                    CalendarEntryCard(
                        entry = entry,
                        onDelete = { onDeleteEntry(entry.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarEmptyMonthCard(
    month: YearMonth,
    isCurrentMonth: Boolean,
) {
    MiniCutEmptyState(
        title = if (isCurrentMonth) "이번 달 로그가 아직 비어 있어요" else "${month.monthValue}월 로그가 비어 있어요",
        body = "식사 기록이 생기면 날짜 칸에 작은 칼로리 배지가 생깁니다. 기록한 날과 쉬어간 날을 한눈에 구분할 수 있어요.",
        accent = MaterialTheme.colorScheme.tertiary,
    )
}

@Composable
private fun CalendarEntryCard(
    entry: CalorieEntry,
    onDelete: () -> Unit,
) {
    Card(
        shape = MiniCutPanelShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = entry.foodName.asMealHeadline(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = entry.calories.asKcal(),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (entry.timeLabel.isNotBlank() || entry.note.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (entry.timeLabel.isNotBlank()) {
                            Text(entry.timeLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (entry.note.isNotBlank()) {
                            Text(entry.note, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = "기록 삭제")
            }
        }
    }
}

@Composable
private fun WeekdayHeader(daysOfWeek: List<DayOfWeek>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        daysOfWeek.forEach { dayOfWeek ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = dayOfWeek.asKoreanShortLabel(),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDay,
    summaryCalories: Int?,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val inMonth = day.position == DayPosition.MonthDate
    val hasLog = (summaryCalories ?: 0) > 0
    val backgroundColor =
        when {
            !inMonth -> Color.Transparent
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            hasLog -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.38f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f)
        }
    val borderColor =
        when {
            isSelected -> MaterialTheme.colorScheme.primary
            isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            else -> Color.Transparent
        }
    val chipContainerColor =
        when {
            isSelected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.primaryContainer
        }
    val chipContentColor =
        when {
            isSelected -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onPrimaryContainer
        }
    val accessibilitySummary =
        if (!inMonth) {
            "선택 불가 날짜"
        } else {
            buildString {
                append("${day.date.monthValue}월 ${day.date.dayOfMonth}일")
                if (isToday) append(", 오늘")
                if (isSelected) append(", 선택됨")
                append(
                    if (hasLog && summaryCalories != null) {
                        ", ${summaryCalories.asKcal()} 기록"
                    } else {
                        ", 기록 없음"
                    },
                )
            }
        }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .semantics(mergeDescendants = true) {
                    selected = isSelected
                    contentDescription = accessibilitySummary
                }
                .clickable(enabled = inMonth, onClick = onClick),
            shape = MiniCutPanelShape,
            color = backgroundColor,
            tonalElevation = if (isSelected) 2.dp else 0.dp,
            border = if (inMonth && borderColor != Color.Transparent) BorderStroke(1.dp, borderColor) else null,
        ) {
            if (inMonth) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = day.date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                        )
                        if (isToday) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                        }
                    }
                    if (hasLog && summaryCalories != null) {
                        Surface(
                            shape = MiniCutPillShape,
                            color = chipContainerColor,
                        ) {
                            Text(
                                text = summaryCalories.asCompactKcal(),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = chipContentColor,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 14.dp, height = 4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun selectedDaySummaryStatus(targetCalories: Int, consumedCalories: Int): Pair<String, Color> =
    if (MiniCutRules.isOverTarget(targetCalories, consumedCalories)) {
        Pair(
            "${MiniCutRules.overCalories(targetCalories, consumedCalories).asKcal()} 초과",
            MaterialTheme.colorScheme.error,
        )
    } else {
        Pair(
            "${MiniCutRules.remainingCalories(targetCalories, consumedCalories).asKcal()} 남음",
            MaterialTheme.colorScheme.primary,
        )
    }

private fun orderedDaysOfWeek(firstDayOfWeek: DayOfWeek): List<DayOfWeek> {
    val values = DayOfWeek.entries.toList()
    val startIndex = values.indexOf(firstDayOfWeek)
    return values.drop(startIndex) + values.take(startIndex)
}

private fun DayOfWeek.asKoreanShortLabel(): String =
    when (this) {
        DayOfWeek.MONDAY -> "월"
        DayOfWeek.TUESDAY -> "화"
        DayOfWeek.WEDNESDAY -> "수"
        DayOfWeek.THURSDAY -> "목"
        DayOfWeek.FRIDAY -> "금"
        DayOfWeek.SATURDAY -> "토"
        DayOfWeek.SUNDAY -> "일"
    }
