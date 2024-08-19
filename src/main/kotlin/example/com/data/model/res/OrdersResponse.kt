package example.com.data.model.res

import example.com.data.schema.ExposedOrder
import example.com.data.schema.ExposedUser
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

@Serializable
data class OrdersResponse(
    val msg: String,
    val orders: List<ExposedOrder>,
)
