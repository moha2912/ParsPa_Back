package example.com.data.model.res

import kotlinx.serialization.Serializable

@Serializable
data class PriceResponse(
    val price: Long,
    val priceFormatted: String,
)
