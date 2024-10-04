package example.com.data.schema

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class FinanceStateService(
    private val database: Database
) {
    object FinanceStates : Table() {
        val name = varchar(name = "name", length = 30)

        override val primaryKey = PrimaryKey(name)
    }

    init {
        prepareDatabase()
    }

    private fun prepareDatabase() {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(FinanceStates)
        }
    }

    suspend fun addStates() {
        FinanceState.entries.forEach {
            create(it)
        }
    }

    suspend fun create(state: FinanceState) = dbQuery {
        FinanceStates.insertIgnore {
            it[name] = state.name
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}