package app.bot

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.handlers.TextHandlerEnvironment
import com.github.kotlintelegrambot.entities.ChatId
import example.com.routes.InsoleRequest

private const val SELF_ID = 5067903470L
private const val MOHA_ID = 548307881L
private val ADMIN_IDs = listOf(SELF_ID, MOHA_ID)

object TelegramBot {
    private var parsBot: Bot = bot {
        token = "6509555495:AAFdozEIVTHvodD82fz9u2HBgGEAM3rh4lw"
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
    }

    fun sendError(e: Throwable) {
        e.printStackTrace()
        sendMessage(e.stackTraceToString())
    }

    fun sendMessage(s: String) {
        ADMIN_IDs.forEach {
            //parsBot.sendMessage(ChatId.fromId(it), s, parseMode = ParseMode.MARKDOWN_V2)
        }
    }

    fun sendRawMessage(s: String) {
        ADMIN_IDs.forEach {
            //parsBot.sendMessage(ChatId.fromId(it), s)
        }
    }

    fun sendCreateOrder(id: Long) {
        val message = buildString {
            appendLine("ثبت سفارش جدید")
            append("شماره سفارش: ")
            appendLine(id)
        }
        ADMIN_IDs.forEach {
            //parsBot.sendMessage(ChatId.fromId(it), message)
        }
    }

    fun sendInsoleOrder(order1: InsoleRequest) {
        val message = buildString {
            appendLine("سفارش کفی")
            append("شماره سفارش: ")
            appendLine(order1.orderID)
            append("تعداد کفی: ")
            appendLine(order1.count)
        }
        ADMIN_IDs.forEach {
            //parsBot.sendMessage(ChatId.fromId(it), message)
        }
    }

    fun sendMessageStatus(s: String) {
        ADMIN_IDs.forEach {
            //parsBot.sendMessage(ChatId.fromId(it), s)
        }
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