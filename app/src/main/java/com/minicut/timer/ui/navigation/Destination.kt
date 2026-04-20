package com.minicut.timer.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.ui.graphics.vector.ImageVector

enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Home("home", "홈", Icons.Outlined.Home),
    Coach("coach", "코칭", Icons.Outlined.FitnessCenter),
    Calendar("calendar", "달력", Icons.Outlined.CalendarMonth),
    Plan("plan", "플랜", Icons.Outlined.EditCalendar),

    ;

    companion object {
        const val PLAN_SUGGESTED_TARGET_ARG = "suggestedTarget"
        const val PLAN_SUGGESTED_TARGET_NONE = -1
        val PLAN_ROUTE_PATTERN = "${Plan.route}?$PLAN_SUGGESTED_TARGET_ARG={$PLAN_SUGGESTED_TARGET_ARG}"

        fun planRouteWithSuggestedTarget(suggestedTargetKcal: Int): String =
            "${Plan.route}?$PLAN_SUGGESTED_TARGET_ARG=$suggestedTargetKcal"
    }
}
