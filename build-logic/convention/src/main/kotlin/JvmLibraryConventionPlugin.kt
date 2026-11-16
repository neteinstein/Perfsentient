import com.neteinstein.persentient.buildlogic.configureKotlinJvm
import com.neteinstein.persentient.buildlogic.configureQuality
import com.neteinstein.persentient.buildlogic.lib
import com.neteinstein.persentient.buildlogic.libs
import com.neteinstein.persentient.buildlogic.pluginId
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
