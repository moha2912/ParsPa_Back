package example.com.plugins

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.handlers.TextHandlerEnvironment
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.mfathi91.time.PersianDate
import example.com.data.model.Strings
import example.com.data.model.ZibalVerifyResponse
import example.com.data.schema.ExposedFinance
import example.com.data.schema.ExposedUser
import example.com.isDebug
import example.com.routes.InsoleRequest
import io.ktor.client.engine.*
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter

private const val SELF_ID = 5067903470L
private const val MOHA_ID = 548307881L
private const val LOGS_CHANNEL = -1002346633099L

object TelegramBot {

    private var parsBot: Bot = bot {
        token = "6509555495:AAFdozEIVTHvodD82fz9u2HBgGEAM3rh4lw"
        proxy = ProxyBuilder.socks("127.0.0.1", 8081)
        dispatch {
            command("start") {
                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = "سلام کاربر ادمین!\n\nپیام های مربوط به سفارشات جدید یا خطاهای سرور در اینجا ارسال خواهند شد."
                )
            }
        }
    }

    fun prepare() {
        parsBot.startPolling()
        val s = buildString {
            append("*")
            append("راه اندازی سرور")
            append("*")
            appendLine()
            append("\\#استارت")
        }
        sendTelegramMessage(s)
    }

    fun sendError(e: Throwable) {
        e.printStackTrace()
        sendTelegramMessage(e.stackTraceToString())
    }

    fun sendRawMessage(s: String) {
        sendTelegramMessage(s)
    }

    fun sendLogin(phone: String, platform: Short?) {
        // TODO:  if (isDebug) return
        val message = buildString {
            append("*")
            appendLine("ورود جدید")
            append("*")
            appendLine()
            append("شماره همراه: ")
            appendLine(phone)
            append("پلتفرم: ")
            appendLine(if (platform?.toInt() == 1) "وب اپ" else "اپلیکیشن")
            appendLine()
            append("\\#ورود")
        }
        sendTelegramMessage(message)
    }

    fun sendCreateOrder(id: Long, phone: String?, platform: Short) {
        // TODO:  if (isDebug) return
        val message = buildString {
            append("*")
            appendLine("درخواست جدید")
            append("*")
            appendLine()
            append("شماره سفارش: ")
            appendLine(id)
            append("شماره همراه: ")
            appendLine(phone)
            append("پلتفرم: ")
            appendLine(if (platform.toInt() == 1) "وب اپ" else "اپلیکیشن")
            appendLine()
            append("\\#درخواست")
        }
        sendTelegramMessage(message)
    }

    fun sendVerifiedPayment(zibalVerify: ZibalVerifyResponse, finance: ExposedFinance, user: String?) {
        // TODO:  if (isDebug) return
        val insole = finance.insole
        val platform = finance.platform
        val dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        val message = buildString {
            append("*")
            appendLine("پرداخت سفارش کفی")
            append("*")
            appendLine()
            append("شماره سفارش: ")
            appendLine(insole.orderID)
            append("شماره همراه: ")
            appendLine(user)
            append("تعداد کفی: ")
            appendLine(insole.count)
            append("مبلغ: ")
            append(
                DecimalFormat
                    .getInstance()
                    .format((zibalVerify.amount ?: 10) / 10)
            )
            appendLine(" ${Strings.TOMAN}")
            append("تاریخ: ")
            appendLine(dtf.format(PersianDate.now()))
            append("پلتفرم: ")
            appendLine(if (platform.toInt() == 1) "وب اپ" else "اپلیکیشن")
            //append("```json")
            //appendLine(Json.encodeToString(zibalVerify))
            //append("```")
            appendLine()
            append("\\#پرداخت")
        }
        sendTelegramMessage(message)
    }

    fun sendCreatedPayment(user: ExposedUser, insoleRequest: InsoleRequest, amount: Long) {
        // TODO:  if (isDebug) return
        val message = buildString {
            appendLine("ایجاد درگاه پرداخت")
            append("شماره سفارش: ")
            appendLine(insoleRequest.orderID)
            append("تعداد کفی: ")
            appendLine(insoleRequest.count)
            append("مبلغ کل: ")
            appendLine(amount)
            append("\\#درگاه")
        }
        sendTelegramMessage(message)
    }

    private fun sendTelegramMessage(message: String) {
        val msg = message
            .plus("\n\\#")
            .plus(if (isDebug) "توسعه" else "محصول")
        parsBot.sendMessage(ChatId.fromId(LOGS_CHANNEL), "sdfgfv")
        parsBot.sendMessage(ChatId.fromId(SELF_ID), msg, parseMode = ParseMode.MARKDOWN_V2)
        parsBot.sendMessage(ChatId.fromId(LOGS_CHANNEL), msg, parseMode = ParseMode.MARKDOWN_V2)
    }

    // -----------------------------------------------------------------------

    fun sendMessageStatus(s: String) {
        sendTelegramMessage(s)
        //parsBot.editMessageText(ChatId.fromId(ADMIN_ID), 784, text = s, parseMode = ParseMode.MARKDOWN_V2)
    }

    fun endBot() {
        parsBot.stopPolling()
        parsBot.close()
    }

    private fun TextHandlerEnvironment.getMessage() {
        bot.sendMessage(ChatId.fromId(message.chat.id), text = text)
    }
}