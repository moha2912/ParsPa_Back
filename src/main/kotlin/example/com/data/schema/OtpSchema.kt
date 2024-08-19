package example.com.data.schema

import example.com.data.schema.OrderService.Orders
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.TimeUnit

val OTP_TIME = TimeUnit.MINUTES.toMillis(2)

@Serializable
data class ExposedOTP(
    val id: Long,
    val created: Long,
    val email: String,
    val code: Int,
)

class OTPService(
    private val database: Database
) {
    object OTPs : Table() {
        val id = long("_id").autoIncrement()
        val created = long("created")
        val email = varchar("email", length = 60)
        val code = integer("code")

        override val primaryKey = PrimaryKey(id)
    }

    init {
        prepareDatabase()
    }

    private fun prepareDatabase() {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(OTPs)
        }
    }

    suspend fun create(requestEmail: String): Int = dbQuery {
        OTPs.insert {
            it[email] = requestEmail
            it[created] = System.currentTimeMillis()
            it[code] = 1234 //todo Random().nextInt(8999) + 1000
        }[OTPs.code]
    }

    suspend fun read(email: String): ExposedOTP? {
        return dbQuery {
            OTPs
                .selectAll()
                .where {
                    OTPs.email.eq(email) and OTPs.created.greater(
                        System
                            .currentTimeMillis()
                            .minus(OTP_TIME)
                    )
                }
                .map {
                    ExposedOTP(
                        id = it[OTPs.id],
                        created = it[OTPs.created],
                        email = it[OTPs.email],
                        code = it[OTPs.code],
                    )
                }
                .singleOrNull()
        }
    }

    suspend fun read(email: String, otp: Int): ExposedOTP? {
        return dbQuery {
            OTPs
                .selectAll()
                .where {
                    OTPs.email.eq(email) and OTPs.code.eq(otp) and OTPs.created.greater(
                        System
                            .currentTimeMillis()
                            .minus(OTP_TIME)
                    )
                }
                .map {
                    ExposedOTP(
                        id = it[OTPs.id],
                        created = it[OTPs.created],
                        email = it[OTPs.email],
                        code = it[OTPs.code],
                    )
                }
                .singleOrNull()
        }
    }

    suspend fun delete(email: String) {
        dbQuery {
            OTPs.deleteWhere { OTPs.email.eq(email) }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}

