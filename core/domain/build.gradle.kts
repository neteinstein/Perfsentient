plugins {
    alias(libs.plugins.perfsentience.jvm.library)
}

dependencies {
    api(projects.core.model)
    api(projects.core.common)
    api(libs.kotlinx.coroutines.core)
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)

    testImplementation(project.dependencies.platform(libs.koin.bom))
    testImplementation(libs.koin.test)
    testImplementation(projects.core.testing)
}

// The domain layer holds all detection heuristics: keep it (almost) fully covered.
kover {
    reports {
        verify {
            rule("Domain line coverage") {
                minBound(90)
            }
        }
    }
}
