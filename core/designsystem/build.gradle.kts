plugins {
    alias(libs.plugins.perfsentience.android.library)
    alias(libs.plugins.perfsentience.android.compose)
}

android {
    namespace = "com.neteinstein.perfsentience.core.designsystem"
}

dependencies {
    api(projects.core.model)
    implementation(libs.androidx.core.ktx)
}
