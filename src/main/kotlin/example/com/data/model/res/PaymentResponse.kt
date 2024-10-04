package example.com.data.model.res

import kotlinx.serialization.Serializable

@Serializable
data class PaymentResponse(
    val link: String,
    val amount: Long,
)
