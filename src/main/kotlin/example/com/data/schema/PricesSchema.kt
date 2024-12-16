package example.com.data.schema

import example.com.data.model.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.text.DecimalFormat

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ExposedPrice(
    @EncodeDefault(EncodeDefault.Mode.NEVER) val name: String? = null,
    val price: Long,
    val priceFormatted: String,
)

class PricesService(
    private val database: Database
) {
    object Prices : Table() {
        val id = long("_id").autoIncrement()
        val name = varchar("name", length = 60)
        val price = long("price").default(0)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        prepareDatabase()
    }

    private fun prepareDatabase() {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(Prices)
        }
    }

    suspend fun create(exposedPrice: ExposedPrice) = dbQuery {
        Prices.insert {
            it[name] = exposedPrice.name ?: ""
            it[price] = exposedPrice.price
        }
    }

    suspend fun read(name: String): ExposedPrice? {
        return dbQuery {
            Prices
                .selectAll()
                .where {
                    Prices.name.eq(name)
                }
                .map {
                    val price = it[Prices.price]
                    ExposedPrice(
                        price = price,
                        priceFormatted = DecimalFormat
                            .getInstance()
                            .format(price)
                            .plus(" ")
                            .plus(Strings.TOMAN)
                    )
                }
                .singleOrNull()
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}

