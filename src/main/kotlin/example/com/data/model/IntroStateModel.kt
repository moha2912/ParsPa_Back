package example.com.data.model

import kotlinx.serialization.Serializable

@Serializable
data class IntroStateModel(
    val id: String,
    val name: String
)