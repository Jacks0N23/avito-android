package com.avito.slack

import com.avito.android.Result
import com.avito.slack.model.SlackChannel
import java.io.File

public interface SlackFileUploader {

    public fun uploadHtml(
        channel: SlackChannel,
        message: String,
        file: File
    ): Result<Unit>
}
