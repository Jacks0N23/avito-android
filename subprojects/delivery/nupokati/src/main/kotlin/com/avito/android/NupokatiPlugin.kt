package com.avito.android

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.android.build.gradle.AppPlugin
import com.avito.android.agp.getVersionCode
import com.avito.android.artifactory_backup.ArtifactoryBackupTask
import com.avito.android.contract_upload.UploadCdBuildResultTask
import com.avito.android.model.CdBuildConfig
import com.avito.android.provider.CdBuildConfigTransformer
import com.avito.android.provider.StrictCdBuildConfigValidator
import com.avito.android.stats.statsdConfig
import com.avito.capitalize
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.specs.Spec
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

public class NupokatiPlugin : Plugin<Project> {

    public lateinit var cdBuildConfig: Provider<CdBuildConfig>

    override fun apply(project: Project) {

        val extension = project.extensions.create<NupokatiExtension>("nupokati")

        cdBuildConfig = extension.cdBuildConfigFile
            .map(CdBuildConfigTransformer(validator = StrictCdBuildConfigValidator()))

        project.plugins.withType<AppPlugin> {
            val androidComponents = project.extensions.getByType<ApplicationAndroidComponentsExtension>()

            val releaseVariantSelector = androidComponents.selector()
                .withName(extension.releaseBuildVariantName.convention(DEFAULT_RELEASE_VARIANT).get())

            val skipUploadSpec = Spec<Task> {
                val skipUpload = cdBuildConfig.get().outputDescriptor.skipUpload
                if (skipUpload) {
                    project.logger.lifecycle(
                        "Skip uploading artifacts and contract json, " +
                            "because skipUpload=true is called"
                    )
                }
                val shouldRunTask = !skipUpload
                shouldRunTask
            }

            androidComponents.onVariants(selector = releaseVariantSelector) { variant: ApplicationVariant ->

                val variantSlug = variant.name.capitalize()

                val bundle: Provider<RegularFile> = variant.artifacts.get(type = SingleArtifact.BUNDLE)

                val publishArtifactsTask =
                    project.tasks.register<ArtifactoryBackupTask>("artifactoryBackup$variantSlug") {
                        group = CD_TASK_GROUP
                        description = "Backup ${variant.name} artifacts in artifactory bucket"

                        this.artifactoryUser.set(extension.artifactory.login)
                        this.artifactoryPassword.set(extension.artifactory.password)
                        this.artifactoryUploadPath.set(
                            cdBuildConfig.map {
                                it.outputDescriptor.path.substringBeforeLast('/')
                            }
                        )
                        this.buildVariant.set(variant.name)
                        this.files.set(project.files(bundle))
                        this.statsDConfig.set(project.statsdConfig)
                        this.buildOutput.set(project.layout.buildDirectory.file("nupokati/buildOutput.json"))

                        onlyIf(skipUploadSpec)
                    }

                project.tasks.register<UploadCdBuildResultTask>(uploadCdBuildResultTaskName(variantSlug)) {
                    group = CD_TASK_GROUP
                    description = "Send build result to Nupokati service"

                    this.artifactoryUser.set(extension.artifactory.login)
                    this.artifactoryPassword.set(extension.artifactory.password)
                    this.reportViewerUrl.set(extension.reportViewer.frontendUrl)
                    this.reportCoordinates.set(extension.reportViewer.reportCoordinates)
                    this.teamcityBuildUrl.set(extension.teamcityBuildUrl)
                    this.cdBuildConfig.set(this@NupokatiPlugin.cdBuildConfig)
                    this.appVersionCode.set(variant.getVersionCode())
                    this.buildOutputFileProperty.set(publishArtifactsTask.flatMap { it.buildOutput })
                    this.statsDConfig.set(project.statsdConfig)

                    // todo depend on output with actually uploaded artifacts
                    dependsOn(publishArtifactsTask)

                    onlyIf(skipUploadSpec)
                }
            }
        }
    }
}
