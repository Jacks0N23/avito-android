package com.avito.android.model.input.v2

import com.avito.android.model.input.CdBuildConfigV2
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

internal object DeploymentDeserializer : JsonDeserializer<CdBuildConfigV2.Deployment> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): CdBuildConfigV2.Deployment {
        return when (val type = json.asJsonObject.get("type").asString) {
            "google-play" ->
                context.deserialize<CdBuildConfigV2.Deployment.GooglePlay>(
                    json,
                    CdBuildConfigV2.Deployment.GooglePlay::class.java
                )
            "ru-store" ->
                context.deserialize<CdBuildConfigV2.Deployment.RuStore>(
                    json,
                    CdBuildConfigV2.Deployment.RuStore::class.java
                )
            "qapps" ->
                context.deserialize<CdBuildConfigV2.Deployment.Qapps>(
                    json,
                    CdBuildConfigV2.Deployment.Qapps::class.java
                )
            else -> CdBuildConfigV2.Deployment.Unknown(type)
        }
    }
}
