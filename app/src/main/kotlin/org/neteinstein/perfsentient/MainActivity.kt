package org.neteinstein.perfsentient

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.neteinstein.perfsentient.core.designsystem.theme.PerfsentienceTheme
import org.neteinstein.perfsentient.core.designsystem.theme.isDark
import org.neteinstein.perfsentient.feature.monitoring.MonitoringContract
import org.neteinstein.perfsentient.ui.PerfsentienceAppUi

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { viewModel.uiState.value is MainUiState.Loading }
        if (savedInstanceState == null) handleDeepLink(intent)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) { viewModel.onAppResumed() }
        }

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
            val settings = (state as? MainUiState.Ready)?.settings ?: return@setContent
            val dark = settings.themeMode.isDark()

            // Edge-to-edge with transparent bars; icon contrast follows the in-app theme.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) {
                        dark
                    },
                )
                onDispose { }
            }

            PerfsentienceTheme(darkTheme = dark, dynamicColor = settings.useDynamicColor) {
                PerfsentienceAppUi(selectedTab = selectedTab, onTabSelected = viewModel::selectTab)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        if (intent?.getStringExtra(MonitoringContract.EXTRA_OPEN_TAB) == MonitoringContract.TAB_REPORT) {
            viewModel.selectTab(AppTab.REPORT)
        }
    }
}
