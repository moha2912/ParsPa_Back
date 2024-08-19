package example.com.data.model.res

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

@Serializable
data class OTPResponse(
    val msg: String,
    val remaining: Long,
)
