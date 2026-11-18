import org.neteinstein.perfsentient.buildlogic.configureKotlinJvm
import org.neteinstein.perfsentient.buildlogic.configureQuality
import org.neteinstein.perfsentient.buildlogic.lib
import org.neteinstein.perfsentient.buildlogic.libs
import org.neteinstein.perfsentient.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Pure Kotlin module (no Android framework) – used for model, domain and common layers. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("kotlin-jvm"))
            configureKotlinJvm()
            configureQuality()

            dependencies {
                add("testImplementation", libs.findBundle("unit-test").get())
            }
        }
    }
}
