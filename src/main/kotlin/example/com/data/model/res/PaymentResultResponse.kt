package example.com.data.model.res

import example.com.data.model.ZibalVerifyResponse
import example.com.data.schema.FinanceState
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PaymentResultResponse(
    val msg: FinanceState,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val successDate: Long? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val zibal: ZibalVerifyResponse? = null
)
