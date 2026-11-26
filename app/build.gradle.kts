import java.util.Properties

plugins {
    alias(libs.plugins.perfsentience.android.application)
    alias(libs.plugins.perfsentience.android.compose)
}

// Release signing: CI passes the keystore through env vars (see .github/workflows/release.yml);
// locally an untracked keystore.properties can be used. Without either, release builds are
// unsigned (the PR pipeline only checks that R8 succeeds).
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use(keystoreProperties::load)
}
val releaseKeystoreFilePath =
    (System.getenv("KEYSTORE_FILE") ?: keystoreProperties.getProperty("storeFile"))?.takeIf { it.isNotBlank() }

// The versionCode is derived from versionName so every release gets a unique, increasing code:
// major.minor.patch -> MMmmmppp00 + build, e.g. 1.0.3 -> 10000300. `build` (0-99,
// -PVERSION_CODE_BUILD) is only for re-uploading the same version to Play, which rejects a
// reused versionCode.
val versionCodeBuild = (project.findProperty("VERSION_CODE_BUILD") as String?)?.toInt() ?: 0

fun versionCodeFor(
    versionName: String,
    build: Int,
): Int {
    val (major, minor, patch) = versionName.split('.').map(String::toInt)
    require(minor < 100 && patch < 1000 && build in 0..99) { "Version $versionName+$build does not fit the versionCode scheme" }
    return major * 10_000_000 + minor * 100_000 + patch * 100 + build
}

android {
    namespace = "org.neteinstein.perfsentient"

    defaultConfig {
        applicationId = "org.neteinstein.perfsentient"
        // release.yml bumps the patch number in this line on every merge to main.
        versionName = "1.0.16"
        versionCode = versionCodeFor(versionName!!, versionCodeBuild)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (releaseKeystoreFilePath != null) {
                storeFile = file(releaseKeystoreFilePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: keystoreProperties.getProperty("storePassword")
                keyAlias = System.getenv("KEY_ALIAS") ?: keystoreProperties.getProperty("keyAlias")
                keyPassword = System.getenv("KEY_PASSWORD") ?: keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            // R8: shrink, optimise and obfuscate code + strip unused resources.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (releaseKeystoreFilePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    // Lists the translated languages (values-*) in a generated locale config, so Android 13+
    // offers them under Settings > Apps > Perfsentient > Language. English is the default.
    androidResources {
        generateLocaleConfig = true
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "DebugProbesKt.bin", "kotlin-tooling-metadata.json")
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.model)
    implementation(projects.core.domain)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.feature.report)
    implementation(projects.feature.apps)
    implementation(projects.feature.parental)
    implementation(projects.feature.settings)
    implementation(projects.feature.monitoring)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.compose.material3.adaptive.navigation.suite)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.androidx.workmanager)

    testImplementation(libs.bundles.unit.test)
    testImplementation(platform(libs.koin.bom))
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit4)
    testImplementation(libs.androidx.work.runtime)
    testImplementation(projects.core.testing)

    // StoreScreenshotsTest renders the real UI with Robolectric (native graphics) for the README / Play listing.
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
