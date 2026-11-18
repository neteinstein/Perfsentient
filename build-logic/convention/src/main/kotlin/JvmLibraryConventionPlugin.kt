import com.neteinstein.perfsentient.buildlogic.configureKotlinJvm
import com.neteinstein.perfsentient.buildlogic.configureQuality
import com.neteinstein.perfsentient.buildlogic.lib
import com.neteinstein.perfsentient.buildlogic.libs
import com.neteinstein.perfsentient.buildlogic.pluginId
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
