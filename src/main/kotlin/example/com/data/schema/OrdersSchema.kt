package example.com.data.schema

import example.com.data.model.OrderState
import example.com.data.schema.OrderService.Orders
import example.com.routes.ChangeState
import example.com.routes.InsoleRequest
import example.com.routes.getUserImages
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ExposedOrder(
    val id: Long? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val orderID: Long? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val userID: Long? = null,
    val created: Long? = null,
    val feetLength: Float,
    val feetSize: Int,
    val weight: Float,
    val state: OrderState = OrderState.PROCESSING,
    val notes: String? = "",
    val isNew: Boolean = true,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val isAdminNew: Boolean? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val insole: ExposedInsole? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val doctorResponse: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val resendPictures: List<String>? = null,
    val concerns: List<Int>? = emptyList(),
    val images: Map<Foots, Map<Angles, String>>,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val video: String? = null,
) {
    val isNotBlank
        get():Boolean {
            return Foots.entries.all { foot ->
                val map = images[foot] ?: return@all false
                Angles.entries
                    .take(foot.count * 2)
                    .all {
                        map[it]
                            .isNullOrBlank()
                            .not()
                    }
            }
        }

    fun isAllExists(id: Long): Boolean {
        return Foots.entries.all { foot ->
            val map = images[foot] ?: return@all false
            map.all { File(getUserImages(id).plus(it.value)).exists() }
        }
    }
}

@Serializable
data class ExposedInsole(
    val count: Int,
    val address: String,
    val phone: String,
)

fun ResultRow.toOrder(fillAdmin: Boolean = false) = ExposedOrder(
    id = this[Orders.id],
    created = this[Orders.created],
    feetLength = this[Orders.feetLength],
    feetSize = this[Orders.feetSize],
    weight = this[Orders.weight],
    isNew = this[Orders.isNew],
    doctorResponse = this[Orders.doctorResponse],
    notes = this[Orders.notes],
    state = OrderState.valueOf(this[Orders.status]),
    concerns = Json.decodeFromString(this[Orders.concerns]),
    images = Json.decodeFromString(this[Orders.images]),
    insole = this[Orders.insole]?.let { Json.decodeFromString(it) },
    resendPictures = this[Orders.resendPictures]?.let { Json.decodeFromString(it) },
    isAdminNew = if (!fillAdmin) null else this[Orders.isAdminNew],
    userID = if (!fillAdmin) null else this[Orders.userId]
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
        val images = text("images")//.default("")
        val concerns = text("concerns")//.default("[]")
        val notes = text("notes")//.default("")
        val resendPictures = text("resend_pictures").nullable()
        val isNew = bool("is_new").default(true)
        val isAdminNew = bool("is_admin_new").default(true)
        val status = varchar(
            "status",
            length = 30
        ).default(OrderState.PROCESSING.name) references OrderStateService.OrderStates.name
        val video = text("video").nullable()
        val insole = text("insole").nullable()
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
            it[images] = Json.encodeToString(order.images)
            order.concerns?.let { n ->
                it[concerns] = Json.encodeToString(n)
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

    suspend fun readAllOrders(filter: String?): List<ExposedOrder> {
        return dbQuery {
            Orders
                .selectAll()
                .run {
                    filter?.let {
                        where { Orders.status.eq(it) }
                    } ?: this
                }
                .map {
                    it.toOrder(fillAdmin = true)
                }
                .sortedWith(compareBy<ExposedOrder> { it.isAdminNew != true }.thenBy { it.created })
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

    suspend fun readOrder(userID: Long, id: Long?): ExposedOrder? {
        id ?: return null
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

    suspend fun setAdminReadOrder(id: Long) {
        dbQuery {
            Orders.update({
                Orders.id.eq(id)
            }) {
                it[isAdminNew] = false
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
                it[status] = OrderState.PROCESSING.name
                it[resendPictures] = null
                it[images] = Json.encodeToString(order.images)
                order.concerns?.let { concern ->
                    it[concerns] = Json.encodeToString(concern)
                }
                order.notes?.let { note ->
                    it[notes] = note
                }
                it[isAdminNew] = true
            }
        }
    }

    suspend fun updateState(order: ChangeState) {
        dbQuery {
            Orders.update({ Orders.id eq order.orderID }) {
                it[resendPictures] =
                    if (order.newState != OrderState.ERROR_RESEND) null else Json.encodeToString(order.resendPictures)
                order.doctorResponse?.let { r ->
                    it[doctorResponse] = r
                }
                it[isNew] = true
                it[status] = order.newState.name
            }
        }
    }

    suspend fun addInsole(order: InsoleRequest) {
        order.orderID ?: return
        dbQuery {
            Orders.update({ Orders.id eq order.orderID }) { up ->
                up[insole] = Json.encodeToString(order.copy(orderID = null))
                up[status] = OrderState.IN_PRODUCTION.name
                up[isAdminNew] = true
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

@Serializable
enum class Foots(val count: Int) {
    RIGHT(2),
    LEFT(2),
    KNEES(1),
}

@Serializable
enum class Angles {
    FRONT,
    BACK,
    UPSIDE,
    INSIDE,
}