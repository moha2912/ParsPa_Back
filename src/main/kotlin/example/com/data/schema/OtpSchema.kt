package example.com.data.schema

import example.com.data.schema.OrderService.Orders
import example.com.data.schema.UserService.Users.email
import example.com.routes.OTP_TIME
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.TimeUnit

@Serializable
data class ExposedOTP(
    val id: Long,
    val created: Long,
    val field: String,
    val code: Int,
)

class OTPService(
    private val database: Database
) {
    object OTPs : Table() {
        val id = long("_id").autoIncrement()
        val created = long("created")
        val field = varchar("field", length = 60)
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

    suspend fun create(requestField: String,otp:Int): Int = dbQuery {
        OTPs.insert {
            it[field] = requestField
            it[created] = System.currentTimeMillis()
            it[code] = otp
        }[OTPs.code]
    }

    suspend fun read(field: String): ExposedOTP? {
        return dbQuery {
            OTPs
                .selectAll()
                .where {
                    OTPs.field.eq(field) and OTPs.created.greater(
                        System
                            .currentTimeMillis()
                            .minus(OTP_TIME)
                    )
                }
                .map {
                    ExposedOTP(
                        id = it[OTPs.id],
                        created = it[OTPs.created],
                        field = it[OTPs.field],
                        code = it[OTPs.code],
                    )
                }
                .singleOrNull()
        }
    }

    suspend fun read(field: String, otp: Int): ExposedOTP? {
        return dbQuery {
            OTPs
                .selectAll()
                .where {
                    OTPs.field.eq(field) and OTPs.code.eq(otp) and OTPs.created.greater(
                        System
                            .currentTimeMillis()
                            .minus(OTP_TIME)
                    )
                }
                .map {
                    ExposedOTP(
                        id = it[OTPs.id],
                        created = it[OTPs.created],
                        field = it[OTPs.field],
                        code = it[OTPs.code],
                    )
                }
                .singleOrNull()
        }
    }

    suspend fun delete(field: String) {
        dbQuery {
            OTPs.deleteWhere { OTPs.field.eq(field) }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}

