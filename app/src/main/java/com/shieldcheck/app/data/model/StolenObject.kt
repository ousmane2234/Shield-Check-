package com.shieldcheck.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StolenObject(
    val id: String,
    val imei: String,
    val status: String, // "vole", "retrouve", "archive"
    val owner_name: String,
    val owner_email: String,
    @SerialName("phone_model")
    val phoneModel: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    val description: String? = null
)