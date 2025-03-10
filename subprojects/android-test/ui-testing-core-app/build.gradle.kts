import com.avito.instrumentation.configuration.KubernetesViaCredentials
import com.avito.instrumentation.reservation.request.Device.CloudEmulator
import com.avito.kotlin.dsl.getMandatoryStringProperty
import com.avito.kotlin.dsl.getOptionalStringProperty

plugins {
    id("convention.kotlin-android-app")
    id("convention.shared-testing")
    id("com.avito.android.instrumentation-tests")
}

android {

    defaultConfig {
        versionName = "1.0"
        versionCode = 1
        testInstrumentationRunner = "com.avito.android.test.app.core.TestAppRunner"

        val instrumentationArgs = mapOf(
            "planSlug" to "AndroidTestApp",
            "jobSlug" to "FunctionalTests",
            "fileStorageUrl" to (getOptionalStringProperty("avito.fileStorage.url") ?: "http://stub"),
            "teamcityBuildId" to (getOptionalStringProperty("teamcityBuildId") ?: "0"),
        )

        // These arguments are updated in IDE configuration only after sync!
        testInstrumentationRunnerArguments.putAll(instrumentationArgs)
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"

        unitTests {
            isIncludeAndroidResources = true

            all {
                val args = mapOf(
                    "android.junit.runner" to "com.avito.robolectric.runner.InHouseRobolectricTestRunner",
                    "planSlug" to "AndroidTestApp",
                    "jobSlug" to "FunctionalTests",
                )
                it.systemProperties.putAll(args)
            }
        }
    }

    packagingOptions {
        resources.pickFirsts.add("META-INF/okhttp.kotlin_module")
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.playServicesBase)
    implementation(libs.recyclerView)
    implementation(libs.swipeRefreshLayout)

    implementation(projects.subprojects.androidLib.proxyToast)

    sharedTestImplementation(projects.subprojects.testRunner.testInhouseRunner)
    sharedTestImplementation(projects.subprojects.testRunner.testReportAndroid)
    sharedTestImplementation(projects.subprojects.testRunner.testAnnotations)
    sharedTestImplementation(projects.subprojects.testRunner.reportViewer)
    sharedTestImplementation(projects.subprojects.androidTest.uiTestingCore)
    sharedTestImplementation(projects.subprojects.androidTest.toastRule)
    sharedTestImplementation(projects.subprojects.common.truthExtensions)

    testImplementation(projects.subprojects.testRunner.robolectricInhouseRunner)

    androidTestUtil(libs.testOrchestrator)
}

val avitoRegistry = getOptionalStringProperty("avito.registry")

instrumentation {

    testReport {
        reportViewer {
            reportApiUrl = getOptionalStringProperty("avito.report.url") ?: "http://stub"
            reportViewerUrl = getOptionalStringProperty("avito.report.viewerUrl") ?: "http://stub"
            fileStorageUrl = getOptionalStringProperty("avito.fileStorage.url") ?: "http://stub"
        }
    }

    logcatTags = setOf(
        "UITestRunner:*",
        "ActivityManager:*",
        "ReportTestListener:*",
        "StorageJsonTransport:*",
        "TestReport:*",
        "VideoCaptureListener:*",
        "TestRunner:*",
        "SystemDialogsManager:*",
        "AndroidJUnitRunner:*",
        "ito.android.de:*", // по этому тэгу система пишет логи об использовании hidden/restricted api https://developer.android.com/distribute/best-practices/develop/restrictions-non-sdk-interfaces
        "*:E"
    )

    instrumentationParams = mapOf(
        "videoRecording" to "failed",
        "jobSlug" to "FunctionalTests"
    )

    filters.register("ci") {
        fromSource.excludeFlaky = true
    }

    environments {
        register<KubernetesViaCredentials>("kubernetes") {
            token.set(getMandatoryStringProperty("kubernetesToken"))
            caCertData.set(getMandatoryStringProperty("kubernetesCaCertData"))
            url.set(getMandatoryStringProperty("kubernetesUrl"))
            namespace.set(getMandatoryStringProperty("kubernetesNamespace"))
        }
    }

    experimental {
        useLegacyExtensionsV1Beta.set(false)
    }

    val emulator22 = CloudEmulator(
        name = "api22",
        api = 22,
        model = "Android_SDK_built_for_x86",
        image = emulatorImage(22, "5429ee25fd2b"),
        cpuCoresRequest = "1",
        cpuCoresLimit = "1.3",
        memoryLimit = "4Gi"
    )

    val emulator29 = CloudEmulator(
        name = "api29",
        api = 29,
        model = "Android_SDK_built_for_x86_64",
        image = emulatorImage(29, "e8ce9edfc78d"),
        cpuCoresRequest = "1",
        cpuCoresLimit = "1.3",
        memoryLimit = "4Gi"
    )

    configurations {
        register("ui") {
            reportSkippedTests = true
            filter = "ci"

            targets {
                register("api22") {
                    deviceName = "API22"

                    scheduling {
                        quota {
                            retryCount = 1
                            minimumSuccessCount = 1
                        }

                        testsCountBasedReservation {
                            device = emulator22
                            maximum = 50
                            minimum = 2
                            testsPerEmulator = 3
                        }
                    }
                }

                register("api29") {
                    deviceName = "API29"

                    scheduling {
                        quota {
                            retryCount = 1
                            minimumSuccessCount = 1
                        }

                        testsCountBasedReservation {
                            device = emulator29
                            maximum = 50
                            minimum = 2
                            testsPerEmulator = 3
                        }
                    }
                }
            }
        }
    }
}

fun emulatorImage(api: Int, label: String): String {
    return if (avitoRegistry != null) {
        "$avitoRegistry/android/emulator-$api:$label"
    } else {
        "avitotech/android-emulator-$api:$label"
    }
}

tasks.check {
    dependsOn(tasks.named("instrumentationUiKubernetes"))
}
