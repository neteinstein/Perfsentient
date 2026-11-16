plugins {
    alias(libs.plugins.persentient.jvm.library)
}

// Shared test doubles (fakes, rules, builders) used by every module's unit tests.
dependencies {
    api(projects.core.domain)
    api(projects.core.model)
    api(projects.core.common)
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
}
