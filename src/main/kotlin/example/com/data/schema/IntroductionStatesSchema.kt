package example.com.data.schema

import example.com.data.model.IntroStateModel
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class IntroductionStateService(
    private val database: Database
) {
    object IntroductionState : Table() {
        val id = varchar(name = "id", length = 30)
        val title = varchar(name = "title", length = 60)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        prepareDatabase()
    }

    private fun prepareDatabase() {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(IntroductionState)
        }
    }

    suspend fun getStates(): List<IntroStateModel> {
        return dbQuery {
            val items = IntroductionState
                .selectAll()
                .map { it.toState() }

            val targetItem = items.find { it.id == "NOT_DEFINED" }
            if (targetItem != null) {
                listOf(targetItem) + items.filter { it != targetItem }
            } else {
                items
            }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}

fun ResultRow.toState() = IntroStateModel(
    id = this[IntroductionStateService.IntroductionState.id],
    name = this[IntroductionStateService.IntroductionState.title]
)