package com.avito.android.model.input.v3

import com.avito.android.model.input.CdBuildConfigV3
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

internal object DeploymentDeserializer : JsonDeserializer<CdBuildConfigV3.Deployment> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): CdBuildConfigV3.Deployment {
        return when (val type = json.asJsonObject.get("type").asString) {
            "app-binary" -> context.deserialize<CdBuildConfigV3.Deployment.AppBinary>(
                json,
                CdBuildConfigV3.Deployment.AppBinary::class.java
            )
            "qapps" ->
                context.deserialize<CdBuildConfigV3.Deployment.Qapps>(
                    json,
                    CdBuildConfigV3.Deployment.Qapps::class.java
                )
            "artifact" -> context.deserialize<CdBuildConfigV3.Deployment.Artifact>(
                json,
                CdBuildConfigV3.Deployment.Artifact::class.java
            )
            else -> CdBuildConfigV3.Deployment.Unknown(type)
        }
    }
}
