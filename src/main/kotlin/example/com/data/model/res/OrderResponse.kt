package example.com.data.model.res

import example.com.data.schema.ExposedOrder
import example.com.data.schema.ExposedUser
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

@Serializable
data class OrderResponse(
    val msg: String,
    val order: ExposedOrder,
)
