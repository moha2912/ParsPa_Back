package example.com.data.schema

import example.com.data.model.OrderState
import example.com.data.schema.OrderService.Orders
import example.com.routes.InsoleRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
enum class FootAngles {
    FRONT,
    SIDE,
    INSIDE,
    OUTSIDE,
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ExposedOrder(
    val id: Long? = null,
    val created: Long? = null,
    val feetLength: Float,
    val state: OrderState = OrderState.PROCESSING,
    val notes: String? = "",
    val isNew: Boolean = true,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val insole: ExposedInsole? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val doctorResponse: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val resendPictures: List<FootAngles>? = null,
    val concerns: List<Int>? = emptyList(),
    val images: ExposedAngles,
)

@Serializable
data class ExposedAngles(
    val front: String,
    val side: String,
    val inside: String,
    val outside: String,
)

@Serializable
data class ExposedInsole(
    val count: Int,
    val address: String,
)

fun ResultRow.toOrder() = ExposedOrder(
    id = this[Orders.id],
    created = this[Orders.created],
    feetLength = this[Orders.feetLength],
    isNew = this[Orders.isNew],
    doctorResponse = this[Orders.doctorResponse],
    notes = this[Orders.notes],
    state = this[Orders.status],
    concerns = this[Orders.concerns].run {
        val list = split(",")
        if (this.isNotBlank() && list.isNotEmpty()) {
            list.map { it.toInt() }
        } else {
            emptyList()
        }
    },
    images = ExposedAngles(
        front = this[Orders.angleFront],
        side = this[Orders.angleSide],
        inside = this[Orders.angleInside],
        outside = this[Orders.angleOutside],
    ),
    insole = this[Orders.orderCount]?.let {
        ExposedInsole(
            count = it,
            address = this[Orders.address] ?: "",
        )
    },
    resendPictures = this[Orders.resendPictures]?.run {
        val list = split(",")
        if (this.isNotBlank() && list.isNotEmpty()) {
            list.map { FootAngles.valueOf(it) }
        } else {
            emptyList()
        }
    },
)

class OrderService(
    private val database: Database
) {
    object Orders : Table() {
        val id = long("_id").autoIncrement()
        val created = long("created")
        val userId = long("user_id") references UserService.Users.id
        val feetLength = float("feet_length")
        val angleFront = text("angle_front")
        val angleSide = text("angle_side")
        val angleInside = text("angle_inside")
        val angleOutside = text("angle_outside")
        val concerns = varchar("concerns", length = 30).default("")
        val notes = text("notes").default("")
        val orderCount = integer("order_count").nullable()
        val resendPictures = varchar("resend_pictures", length = 30).nullable()
        val isNew = bool("is_new").default(true)
        val status: Column<OrderState> = customEnumeration(
            name = "status",
            sql = "VARCHAR(30)",
            fromDb = { value -> OrderState.valueOf(value as String) },
            toDb = { it.name }
        ).default(OrderState.PROCESSING)
        val address = text("address").nullable()
        val doctorResponse = text("doctor_response").nullable()

        override val primaryKey = PrimaryKey(id)
    }

    init {
        prepareDatabase()
    }

    private fun prepareDatabase() {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(Orders)
        }
    }

    suspend fun create(userID: Long, order: ExposedOrder): Long = dbQuery {
        Orders.insert {
            it[created] = System.currentTimeMillis()
            it[userId] = userID
            it[feetLength] = order.feetLength
            order.images.let { o ->
                it[angleFront] = o.front
                it[angleSide] = o.side
                it[angleInside] = o.inside
                it[angleOutside] = o.outside
            }
            order.concerns?.let { n ->
                it[concerns] = n.joinToString(separator = ",") { it.toString() }
            }
            order.notes?.let { n ->
                it[notes] = n
            }
        }[Orders.id]
    }

    suspend fun read(id: Long): List<ExposedOrder> {
        return dbQuery {
            Orders
                .selectAll()
                .where { Orders.userId eq id }
                .map {
                    it.toOrder()
                }
        }
    }

    suspend fun readUnread(id: Long): List<ExposedOrder> {
        return dbQuery {
            Orders
                .selectAll()
                .where { Orders.userId eq id and Orders.isNew.eq(true) }
                .map {
                    it.toOrder()
                }
        }
    }

    suspend fun isNotExists(id: Long): Boolean {
        return dbQuery {
            Orders
                .selectAll()
                .where { Orders.userId eq id }
                .empty()
        }
    }

    suspend fun update(id: Long, order: ExposedOrder) {
        dbQuery {
            Orders.update({ Orders.id eq id }) {
                it[feetLength] = order.feetLength
                order.images.let { angle ->
                    it[angleFront] = angle.front
                    it[angleSide] = angle.side
                    it[angleInside] = angle.inside
                    it[angleOutside] = angle.outside
                }
                order.concerns?.let { concern ->
                    it[concerns] = concern.joinToString(separator = ",") { it.toString() }
                }
                order.notes?.let { note ->
                    it[notes] = note
                }
            }
        }
    }

    suspend fun readOrder(id: Long) {
        dbQuery {
            Orders.update({ Orders.id eq id }) { up ->
                up[isNew] = false
            }
        }
    }

    suspend fun addInsole(id: Long, order: InsoleRequest) {
        dbQuery {
            Orders.update({ Orders.id eq id }) { up ->
                up[orderCount] = order.count
                up[address] = order.address
                up[status] = OrderState.IN_PRODUCTION
            }
        }
    }

    suspend fun delete(id: Long) {
        dbQuery {
            Orders.deleteWhere { Orders.id.eq(id) }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}

