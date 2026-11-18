// Top-level build file. Plugins are declared here (apply false) so that they share a single
// classloader with the convention plugins in build-logic.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.kover)
}

// Aggregated coverage report across every module: ./gradlew koverXmlReport koverHtmlReport
dependencies {
    kover(projects.app)
    kover(projects.core.common)
    kover(projects.core.model)
    kover(projects.core.domain)
    kover(projects.core.data)
    kover(projects.core.designsystem)
    kover(projects.feature.report)
    kover(projects.feature.apps)
    kover(projects.feature.parental)
    kover(projects.feature.settings)
    kover(projects.feature.monitoring)
}

kover {
    reports {
        filters {
            excludes {
                // Generated / framework glue that cannot be meaningfully unit tested.
                classes(
                    "*.BuildConfig",
                    "*.ComposableSingletons*",
                    "*_Impl*",
                    "*Database_Impl*",
                    "*.di.*",
                )
                annotatedBy(
                    "androidx.compose.runtime.Composable",
                    "androidx.compose.ui.tooling.preview.Preview",
                )
            }
        }
        verify {
            rule("Minimum line coverage") {
                minBound(50)
            }
        }
    }
}
