import com.android.build.api.dsl.LibraryExtension
import com.neteinstein.perfsentience.buildlogic.configureKotlinAndroid
import com.neteinstein.perfsentience.buildlogic.configureQuality
import com.neteinstein.perfsentience.buildlogic.lib
import com.neteinstein.perfsentience.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                defaultConfig.consumerProguardFiles("consumer-rules.pro")
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                lint.targetSdk = libs.findVersion("targetSdk").get().requiredVersion.toInt()
                testOptions.targetSdk = libs.findVersion("targetSdk").get().requiredVersion.toInt()
            }
            configureQuality()

            dependencies {
                add("testImplementation", libs.findBundle("unit-test").get())
                add("testImplementation", libs.lib("robolectric"))
            }
        }
    }
}
