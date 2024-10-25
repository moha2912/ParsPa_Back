package example.com.data.model.res

import example.com.data.schema.ExposedFinance
import kotlinx.serialization.Serializable

@Serializable
data class FinancesResponse(
    val msg: String,
    val amount: Long,
    val count: Int,
    val finances: List<ExposedFinance>,
)
