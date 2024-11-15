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
    val age: Int = 20, //todo
    val feetLength: Float,
    val feetWidth: Float = 10f, //todo
    val feetSize: Float,
    val gender: Short,
    val weight: Float,
    val state: OrderState = OrderState.PROCESSING,
    val notes: String? = "",
    val isNew: Boolean = true,
    val platform: Short = 0,
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

                if (foot == Foots.KNEE) map[Angles.FRONT]
                    .isNullOrBlank()
                    .not() else
                    Angles.entries
                        .take(4)
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
    val postID: Long = 0,
    val count: Int,
    val name: String = "",
    val address: String,
    val phone: String,
)

fun ResultRow.toOrder(fillAdmin: Boolean = false) = ExposedOrder(
    id = this[Orders.id],
    created = this[Orders.created],
    feetWidth = this[Orders.feetWidth],
    feetLength = this[Orders.feetLength],
    feetSize = this[Orders.feetSize],
    age = this[Orders.age],
    gender = this[Orders.gender],
    weight = this[Orders.weight],
    isNew = this[Orders.isNew],
    platform = this[Orders.platform],
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
        val feetWidth = float("feet_width")
        val feetSize = float("feet_size").default(0f)
        val age = integer("age").default(0)
        val gender = short("gender").default(0)
        val weight = float("weight")
        val images = text("images")//.default("")
        val concerns = text("concerns")//.default("[]")
        val notes = text("notes")//.default("")
        val resendPictures = text("resend_pictures").nullable()
        val isNew = bool("is_new").default(true)
        val platform = short("platform").default(0)
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
            it[age] = order.age
            it[feetWidth] = order.feetWidth
            it[feetLength] = order.feetLength
            it[feetSize] = order.feetSize
            it[gender] = order.gender
            it[weight] = order.weight
            it[platform] = order.platform
            it[images] = Json.encodeToString(order.images)
            order.concerns?.let { n ->
                it[concerns] = Json.encodeToString(n)
            }
            order.notes?.let { n ->
                it[notes] = n
            }
        }[Orders.id]
    }

    suspend fun getOrders(id: Long): List<ExposedOrder> {
        return dbQuery {
            Orders
                .selectAll()
                .where { Orders.userId eq id }
                .sortedByDescending { it[Orders.created] }
                .map {
                    it.toOrder()
                }
        }
    }

    suspend fun getUnreadOrders(id: Long): List<ExposedOrder> {
        return dbQuery {
            Orders
                .selectAll()
                .where { Orders.userId eq id and Orders.isNew.eq(true) }
                .map {
                    it.toOrder()
                }
        }
    }

    suspend fun getOrder(userID: Long, id: Long?): ExposedOrder? {
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

    suspend fun getUserID(id: Long?): Long? {
        id ?: return null
        return dbQuery {
            Orders
                .selectAll()
                .where {
                    Orders.id.eq(id)
                }
                .map {
                    it.toOrder().userID
                }
                .singleOrNull()
        }
    }

    suspend fun getAllOrders(filter: String?, start: Long, end: Long): List<ExposedOrder> {
        return dbQuery {
            // افزودن شرط‌های لازم به صورت ترکیبی
            Orders
                .selectAll()
                .where {
                    listOfNotNull(
                        filter?.let { Orders.status eq it },
                        start
                            .takeIf { it > 0 }
                            ?.let { Orders.created greaterEq it },
                        end
                            .takeIf { it > 0 }
                            ?.let { Orders.created lessEq it }
                    ).reduceOrNull { acc, condition -> acc and condition } ?: Op.TRUE
                }
                .map {
                    it.toOrder(fillAdmin = true)
                }
                .sortedWith(compareBy<ExposedOrder> { it.isAdminNew != true }.thenBy { it.created })
        }
    }

    suspend fun adminGetOrder(id: Long?): ExposedOrder? {
        id ?: return null
        return dbQuery {
            Orders
                .selectAll()
                .where {
                    Orders.id.eq(id)
                }
                .map {
                    it.toOrder(fillAdmin = true)
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
                it[age] = order.age
                it[feetWidth] = order.feetWidth
                it[feetLength] = order.feetLength
                it[feetSize] = order.feetSize
                it[gender] = order.gender
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
enum class Foots {
    FOOTS(),
    KNEE(),
}

@Serializable
enum class Angles {
    RIGHT,
    LEFT,
    UPSIDE,
    BACK,
    FRONT,
}