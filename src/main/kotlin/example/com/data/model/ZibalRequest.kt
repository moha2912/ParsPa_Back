package example.com.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ZibalRequest(
    val amount: Long,
    val callbackUrl: String,
    val description: String,
    val merchant: String,
    val mobile: String,
)

@Serializable
data class ZibalRequestStatus(
    val message: String,
    val result: Int,
    val trackId: Long
)