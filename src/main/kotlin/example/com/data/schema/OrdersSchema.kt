package example.com.data.schema

import example.com.data.model.OrderState
import example.com.data.schema.OrderService.Orders
import example.com.routes.InsoleRequest
import example.com.routes.getUserImages
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

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
    @EncodeDefault(EncodeDefault.Mode.NEVER) val orderID: Long? = null,
    val created: Long? = null,
    val feetLength: Float,
    val feetSize: Int,
    val weight: Float,
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
) {
    val angleMap
        get() = mapOf(
            FootAngles.FRONT to front,
            FootAngles.SIDE to side,
            FootAngles.INSIDE to inside,
            FootAngles.OUTSIDE to outside,
        )

    val hasBlank
        get() = angleMap.any { it.value.isBlank() }

    val whichIsBlank
        get():String {
            val map = angleMap.filter { it.value.isBlank() }
            return map
                .map { it.key }
                .joinToString(separator = ",") { it.name }
        }

    fun hasNotExists(id: Long): Boolean =
        angleMap.any { !File(getUserImages(id).plus(it.value)).exists() }


    fun whichIsNotExists(id: Long): String {
        val map = angleMap.filter { !File(getUserImages(id).plus(it.value)).exists() }
        return map
            .map { it.key }
            .joinToString(separator = ",") { it.name }
    }
}

@Serializable
data class ExposedInsole(
    val count: Int,
    val address: String,
    val phone: String,
)

fun ResultRow.toOrder() = ExposedOrder(
    id = this[Orders.id],
    created = this[Orders.created],
    feetLength = this[Orders.feetLength],
    feetSize = this[Orders.feetSize],
    weight = this[Orders.weight],
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
            phone = this[Orders.phone] ?: "",
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
        val feetSize = integer("feet_size").default(0)
        val weight = float("weight")
        val angleFront = text("angle_front")
        val angleSide = text("angle_side")
        val angleInside = text("angle_inside")
        val angleOutside = text("angle_outside")
        val concerns = varchar("concerns", length = 30).default("")
        val notes = text("notes")//.default("")
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
        val phone = varchar("phone", length = 30).nullable()
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
            it[feetSize] = order.feetSize
            it[weight] = order.weight
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

    suspend fun readOrders(id: Long): List<ExposedOrder> {
        return dbQuery {
            Orders
                .selectAll()
                .where { Orders.userId eq id }
                .map {
                    it.toOrder()
                }
        }
    }

    suspend fun readUnreadOrders(id: Long): List<ExposedOrder> {
        return dbQuery {
            Orders
                .selectAll()
                .where { Orders.userId eq id and Orders.isNew.eq(true) }
                .map {
                    it.toOrder()
                }
        }
    }

    suspend fun readOrder(userID: Long, id: Long): ExposedOrder? {
        return dbQuery {
            Orders
                .selectAll()
                .where {
                    Orders.id.eq(id) and Orders.userId.eq(userID)
                }
                .map {
                    it.toOrder()
                }
                .singleOrNull()
        }
    }

    suspend fun setReadOrder(userID: Long, id: Long) {
        dbQuery {
            Orders.update({
                Orders.id.eq(id) and Orders.userId.eq(userID)
            }) {
                it[isNew] = false
            }
        }
    }

    suspend fun isNotExists(userID: Long, id: Long): Boolean {
        return dbQuery {
            Orders
                .selectAll()
                .where {
                    Orders.id.eq(id) and Orders.userId.eq(userID)
                }
                .empty()
        }
    }

    suspend fun update(id: Long, order: ExposedOrder) {
        dbQuery {
            Orders.update({ Orders.id eq id }) {
                it[feetLength] = order.feetLength
                it[feetSize] = order.feetSize
                it[weight] = order.weight
                it[status] = OrderState.PROCESSING
                it[resendPictures] = null
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

    suspend fun addInsole(order: InsoleRequest) {
        dbQuery {
            Orders.update({ Orders.id eq order.orderID }) { up ->
                up[orderCount] = order.count
                up[address] = order.address
                up[phone] = order.phone
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

