plugins {
    alias(libs.plugins.perfsentience.android.feature)
}

android {
    namespace = "com.neteinstein.perfsentient.feature.settings"
}

dependencies {
    implementation(libs.androidx.activity.compose)
}
