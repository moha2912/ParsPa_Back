package example.com.data.schema

import example.com.data.model.ZibalVerifyResponse
import example.com.routes.InsoleRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

enum class FinanceState {
    NO_ORDER,
    WAITING,
    ERROR,
    SUCCESS
}

@Serializable
data class ExposedFinance(
    val id: Long = -1,
    val date: Long,
    val successDate: Long? = null,
    val trackID: Long,
    val userID: Long,
    val orderID: Long,
    val insole: InsoleRequest,
    val zibal: ZibalVerifyResponse? = null,
    val status: FinanceState = FinanceState.WAITING,
)

fun ResultRow.getExposedFinance(): ExposedFinance = ExposedFinance(
    id = this[FinancialService.Financial.id],
    date = this[FinancialService.Financial.date],
    successDate = this[FinancialService.Financial.successDate],
    trackID = this[FinancialService.Financial.trackID],
    userID = this[FinancialService.Financial.userID],
    orderID = this[FinancialService.Financial.orderID],
    insole = this[FinancialService.Financial.insole].let { Json.decodeFromString(it) },
    zibal = this[FinancialService.Financial.zibal]?.let { Json.decodeFromString(it) },
    status = FinanceState.valueOf(this[FinancialService.Financial.status])
)

class FinancialService(
    private val database: Database
) {
    object Financial : Table() {
        val id = long("_id").autoIncrement()
        val date = long("date")
        val successDate = long("success_date").nullable()
        val trackID = long("track_id")
        val userID = long("user_id")
        val orderID = long("order_id")
        val insole = text("insole")
        val zibal = text("zibal").nullable()
        val status = varchar(
            "status",
            length = 30
        ).default(FinanceState.WAITING.name) references FinanceStateService.FinanceStates.name

        override val primaryKey = PrimaryKey(id)
    }

    init {
        prepareDatabase()
    }

    private fun prepareDatabase() {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(Financial)
        }
    }

    suspend fun create(exposedFinance: ExposedFinance) = dbQuery {
        Financial.insert {
            it[date] = System.currentTimeMillis()
            it[trackID] = exposedFinance.trackID
            it[userID] = exposedFinance.userID
            it[insole] = Json.encodeToString(exposedFinance.insole)
            it[orderID] = exposedFinance.orderID
            it[status] = FinanceState.WAITING.name
        }
    }

    suspend fun readID(trackID: Long): ExposedFinance? {
        return dbQuery {
            Financial
                .selectAll()
                .where { Financial.trackID eq trackID }
                .map {
                    it.getExposedFinance()
                }
                .maxByOrNull { it.date }
        }
    }

    suspend fun readOrderID(orderID: Long, userID: Long): ExposedFinance? {
        return dbQuery {
            Financial
                .selectAll()
                .where { Financial.orderID eq orderID }
                .map {
                    it.getExposedFinance()
                }
                .maxByOrNull { it.date }
        }
    }

    suspend fun readOrderID(orderID: Long, status: FinanceState): ExposedFinance? {
        return dbQuery {
            Financial
                .selectAll()
                .where { Financial.orderID.eq(orderID) and Financial.status.eq(status.name) }
                .map {
                    it.getExposedFinance()
                }
                .maxByOrNull { it.date }
        }
    }

    suspend fun getAllFinances(start: Long, end: Long): List<ExposedFinance> {
        return dbQuery {
            Financial
                .selectAll()
                .where {
                    listOfNotNull(
                        Financial.status eq FinanceState.SUCCESS.name,
                        start
                            .takeIf { it > 0 }
                            ?.let { Financial.successDate greaterEq it },
                        end
                            .takeIf { it > 0 }
                            ?.let { Financial.successDate lessEq it }
                    ).reduceOrNull { acc, condition -> acc and condition } ?: Op.TRUE
                }
                .map {
                    it.getExposedFinance()
                }
        }
    }

    suspend fun update(exposedFinance: ExposedFinance) {
        dbQuery {
            Financial.update({ Financial.trackID eq exposedFinance.trackID }) {
                it[successDate] = System.currentTimeMillis()
                it[status] = status.name
            }
        }
    }

    suspend fun update(id: Long, trackID: Long, status: FinanceState, zibal: ZibalVerifyResponse?) {
        dbQuery {
            Financial.update({ Financial.id.eq(id) and Financial.trackID.eq(trackID) }) {
                it[successDate] = System.currentTimeMillis()
                it[Financial.status] = status.name
                it[Financial.zibal] = Json.encodeToString(zibal)
            }
        }
    }

    suspend fun updateError(orderID: Long?) {
        orderID ?: return
        dbQuery {
            // todo notEq success
            Financial.update({ Financial.orderID.eq(orderID)/* and Financial.status.*/ }) {
                it[status] = FinanceState.ERROR.name
            }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

}

