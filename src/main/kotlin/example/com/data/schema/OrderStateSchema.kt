package example.com.data.schema

import example.com.data.model.OrderState
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class OrderStateService(
    private val database: Database
) {
    object OrderStates : Table() {
        val name = varchar(name = "name", length = 30)

        override val primaryKey = PrimaryKey(name)
    }

    init {
        prepareDatabase()
    }

    private fun prepareDatabase() {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(OrderStates)
        }
    }

    suspend fun addStates() {
        OrderState.entries.forEach {
            create(it)
        }
    }

    suspend fun create(state: OrderState) = dbQuery {
        OrderStates.insertIgnore {
            it[name] = state.name
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}