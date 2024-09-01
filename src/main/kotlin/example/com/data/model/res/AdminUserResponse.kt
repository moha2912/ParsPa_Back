package example.com.data.model.res

import example.com.data.schema.ExposedAdminUser
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class AdminUserResponse(
    val msg: String,
    val user: ExposedAdminUser?,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val token: String? = null,
)
