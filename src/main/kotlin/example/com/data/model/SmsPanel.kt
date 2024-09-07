package example.com.data.model

import kotlinx.serialization.Serializable


@Serializable
data class RequestSMS(
    var code: String,
    var recipient: String,
    var sender: String,
    var variable: Variable
) {
    @Serializable
    data class Variable(
        val hash: String,
        val otp: Int
    )
}

@Serializable
data class NormalSMS(
    var recipient: List<String>,
    var sender: String,
    var message: String
)

@Serializable
data class ResponseSMS(
    val code: Int?,
    val `data`: String?,
    val error_message: String?,
    val status: String?
)