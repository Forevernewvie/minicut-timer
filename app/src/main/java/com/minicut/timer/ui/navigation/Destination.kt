package com.minicut.timer.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.ui.graphics.vector.ImageVector

enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Home("home", "홈", Icons.Outlined.Home),
    Calendar("calendar", "달력", Icons.Outlined.CalendarMonth),
    Plan("plan", "플랜", Icons.Outlined.EditCalendar),
}
