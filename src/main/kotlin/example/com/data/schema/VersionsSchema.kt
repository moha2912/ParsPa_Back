package example.com.data.schema

import example.com.data.schema.OTPService.OTPs.code
import example.com.data.schema.OrderService.Orders
import example.com.data.schema.OrderService.Orders.angleFront
import example.com.data.schema.OrderService.Orders.angleInside
import example.com.data.schema.OrderService.Orders.angleOutside
import example.com.data.schema.OrderService.Orders.angleSide
import example.com.data.schema.OrderService.Orders.concerns
import example.com.data.schema.OrderService.Orders.feetLength
import example.com.data.schema.OrderService.Orders.notes
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.Random

@Serializable
data class ExposedVersion(
    val name: String,
    val version: String,
    val versionCode: Int,
    val lastChanges: String,
    val mandatory: Boolean,
    val updateUrl: String
)

class VersionsService(
    private val database: Database
) {
    object Versions : Table() {
        val id = long("_id").autoIncrement()
        val name = varchar("name", length = 60)
        val version = varchar("version", length = 60).default("1.0")
        val versionCode = integer("version_code").default(1)
        val lastChanges = text("last_changes").default("")
        val mandatory = bool("mandatory").default(false)
        val updateUrl = text("update_url").default("")

        override val primaryKey = PrimaryKey(id)
    }

    init {
        prepareDatabase()
    }

    private fun prepareDatabase() {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(Versions)
        }
    }

    suspend fun create(exposedVersion: ExposedVersion) = dbQuery {
        Versions.insert {
            it[name] = exposedVersion.name
            it[version] = exposedVersion.version
            it[versionCode] = exposedVersion.versionCode
            it[lastChanges] = exposedVersion.lastChanges
            it[mandatory] = exposedVersion.mandatory
            it[updateUrl] = exposedVersion.updateUrl
        }
    }

    suspend fun read(name: String): ExposedVersion? {
        return dbQuery {
            Versions
                .selectAll()
                .where {
                    Versions.name.eq(name)
                }
                .map {
                    ExposedVersion(
                        name = it[Versions.name],
                        version = it[Versions.version],
                        versionCode = it[Versions.versionCode],
                        lastChanges = it[Versions.lastChanges],
                        mandatory = it[Versions.mandatory],
                        updateUrl = it[Versions.updateUrl],
                    )
                }
                .singleOrNull()
        }
    }

    suspend fun update(exposedVersion: ExposedVersion) {
        dbQuery {
            Versions.update({ Versions.name eq exposedVersion.name }) {
                it[version] = exposedVersion.version
                it[versionCode] = exposedVersion.versionCode
                it[lastChanges] = exposedVersion.lastChanges
                it[mandatory] = exposedVersion.mandatory
                it[updateUrl] = exposedVersion.updateUrl
            }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}

