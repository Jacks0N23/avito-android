package com.avito.android.model.input

public data class CdBuildConfigV2(
    override val schemaVersion: Long,
    val project: NupokatiProject,
    override val outputDescriptor: OutputDescriptor,
    override val releaseVersion: String,
    val deployments: List<Deployment>
) : CdBuildConfig {

    public sealed class Deployment {

        public data class GooglePlay(
            val artifactType: AndroidArtifactType,
            // TODO: decouple logic and move the mapping on client side MBSA-636
            // Contract should show a declarative intention, but build variants are more implementation details.
            // They tend to change and it's no use to couple Nupokati and apps in this point.
            @Deprecated("Will be deleted. Try to avoid usage if possible.")
            val buildVariant: String,
            val track: Track
        ) : Deployment()

        public data class RuStore(
            val artifactType: AndroidArtifactType,
        ) : Deployment()

        /**
         * Deploy artifacts to QApps.
         */
        public data class Qapps(
            /**
             * Send artifacts as release versions.
             * Non-release artifacts are stored for a limited time.
             */
            val isRelease: Boolean
        ) : Deployment()

        public data class Unknown(val type: String) : Deployment()

        public enum class Track {
            alpha,
            internal,
            beta,
            production
        }
    }
}
