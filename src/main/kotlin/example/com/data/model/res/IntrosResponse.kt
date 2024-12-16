package example.com.data.model.res

import example.com.data.model.IntroStateModel
import kotlinx.serialization.Serializable

@Serializable
data class IntrosResponse(
    val msg: String = "Ok.",
    val intros: List<IntroStateModel>,
)
