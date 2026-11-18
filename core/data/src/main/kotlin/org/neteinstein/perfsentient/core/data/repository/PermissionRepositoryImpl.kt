package org.neteinstein.perfsentient.core.data.repository

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.neteinstein.perfsentient.core.domain.repository.PermissionRepository
import org.neteinstein.perfsentient.core.model.GrantMode
import org.neteinstein.perfsentient.core.model.PermissionState
import org.neteinstein.perfsentient.core.model.PermissionType

/** Reads grant state from the OS. All checks are cheap, synchronous binder calls. */
class PermissionRepositoryImpl(private val context: Context) : PermissionRepository {
    private val state = MutableStateFlow(read())

    override val permissions: StateFlow<List<PermissionState>> = state.asStateFlow()

    override fun refresh() {
        state.value = read()
    }

    private fun read(): List<PermissionState> = listOf(
        PermissionState(PermissionType.USAGE_ACCESS, hasUsageAccess(), isRequired = true, GrantMode.SYSTEM_SETTINGS),
        PermissionState(
            PermissionType.POST_NOTIFICATIONS,
            canPostNotifications(),
            isRequired = true,
            grantMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) GrantMode.RUNTIME_DIALOG else GrantMode.SYSTEM_SETTINGS,
        ),
        PermissionState(PermissionType.NOTIFICATION_ACCESS, hasNotificationAccess(), isRequired = true, GrantMode.SYSTEM_SETTINGS),
        PermissionState(PermissionType.APP_INVENTORY, isGranted = true, isRequired = true, GrantMode.INSTALL_TIME),
    )

    private fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)

        @Suppress("DEPRECATION") // Still the public way to check our own usage-access op.
        val mode = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }

    private fun canPostNotifications(): Boolean {
        val runtimeGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return runtimeGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun hasNotificationAccess(): Boolean =
        context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)
}
