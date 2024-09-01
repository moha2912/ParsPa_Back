package example.com.data.model

enum class OrderState {
    PROCESSING,
    ERROR_RESEND,
    DOCTOR_RESPONSE,
    IN_PRODUCTION,
    SENDING,
    DELIVERED,
}