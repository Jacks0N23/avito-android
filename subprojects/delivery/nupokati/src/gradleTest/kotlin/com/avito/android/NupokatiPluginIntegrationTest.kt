package com.avito.android

import com.avito.http.HttpCodes
import com.avito.test.gradle.TestProjectGenerator
import com.avito.test.gradle.git
import com.avito.test.gradle.gradlew
import com.avito.test.gradle.module.AndroidAppModule
import com.avito.test.gradle.plugin.plugins
import com.avito.test.http.Mock
import com.avito.test.http.MockDispatcher
import com.avito.test.http.MockWebServerFactory
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class NupokatiPluginIntegrationTest {

    private val mockDispatcher = MockDispatcher()

    private val mockWebServer = MockWebServerFactory.create().apply {
        dispatcher = mockDispatcher
    }

    @AfterEach
    fun cleanup() {
        mockWebServer.shutdown()
    }

    @Test
    fun `uploadCdBuildResult - json contains required data`(@TempDir projectDir: File) {

        val outputDescriptorPath =
            mockWebServer.url("/artifactory/mobile-releases/avito_android/118.0_2/release_info.json")

        val releaseVersion = "118.0"

        val cdConfig = """
            |{
            |  "schema_version": 2,
            |  "project": "avito",
            |  "release_version": "$releaseVersion",
            |  "output_descriptor": {
            |    "path": "$outputDescriptorPath",
            |    "skip_upload": false
            |  },
            |  "deployments": [
            |    {
            |      "type": "qapps",
            |      "is_release": true
            |    }
            |  ]
            |}""".trimMargin()

        val cdConfigFile = File(projectDir, "cd-config.json").also { it.writeText(cdConfig) }

        val mockWebServerUrl = mockWebServer.url("/")

        val reportViewerFrontendUrl = "http://stub.com"
        val planSlug = "AvitoAndroid"
        val jobSlug = "FunctionalTest"
        val runId = "someId"
        val teamcityUrl = "http://teamcity.ru"
        val versionCode = 122

        TestProjectGenerator(
            modules = listOf(
                AndroidAppModule(
                    name = "app",
                    plugins = plugins {
                        id("com.avito.android.nupokati")
                    },
                    versionCode = versionCode,
                    useKts = true,
                    imports = listOf(
                        "import com.avito.reportviewer.model.ReportCoordinates",
                    ),
                    buildGradleExtra = """
                        |android {
                        |    buildTypes {
                        |        getByName("release") {
                        |            isMinifyEnabled = true
                        |            proguardFile("proguard.pro")
                        |        }
                        |    }
                        |}
                        |
                        |nupokati {
                        |    cdBuildConfigFile.set(rootProject.file("${cdConfigFile.name}"))
                        |    teamcityBuildUrl.set("$teamcityUrl")
                        |
                        |    artifactory {
                        |        login.set("user")
                        |        password.set("12345")
                        |    }
                        |
                        |    reportViewer {
                        |        frontendUrl.set("$reportViewerFrontendUrl")
                        |        reportCoordinates.set(ReportCoordinates("$planSlug", "$jobSlug", "$runId"))
                        |    }
                        |}
                        |""".trimMargin()
                )
            )
        ).generateIn(projectDir)

        val branchName = "release_11"
        projectDir.git("checkout -b $branchName")
        val commit = projectDir.git("rev-parse HEAD").trim()

        mockDispatcher.registerMock(
            Mock(
                requestMatcher = {
                    method == "PUT"
                        && path == "/artifactory/mobile-releases/avito_android/118.0_2/app-release.aab"
                },
                response = MockResponse().setResponseCode(HttpCodes.OK)
            )
        )

        val contractJson = mockDispatcher.captureRequest(
            Mock(
                requestMatcher = {
                    method == "PUT" && path == "/artifactory/mobile-releases/avito_android/118.0_2/release_info.json"
                },
                response = MockResponse().setResponseCode(200)
            )
        )

        gradlew(
            projectDir,
            ":app:uploadCdBuildResultRelease",
            dryRun = false
        )
            .assertThat()
            .buildSuccessful()

        val expectedContract = """
        |{
        |  "schema_version": 2,
        |  "teamcity_build_url": "$teamcityUrl",
        |  "build_number": "$versionCode",
        |  "release_version": "$releaseVersion",
        |  "git_branch": {
        |    "name": "$branchName",
        |    "commit_hash": "$commit"
        |  },
        |  "test_results": {
        |    "report_url": "$reportViewerFrontendUrl/report/$planSlug/$jobSlug/$runId?q=eyJmaWx0ZXIiOnsic2tpcCI6MH19",
        |    "report_coordinates": {
        |      "plan_slug": "$planSlug",
        |      "job_slug": "$jobSlug",
        |      "run_id": "$runId"
        |    }
        |  },
        |  "artifacts": [
        |    {
        |      "type": "bundle",
        |      "name": "app-release.aab",
        |      "uri": "${mockWebServerUrl}artifactory/mobile-releases/avito_android/118.0_2/app-release.aab",
        |      "build_variant": "release"
        |    }
        |  ]
        |}
        |""".trimMargin()

        contractJson.checks.singleRequestCaptured().jsonEquals(expectedContract)
    }
}
