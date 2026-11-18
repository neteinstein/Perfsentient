plugins {
    alias(libs.plugins.perfsentience.android.feature)
}

android {
    namespace = "org.neteinstein.perfsentient.feature.monitoring"
}

dependencies {
    implementation(libs.androidx.work.runtime)
    implementation(libs.koin.androidx.workmanager)

    testImplementation(libs.androidx.work.testing)
}
