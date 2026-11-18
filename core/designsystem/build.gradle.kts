plugins {
    alias(libs.plugins.perfsentience.android.library)
    alias(libs.plugins.perfsentience.android.compose)
}

android {
    namespace = "org.neteinstein.perfsentient.core.designsystem"
}

dependencies {
    api(projects.core.model)
    implementation(libs.androidx.core.ktx)
}
