package example.com.data.model.res

import example.com.routes.InsoleRequest
import kotlinx.serialization.Serializable

@Serializable
data class PaymentReviewResponse(
    val insole: InsoleRequest,
    val date: Long,
    val card: String,
    val total: Long
)
