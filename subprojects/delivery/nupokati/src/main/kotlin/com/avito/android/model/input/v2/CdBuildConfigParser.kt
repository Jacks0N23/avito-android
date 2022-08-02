package com.avito.android.model.input.v2

import com.avito.android.model.input.CdBuildConfigV2
import com.google.gson.Gson
import org.gradle.api.Transformer
import org.gradle.api.file.RegularFile
import java.io.File

internal class CdBuildConfigParser(
    private val gson: Gson,
    private val validator: CdBuildConfigValidator = StrictCdBuildConfigValidator()
) : Transformer<CdBuildConfigV2, RegularFile> {

    override fun transform(configFilePath: RegularFile): CdBuildConfigV2 {
        val configFile = configFilePath.asFile

        return deserializeToCdBuildConfig(configFile).also {
            validator.validate(it)
        }
    }

    private fun deserializeToCdBuildConfig(configFile: File): CdBuildConfigV2 {
        require(configFile.exists()) {
            "Can't find cd config file in $configFile"
        }
        return gson.fromJson(configFile.reader(), CdBuildConfigV2::class.java)
    }
}
