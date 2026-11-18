package org.neteinstein.perfsentient.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

/** Applies ktlint (formatting/style) and Kover (coverage) to every module. */
internal fun Project.configureQuality() {
    pluginManager.apply(libs.pluginId("ktlint"))
    pluginManager.apply(libs.pluginId("kover"))

    extensions.configure<KtlintExtension> {
        version.set(libs.version("ktlint"))
        android.set(true)
        outputToConsole.set(true)
        ignoreFailures.set(false)
        reporters {
            reporter(ReporterType.PLAIN)
            reporter(ReporterType.CHECKSTYLE)
        }
        filter {
            exclude { it.file.path.contains("/build/") }
        }
    }
}
