import com.android.build.api.dsl.CommonExtension
import com.neteinstein.persentient.buildlogic.lib
import com.neteinstein.persentient.buildlogic.libs
import com.neteinstein.persentient.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/** Enables Jetpack Compose (compiler plugin + BOM) on an Android module. */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("kotlin-compose"))

            extensions.getByType(CommonExtension::class.java).apply {
                buildFeatures.compose = true
            }

            dependencies {
                val bom = libs.lib("compose-bom")
                add("implementation", platform(bom))
                add("testImplementation", platform(bom))
                add("implementation", libs.lib("compose-ui"))
                add("implementation", libs.lib("compose-ui-graphics"))
                add("implementation", libs.lib("compose-ui-tooling-preview"))
                add("implementation", libs.lib("compose-foundation"))
                add("implementation", libs.lib("compose-animation"))
                add("implementation", libs.lib("compose-material3"))
                add("implementation", libs.lib("compose-material-icons-extended"))
                add("debugImplementation", libs.lib("compose-ui-tooling"))
                add("debugImplementation", libs.lib("compose-ui-test-manifest"))
                add("testImplementation", libs.lib("compose-ui-test-junit4"))
            }
        }
    }
}
