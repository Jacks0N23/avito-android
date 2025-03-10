package com.avito.test.summary

import com.avito.logger.LoggerFactory
import com.avito.notification.NotificationClient
import com.avito.notification.finder.ConjunctionPredicate
import com.avito.notification.finder.SameAuthorPredicate
import com.avito.notification.finder.TextContainsStringPredicate
import com.avito.report.model.Team
import com.avito.report.model.TestStatus
import com.avito.reportviewer.ReportsApi
import com.avito.reportviewer.model.ReportCoordinates
import com.avito.reportviewer.model.SimpleRunTest
import com.avito.slack.CoroutinesSlackBulkSender
import com.avito.slack.SlackBulkSender
import com.avito.slack.SlackConditionalSender
import com.avito.slack.SlackMessageUpdater
import com.avito.slack.SlackMessageUpdaterWithThreadMark
import com.avito.slack.model.SlackChannel
import com.avito.slack.model.SlackSendMessageRequest
import com.avito.test.summary.compose.SlackSummaryComposer
import com.avito.test.summary.compose.SlackSummaryComposerImpl
import com.avito.test.summary.model.CrossDeviceRunTest
import com.avito.test.summary.model.CrossDeviceStatus
import com.avito.test.summary.model.CrossDeviceSuite
import com.avito.test.summary.model.FailureOnDevice

internal interface TestSummarySender {
    fun send()
}

internal class TestSummarySenderImpl(
    notificationClient: NotificationClient,
    reportViewerUrl: String,
    private val reportsApi: ReportsApi,
    private val buildUrl: String,
    private val reportCoordinates: ReportCoordinates,
    private val globalSummaryChannel: SlackChannel,
    private val unitToChannelMapping: Map<Team, SlackChannel>,
    private val mentionOnFailures: Set<Team>,
    private val slackUserName: String,
    private val loggerFactory: LoggerFactory
) : TestSummarySender {

    private val slackSummaryComposer: SlackSummaryComposer = SlackSummaryComposerImpl(reportViewerUrl)
    private val slackMessageUpdater: SlackMessageUpdater = SlackMessageUpdaterWithThreadMark(
        notificationClient = notificationClient,
        threadMessage = "Updated by: $buildUrl"
    )
    private val slackConditionalSender: SlackConditionalSender = SlackConditionalSender(
        notificationClient = notificationClient,
        updater = slackMessageUpdater,
        condition = ConjunctionPredicate(
            listOf(
                SameAuthorPredicate(slackUserName),
                TextContainsStringPredicate(reportCoordinates.runId)
            )
        )
    )
    private val slackBulkSender: SlackBulkSender = CoroutinesSlackBulkSender(
        sender = slackConditionalSender,
        loggerFactory = loggerFactory,
    )

    private val slackEmojiProvider = SlackEmojiProvider()

    override fun send() {
        reportsApi.getTestsForRunId(reportCoordinates)
            .map { toCrossDeviceTestData(it) }
            .fold(
                { suite -> send(suite, requireNotNull(reportsApi.tryGetId(reportCoordinates))) },
                {
                    // TODO handle throwable
                }
            )
    }

    private fun send(suite: CrossDeviceSuite, reportId: String) {
        slackBulkSender.sendBulk {
            unitToChannelMapping.entries.map { (team, channel) ->
                val unitSuite = suite.filterTeam(team)

                if (unitSuite.crossDeviceRuns.isNotEmpty()) {
                    slackSummaryComposer.composeMessage(
                        testData = unitSuite,
                        team = team,
                        mentionOnFailures = mentionOnFailures.contains(team),
                        reportCoordinates = reportCoordinates,
                        reportId = reportId,
                        buildUrl = buildUrl
                    ).onSuccess { message ->
                        sendMessage(
                            SlackSendMessageRequest(
                                channel = channel,
                                text = message,
                                author = slackUserName,
                                emoji = slackEmojiProvider.emojiName(unitSuite.percentSuccessOfAutomated.roundToInt())
                            )
                        )
                    }.onFailure {
                        // TODO handle throwable
                    }
                }
            }

            slackSummaryComposer.composeMessage(
                testData = suite,
                team = Team.UNDEFINED,
                mentionOnFailures = false,
                reportCoordinates = reportCoordinates,
                reportId = reportId,
                buildUrl = buildUrl
            ).onSuccess {
                sendMessage(
                    SlackSendMessageRequest(
                        channel = globalSummaryChannel,
                        text = it,
                        author = slackUserName,
                        emoji = slackEmojiProvider.emojiName(suite.percentSuccessOfAutomated.roundToInt())
                    )
                )
            }.onFailure {
                // TODO handle throwable
            }
        }
    }

    private fun ReportsApi.tryGetId(reportCoordinates: ReportCoordinates): String? {
        return getReport(reportCoordinates).fold(
            onSuccess = { report ->
                report.id
            },
            onFailure = {
                // TODO handle throwable
                null
            }
        )
    }

    private fun toCrossDeviceTestData(testData: List<SimpleRunTest>): CrossDeviceSuite {
        return testData
            .groupBy { it.name }
            .map { (testName, runs) ->
                val status: CrossDeviceStatus = when {
                    runs.any { it.status is TestStatus.Lost } ->
                        CrossDeviceStatus.LostOnSomeDevices

                    runs.all { it.status is TestStatus.Skipped } ->
                        CrossDeviceStatus.SkippedOnAllDevices

                    runs.all { it.status is TestStatus.Manual } ->
                        CrossDeviceStatus.Manual

                    /**
                     * Успешным прогоном является при соблюдении 2 условий:
                     *  - Все тесты прошли (имеют Success статус)
                     *  - Есть пропущенные тесты (скипнули на каком-то SDK например),
                     *    но все остальные являются успешными (как минимум 1)
                     */
                    runs.any { it.status is TestStatus.Success } &&
                        runs.all { it.status is TestStatus.Success || it.status is TestStatus.Skipped } ->
                        CrossDeviceStatus.Success

                    runs.all { it.status is TestStatus.Failure } ->
                        CrossDeviceStatus.FailedOnAllDevices(runs.deviceFailures())

                    runs.any { it.status is TestStatus.Failure } ->
                        CrossDeviceStatus.FailedOnSomeDevices(runs.deviceFailures())

                    else ->
                        CrossDeviceStatus.Inconsistent
                }
                CrossDeviceRunTest(testName, status)
            }
            .let { CrossDeviceSuite(it) }
    }

    private fun List<SimpleRunTest>.deviceFailures(): List<FailureOnDevice> {
        return this.filter { it.status is TestStatus.Failure }
            .map {
                FailureOnDevice(it.deviceName, (it.status as TestStatus.Failure).verdict)
            }
    }
}
