package org.neteinstein.perfsentient.core.model

import java.time.LocalDate

/** How Android says an app arrived on the phone (InstallSourceInfo.packageSource, Android 13+). */
enum class PackageSource {
    /** Installed by an app store. */
    STORE,

    /** Installed from a file downloaded from the internet (browser, messaging app). */
    DOWNLOADED_FILE,

    /** Installed from a file already on the phone (file manager, transfer). */
    LOCAL_FILE,

    /** Another known source (e.g. a device-to-device transfer). */
    OTHER,

    /** Not reported: Android 12L, or the installer did not say. */
    UNKNOWN,
}

/** Runtime permissions worth knowing about when judging scam and fraud risk. */
enum class SensitivePermission(
    /** Lets an app read one-time codes, calls or messages, or act on them: what fraud apps are after. */
    val isFraudRelevant: Boolean,
) {
    READ_SMS(isFraudRelevant = true),
    RECEIVE_SMS(isFraudRelevant = true),
    SEND_SMS(isFraudRelevant = true),
    READ_CALL_LOG(isFraudRelevant = true),
    CALL_PHONE(isFraudRelevant = false),
    ANSWER_PHONE_CALLS(isFraudRelevant = true),
    READ_CONTACTS(isFraudRelevant = false),
    RECORD_AUDIO(isFraudRelevant = false),
    CAMERA(isFraudRelevant = false),
    FINE_LOCATION(isFraudRelevant = false),
    USE_FULL_SCREEN_INTENT(isFraudRelevant = false),
}

/** Device-wide security settings that make scams and theft easier. */
data class DeviceSecurityPosture(
    /** A PIN, pattern, password or biometric protects the phone. */
    val hasSecureLock: Boolean,
    /** Android security patch level, when the build reports a valid one. */
    val securityPatch: LocalDate?,
    /** USB debugging (adb) is on. */
    val adbEnabled: Boolean,
) {
    companion object {
        /** Nothing known: raises no issue. */
        val UNKNOWN = DeviceSecurityPosture(hasSecureLock = true, securityPatch = null, adbEnabled = false)
    }
}

/**
 * Where the last look at installed-app changes stopped. The sequence comes from
 * PackageManager.getChangedPackages and restarts at every boot, hence [bootCount].
 */
data class PackageChangeCursor(val bootCount: Int, val sequence: Int)

/** Apps installed or updated since a [PackageChangeCursor], and the cursor to use next time. */
data class PackageChanges(val cursor: PackageChangeCursor, val packages: Set<String>)

/** Apps holding the accesses fraud apps want, read in one cheap pass. */
data class CapabilityHolders(
    val accessibility: Set<String>,
    val deviceAdmins: Set<String>,
    val notificationListeners: Set<String>,
    val defaultSms: String?,
) {
    /** Packages that hold an access now that they did not hold in [previous]. */
    fun newlyGrantedSince(previous: CapabilityHolders): Set<String> = buildSet {
        addAll(accessibility - previous.accessibility)
        addAll(deviceAdmins - previous.deviceAdmins)
        addAll(notificationListeners - previous.notificationListeners)
        if (defaultSms != null && defaultSms != previous.defaultSms) add(defaultSms)
    }

    companion object {
        val NONE = CapabilityHolders(emptySet(), emptySet(), emptySet(), defaultSms = null)
    }
}

/** What the live security check remembers between runs. */
data class SecurityWatchState(
    val cursor: PackageChangeCursor,
    val holders: CapabilityHolders,
    /** "package:BEHAVIOR" keys already alerted, oldest first, so an alert is never repeated. */
    val alerted: List<String>,
)
