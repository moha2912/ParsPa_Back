package example.com.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.*
import kotlin.time.Duration.Companion.seconds

fun Application.configureRateLimit() {
    install(RateLimit) {
        global {
            rateLimiter(limit = 2, refillPeriod = 1.seconds)
        }
        register(RateLimitName("otp")) {
            rateLimiter(limit = 2, refillPeriod = 1.seconds)
        }
    }
}