package com.avito.report.model

import com.avito.android.test.annotations.TestCaseBehavior
import com.avito.android.test.annotations.TestCasePriority

fun SimpleRunTest.Companion.createStubInstance(
    id: String = "1234",
    reportId: String = "12345",
    name: String = "com.Test.test",
    testCaseId: Int? = null,
    deviceName: String = "api22",
    tcBuild: String = "12345",
    groupList: List<String> = emptyList(),
    status: Status = Status.Success,
    stability: Stability = Stability.Stable,
    skipReason: String? = null,
    isFinished: Boolean = false,
    lastAttemptDurationInSeconds: Int = 123,
    externalId: String? = null,
    description: String? = null,
    startTime: Long = 0,
    endTime: Long = 0,
    dataSetNumber: Int? = null,
    features: List<String> = emptyList(),
    tagIds: List<Int> = emptyList(),
    priority: TestCasePriority? = null,
    behavior: TestCaseBehavior? = null,
    kind: Kind = Kind.E2E
) = SimpleRunTest(
    id = id,
    reportId = reportId,
    name = name,
    className = name.substringBeforeLast('.'),
    methodName = name.substringAfterLast('.'),
    testCaseId = testCaseId,
    deviceName = deviceName,
    groupList = groupList,
    status = status,
    stability = stability,
    skipReason = skipReason,
    buildId = tcBuild,
    isFinished = isFinished,
    lastAttemptDurationInSeconds = lastAttemptDurationInSeconds,
    externalId = externalId,
    description = description,
    startTime = startTime,
    endTime = endTime,
    dataSetNumber = dataSetNumber,
    features = features,
    tagIds = tagIds,
    priority = priority,
    behavior = behavior,
    kind = kind
)
