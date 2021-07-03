package com.avito.teamcity

import java.io.Serializable

public data class TeamcityCredentials(
    val url: String,
    val user: String,
    val password: String
) : Serializable
