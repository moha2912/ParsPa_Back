package example.com.data.model

enum class OrderState(
    val msg: String = ""
) {
    PROCESSING,
    ERROR_RESEND(Strings.STATE_RESEND),
    DOCTOR_RESPONSE(Strings.STATE_RESPONSE),
    IN_PRODUCTION,
    SENDING(Strings.STATE_SENDING),
    DELIVERED,
}