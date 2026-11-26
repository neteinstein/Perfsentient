package org.neteinstein.perfsentient.core.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class ReportTime(val hour: Int, val minute: Int) {
    init {
        require(hour in 0..23) { "hour must be in 0..23" }
        require(minute in 0..59) { "minute must be in 0..59" }
    }

    companion object {
        val DEFAULT = ReportTime(hour = 21, minute = 30)
    }
}

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = false,
    val reportTime: ReportTime = ReportTime.DEFAULT,
    val monitoringEnabled: Boolean = false,
    val parental: ParentalSettings = ParentalSettings(),
    /** Notify as soon as a risky app is installed or gets risky access, instead of waiting for the report. */
    val securityAlertsEnabled: Boolean = true,
)
