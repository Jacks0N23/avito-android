package com.avito.android.model.input

import com.google.gson.Gson
import org.gradle.api.Transformer
import org.gradle.api.file.RegularFile
import java.io.File

internal class CdBuildConfigParserFactory(private val gson: Gson) : Transformer<CdBuildConfig, RegularFile> {

    override fun transform(configFilePath: RegularFile): CdBuildConfig {
        val actualParser = when (val version = parseSchemaVersion(configFilePath.asFile)) {
            2L -> com.avito.android.model.input.v2.CdBuildConfigParser(gson)
            3L -> com.avito.android.model.input.v3.CdBuildConfigParser(gson)
            else -> throw IllegalArgumentException("Unsupported schema version: $version")
        }

        return actualParser.transform(configFilePath)
    }

    private fun parseSchemaVersion(configFilePath: File): Long {
        return gson.fromJson(configFilePath.reader(), SchemaOnly::class.java).schemaVersion
    }

    private data class SchemaOnly(val schemaVersion: Long)
}
