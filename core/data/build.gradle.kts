plugins {
    alias(libs.plugins.perfsentience.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "org.neteinstein.perfsentient.core.data"
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    api(projects.core.domain)
    implementation(projects.core.common)
    implementation(projects.core.model)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)

    testImplementation(platform(libs.koin.bom))
    testImplementation(libs.koin.test)
    testImplementation(projects.core.testing)
}
