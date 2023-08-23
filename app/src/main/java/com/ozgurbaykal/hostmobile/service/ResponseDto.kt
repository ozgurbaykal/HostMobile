package com.ozgurbaykal.hostmobile.service

import kotlinx.serialization.Serializable

@Serializable
data class ResponseDto(val password_status: Boolean, val token: String? = null)
