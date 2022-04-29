plugins {
    id("convention.kotlin-jvm")
    id("convention.publish-kotlin-library")
    kotlin("kapt") // TODO replace with ksp
}

dependencies {
    api(projects.subprojects.emcee.queueClientModels)
    api(libs.moshiAdapters) {
        "used because EmceeConfig serialized to JSON for testing. Replace as soon as possible"
    }

    implementation(projects.subprojects.common.httpClient)
    implementation(projects.subprojects.common.okhttp)
    implementation(projects.subprojects.emcee.queueClientApi)
    implementation(projects.subprojects.logger.slf4jGradleLogger)
    implementation(projects.subprojects.testRunner.instrumentationTestsDexLoader)
    implementation(projects.subprojects.testRunner.testAnnotations)
    implementation(libs.coroutinesCore)
    implementation(libs.okhttp)
    implementation(libs.okhttpLogging)

    kapt(libs.moshiKapt)
}
