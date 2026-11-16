plugins {
    alias(libs.plugins.persentient.android.feature)
}

android {
    namespace = "com.neteinstein.persentient.feature.monitoring"
}

dependencies {
    implementation(libs.androidx.work.runtime)
    implementation(libs.koin.androidx.workmanager)

    testImplementation(libs.androidx.work.testing)
}
