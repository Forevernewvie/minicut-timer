package com.minicut.timer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.minicut.timer.domain.rules.MiniCutRules
import com.minicut.timer.ui.calendar.CalendarScreen
import com.minicut.timer.ui.components.AdMobBanner
import com.minicut.timer.ui.home.HomeScreen
import com.minicut.timer.ui.navigation.Destination
import com.minicut.timer.ui.plan.PlanScreen

@Composable
fun MiniCutRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column(
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    tonalElevation = 1.dp,
                    shadowElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.10f),
                    ),
                ) {
                    AdMobBanner(
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Surface(
                    shape = RoundedCornerShape(30.dp),
                    tonalElevation = 2.dp,
                    shadowElevation = 16.dp,
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                    ),
                ) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        Destination.entries.forEach { destination ->
                            val selected =
                                currentDestination?.hierarchy?.any { routeDestination ->
                                    val route = routeDestination.route
                                    route == destination.route || route?.startsWith("${destination.route}?") == true
                                } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        launchSingleTop = true
                                        restoreState = true
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                                alwaysShowLabel = true,
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.label,
                                    )
                                },
                                label = {
                                    Text(
                                        destination.label,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Destination.Home.route) {
                HomeScreen(
                    onOpenPlan = { suggestedTargetKcal ->
                        val route =
                            suggestedTargetKcal?.takeIf { it in MiniCutRules.TARGET_OPTIONS_KCAL }
                                ?.let { Destination.planRouteWithSuggestedTarget(it) }
                                ?: Destination.Plan.route
                        navController.navigate(route)
                    },
                )
            }
            composable(Destination.Calendar.route) {
                CalendarScreen()
            }
            composable(
                route = Destination.PLAN_ROUTE_PATTERN,
                arguments =
                    listOf(
                        navArgument(Destination.PLAN_SUGGESTED_TARGET_ARG) {
                            type = NavType.IntType
                            defaultValue = Destination.PLAN_SUGGESTED_TARGET_NONE
                        },
                    ),
            ) { backStackEntry ->
                val suggestedTarget =
                    backStackEntry.arguments
                        ?.getInt(Destination.PLAN_SUGGESTED_TARGET_ARG)
                        ?.takeIf { it != Destination.PLAN_SUGGESTED_TARGET_NONE }
                PlanScreen(
                    onSaved = { navController.navigate(Destination.Home.route) },
                    suggestedTargetKcal = suggestedTarget,
                )
            }
        }
    }
}
