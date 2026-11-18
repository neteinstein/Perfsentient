import com.neteinstein.perfsentience.buildlogic.lib
import com.neteinstein.perfsentience.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * A feature module = Android library + Compose + Koin + the shared core layers.
 * Features never depend on each other; they only talk through core:domain.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("perfsentience.android.library")
            pluginManager.apply("perfsentience.android.compose")

            dependencies {
                add("implementation", project(":core:common"))
                add("implementation", project(":core:model"))
                add("implementation", project(":core:domain"))
                add("implementation", project(":core:designsystem"))

                add("implementation", libs.lib("androidx-core-ktx"))
                add("implementation", libs.lib("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.lib("androidx-lifecycle-viewmodel-compose"))
                add("implementation", libs.lib("kotlinx-coroutines-android"))

                add("implementation", platform(libs.lib("koin-bom")))
                add("implementation", libs.lib("koin-android"))
                add("implementation", libs.lib("koin-androidx-compose"))
                add("testImplementation", project(":core:testing"))
                add("testImplementation", platform(libs.lib("koin-bom")))
                add("testImplementation", libs.lib("koin-test"))
                add("testImplementation", libs.lib("koin-test-junit4"))
            }
        }
    }
}
