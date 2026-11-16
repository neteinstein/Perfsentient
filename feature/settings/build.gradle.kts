plugins {
    alias(libs.plugins.persentient.android.feature)
}

android {
    namespace = "com.neteinstein.persentient.feature.settings"
}

dependencies {
    implementation(libs.androidx.activity.compose)
}
