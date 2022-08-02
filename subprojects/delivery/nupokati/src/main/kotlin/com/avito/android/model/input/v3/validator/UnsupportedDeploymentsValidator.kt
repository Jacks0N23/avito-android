package com.avito.android.model.input.v3.validator

import com.avito.android.model.input.CdBuildConfigV3

internal class UnsupportedDeploymentsValidator : CdBuildConfigValidator {

    override fun validate(config: CdBuildConfigV3) {
        val unknownDeployments = config.deployments.filterIsInstance<CdBuildConfigV3.Deployment.Unknown>()
        require(unknownDeployments.isEmpty()) {
            "Unknown deployment types: $unknownDeployments"
        }
    }
}
