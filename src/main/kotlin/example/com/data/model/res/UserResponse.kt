package example.com.data.model.res

import example.com.data.schema.ExposedUser
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class UserResponse(
    val msg: String,
    val user: ExposedUser?,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val token: String? = null,
)
