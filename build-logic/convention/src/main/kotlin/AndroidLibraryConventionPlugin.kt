import com.android.build.api.dsl.LibraryExtension
import org.neteinstein.perfsentient.buildlogic.configureKotlinAndroid
import org.neteinstein.perfsentient.buildlogic.configureQuality
import org.neteinstein.perfsentient.buildlogic.lib
import org.neteinstein.perfsentient.buildlogic.libs
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
