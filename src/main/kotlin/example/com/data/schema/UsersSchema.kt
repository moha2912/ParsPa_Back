package example.com.data.schema

import example.com.data.schema.UserService.Users
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ExposedUser(
    @EncodeDefault(EncodeDefault.Mode.NEVER) var id: Long? = null,
    var email: String? = null,
    val name: String? = null,
    val birthday: String? = null,
    val gender: Short? = null,
    var phone: String? = null,
    val address: String? = null,
    val appNotify: Boolean? = null,
    val smsNotify: Boolean? = null,
)

fun ResultRow.getExposedUser(): ExposedUser = ExposedUser(
    id = this[Users.id],
    email = this[Users.email],
    name = this[Users.name],
    birthday = this[Users.birthday],
    gender = this[Users.gender],
    phone = this[Users.phone],
    address = this[Users.address],
    appNotify = this[Users.appNotify],
    smsNotify = this[Users.smsNotify],
)

class UserService(
    private val database: Database
) {
    object Users : Table() {
        val id = long("_id").autoIncrement()
        val email = varchar("email", length = 60).default("")
        val name = varchar("name", length = 60).default("")
        val birthday = varchar("birthday", length = 10).default("")
        val gender = short("gender").default(0)
        val phone = varchar("phone", length = 30).default("")
        val address = text("address").nullable()//.default("")
        val appNotify = bool("app_notify").default(true)
        val smsNotify = bool("sms_notify").default(true)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        prepareDatabase()
    }

    private fun prepareDatabase() {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(Users)
        }
    }

    suspend fun create(email: String): Long = dbQuery {
        Users.insert {
            it[Users.email] = email
        }[Users.id]
    }

    suspend fun createPhone(phone: String): Long = dbQuery {
        Users.insert {
            it[Users.phone] = phone
        }[Users.id]
    }

    suspend fun readID(id: Long): ExposedUser? {
        return dbQuery {
            Users
                .selectAll()
                .where { Users.id eq id }
                .map {
                    it.getExposedUser()
                }
                .singleOrNull()
        }
    }

    suspend fun readEmail(email: String): ExposedUser? {
        return dbQuery {
            Users
                .selectAll()
                .where { Users.email eq email }
                .map {
                    it.getExposedUser()
                }
                .singleOrNull()
        }
    }

    suspend fun readPhone(phone: String): ExposedUser? {
        return dbQuery {
            Users
                .selectAll()
                .where { Users.phone eq phone }
                .map {
                    it.getExposedUser()
                }
                .singleOrNull()
        }
    }

    suspend fun update(id: Long, user: ExposedUser) {
        dbQuery {
            Users.update({ Users.id eq id }) { up ->
                user.name?.let {
                    up[name] = it
                }
                user.birthday?.let {
                    up[birthday] = it
                }
                user.gender?.let {
                    up[gender] = it
                }
                user.phone?.let {
                    up[phone] = it
                }
                user.email?.let {
                    up[email] = it
                }
                user.address?.let {
                    up[address] = it
                }
                user.smsNotify?.let {
                    up[smsNotify] = it
                }
                user.appNotify?.let {
                    up[appNotify] = it
                }
            }
        }
    }

    suspend fun delete(id: Long) {
        dbQuery {
            Users.deleteWhere { Users.id.eq(id) }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun isNotFilled(id: Long): Boolean {
        return readID(id)?.address.isNullOrBlank()
    }
}

