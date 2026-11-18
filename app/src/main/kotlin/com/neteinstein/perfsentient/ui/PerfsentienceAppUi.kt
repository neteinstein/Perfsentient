package com.neteinstein.perfsentient.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.neteinstein.perfsentient.AppTab
import com.neteinstein.perfsentient.R
import com.neteinstein.perfsentient.feature.apps.presentation.AppsRoute
import com.neteinstein.perfsentient.feature.report.presentation.ReportRoute
import com.neteinstein.perfsentient.feature.settings.presentation.SettingsRoute

private data class TabSpec(val tab: AppTab, val label: Int, val selectedIcon: ImageVector, val icon: ImageVector)

private val Tabs = listOf(
    TabSpec(AppTab.REPORT, R.string.tab_report, Icons.Rounded.Insights, Icons.Outlined.Insights),
    TabSpec(AppTab.APPS, R.string.tab_apps, Icons.Rounded.Apps, Icons.Outlined.Apps),
    TabSpec(AppTab.SETTINGS, R.string.tab_settings, Icons.Rounded.Settings, Icons.Outlined.Settings),
)

/**
 * Root UI. NavigationSuiteScaffold adapts automatically: bottom bar on phones, navigation rail
 * on tablets / foldables / landscape – required by the large-screen quality guidelines.
 */
@Composable
fun PerfsentienceAppUi(selectedTab: AppTab, onTabSelected: (AppTab) -> Unit, modifier: Modifier = Modifier) {
    NavigationSuiteScaffold(
        modifier = modifier,
        navigationSuiteItems = {
            Tabs.forEach { spec ->
                val selected = spec.tab == selectedTab
                item(
                    selected = selected,
                    onClick = { onTabSelected(spec.tab) },
                    icon = { Icon(if (selected) spec.selectedIcon else spec.icon, contentDescription = null) },
                    label = { Text(stringResource(spec.label)) },
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        // Surface sets both background and LocalContentColor so text adapts to light/dark.
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    val enter = slideInHorizontally(tween(380)) { if (forward) it / 4 else -it / 4 } +
                        fadeIn(tween(380)) + scaleIn(tween(380), initialScale = 0.96f)
                    val exit = slideOutHorizontally(tween(300)) { if (forward) -it / 4 else it / 4 } + fadeOut(tween(220))
                    enter togetherWith exit
                },
                label = "tabs",
            ) { tab ->
                when (tab) {
                    AppTab.REPORT -> ReportRoute(onOpenSettings = { onTabSelected(AppTab.SETTINGS) })
                    AppTab.APPS -> AppsRoute()
                    AppTab.SETTINGS -> SettingsRoute()
                }
            }
        }
    }
}
