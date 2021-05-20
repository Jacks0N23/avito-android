package com.avito.instrumentation.internal.finalizer

import com.avito.logger.LoggerFactory
import com.avito.logger.create
import com.avito.report.model.AndroidTest
import com.avito.report.model.TestStaticData
import com.google.gson.Gson
import java.io.File

internal class FinalizerFileDumper(
    private val outputDir: File,
    loggerFactory: LoggerFactory
) {

    private val gson = Gson()

    private val logger = loggerFactory.create<FinalizerFileDumper>()

    fun dump(
        initialTestSuite: Set<TestStaticData>,
        testResults: Collection<AndroidTest>
    ) {
        try {
            val initialJson = gson.toJson(initialTestSuite)
            val resultsJson = gson.toJson(testResults)

            val verdictDir = File(outputDir, "verdict").apply { mkdirs() }

            File(verdictDir, "initial.json").writeText(initialJson)
            File(verdictDir, "results.json").writeText(resultsJson)
        } catch (e: Throwable) {
            logger.warn("Can't dump test suite finalize data", e)
        }
    }
}
