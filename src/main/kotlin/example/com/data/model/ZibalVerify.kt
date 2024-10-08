package example.com.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ZibalVerify(
    val merchant: String,
    val trackId: Long
)

@Serializable
data class ZibalVerifyResponse(
    val amount: Long? = null,
    val cardNumber: String? = null,
    val description: String? = null,
    val message: String,
    val orderId: String? = null,
    val multiplexingInfos: List<String>? = null,
    val paidAt: String? = null,
    val refNumber: Long? = null,
    val result: Int,
    val status: Int? = null,
)