package example.com.data.schema

import example.com.data.schema.AdminUserService.AdminUsers
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
data class ExposedAdminUser(
    @EncodeDefault(EncodeDefault.Mode.NEVER) var id: Long? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) var password: String? = null,
    val username: String? = null,
    val isActive: Boolean? = null,
)

fun ResultRow.getExposedAdminUser(): ExposedAdminUser = ExposedAdminUser(
    id = this[AdminUsers.id],
    username = this[AdminUsers.username],
    isActive = this[AdminUsers.isActive]
)

class AdminUserService(
    private val database: Database
) {
    object AdminUsers : Table() {
        val id = long("_id").autoIncrement()
        val username = varchar("username", length = 20)
        val password = text("password")
        val isActive = bool("isActive").default(false)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        prepareDatabase()
    }

    private fun prepareDatabase() {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(AdminUsers)
            //exec("ALTER TABLE ${AdminUsers.tableName} AUTO_INCREMENT = 1000000")
        }
    }

    suspend fun create(exposedAdminUser: ExposedAdminUser): Long = dbQuery {
        AdminUsers.insert {
            it[username] = exposedAdminUser.username ?: throw NullPointerException()
            it[password] = exposedAdminUser.password ?: throw NullPointerException()
        }[AdminUsers.id]
    }

    suspend fun readID(id: Long): ExposedAdminUser? {
        return dbQuery {
            AdminUsers
                .selectAll()
                .where { AdminUsers.id eq id }
                .map {
                    it.getExposedAdminUser()
                }
                .singleOrNull()
        }
    }

    suspend fun existUser(username: String, password: String): Boolean {
        return dbQuery {
            AdminUsers
                .selectAll()
                .where {
                    AdminUsers.username.eq(username) and AdminUsers.password.eq(password)
                }
                .empty()
                .not()
        }
    }

    suspend fun readUser(username: String, password: String): ExposedAdminUser? {
        return dbQuery {
            AdminUsers
                .selectAll()
                .where {
                    AdminUsers.username.eq(username) and AdminUsers.password.eq(password)
                }
                .map {
                    it.getExposedAdminUser()
                }
                .singleOrNull()
        }
    }

    suspend fun update(id: Long, isActive: Boolean) {
        dbQuery {
            AdminUsers.update({ AdminUsers.id eq id }) { up ->
                up[AdminUsers.isActive] = isActive
            }
        }
    }

    suspend fun delete(id: Long) {
        dbQuery {
            AdminUsers.deleteWhere { AdminUsers.id.eq(id) }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}

