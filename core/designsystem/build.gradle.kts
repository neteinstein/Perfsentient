plugins {
    alias(libs.plugins.persentient.android.library)
    alias(libs.plugins.persentient.android.compose)
}

android {
    namespace = "com.neteinstein.persentient.core.designsystem"
}

dependencies {
    api(projects.core.model)
    implementation(libs.androidx.core.ktx)
}
