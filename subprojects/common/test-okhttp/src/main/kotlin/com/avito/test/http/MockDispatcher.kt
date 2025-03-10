package com.avito.test.http

import com.avito.android.Result
import com.avito.logger.PrintlnLoggerFactory
import com.avito.logger.create
import com.avito.utils.ResourcesReader
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.google.gson.JsonElement
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import java.util.Collections

public class MockDispatcher(
    private val unmockedResponse: MockResponse = MockResponse().setResponseCode(418).setBody("Not mocked"),
) : Dispatcher() {

    private val logger = PrintlnLoggerFactory.create<MockDispatcher>()

    private val mocks = Collections.synchronizedList(mutableListOf<Mock>())

    private val capturers = Collections.synchronizedList(mutableListOf<RequestCapturer>())

    public fun registerMock(mock: Mock) {
        mocks.add(mock)
    }

    public fun captureRequest(requestMatcher: RequestData.() -> Boolean): RequestCapturer {
        val capturer = RequestCapturer(requestMatcher)
        capturers.add(capturer)
        return capturer
    }

    public fun captureRequest(mock: Mock): RequestCapturer {
        val capturer = RequestCapturer(mock.requestMatcher)
        capturers.add(capturer)
        mocks.add(mock)
        return capturer
    }

    override fun dispatch(request: RecordedRequest): MockResponse {
        val requestData = RequestData(request)

        synchronized(capturers) {
            capturers.find { it.requestMatcher.invoke(requestData) }?.run {
                capture(request)
            }
        }

        /**
         * to be able to replace mocks (last one wins) (see MBS-5878)
         *
         * synchronized because of concurrent iterator access (see MBS-7636)
         */
        val matchedMock = synchronized(mocks) {
            mocks.findLast { it.requestMatcher.invoke(requestData) }
                ?.also { mock -> if (mock.removeAfterMatched) mocks.remove(mock) }
        }

        val response = if (matchedMock?.response != null) {
            logger.debug("Request matched: [$requestData], answering: ${matchedMock.response}")
            matchedMock.response
        } else {
            logger.warn("Unmocked request captured: [$requestData], answering: [$unmockedResponse]")
            unmockedResponse
        }

        return response
    }
}

private val gson = Gson()

/**
 * Use file contents as response body
 *
 * @param fileName specify file path, relative to assets dir
 *                 example: "assets/mock/seller_x/publish/parameters/ok.json"
 */
public fun MockResponse.setBodyFromFile(fileName: String): MockResponse {
    val text = ResourcesReader.readText(fileName)
    if (fileName.endsWith(".json")) {
        validateJson(text).onFailure {
            throw IllegalArgumentException("$fileName contains invalid json", it)
        }
    }
    setBody(text)
    return this
}

private fun validateJson(json: String): Result<Unit> = Result.tryCatch { gson.fromJson<JsonElement>(json) }
