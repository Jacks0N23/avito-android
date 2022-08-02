package com.avito.android.model.input

public data class CdBuildConfigV3(
    override val schemaVersion: Long,
    val project: NupokatiProject,
    override val outputDescriptor: OutputDescriptor,
    override val releaseVersion: String,
    val deployments: List<Deployment>
) : CdBuildConfig {

    public sealed class Deployment {

        public data class AppBinary(
            val store: String,
            val fileType: AndroidArtifactType,
            val buildConfiguration: String
        ) : Deployment()

        public data class Artifact(
            val kind: String,
            val fileType: String
        ) : Deployment()

        /**
         * @param isRelease Send artifacts as release versions. Non-release artifacts are stored for a limited time.
         */
        public data class Qapps(val isRelease: Boolean) : Deployment()

        public data class Unknown(val type: String) : Deployment()
    }
}
