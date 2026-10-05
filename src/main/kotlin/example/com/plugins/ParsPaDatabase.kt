package example.com.plugins

import example.com.isDebug
import org.jetbrains.exposed.sql.Database

private const val url: String = "localhost"
private const val username: String = "parspa"
private const val password: String = "G]=dvbyk}T%pb"
private val database: String
    get() = /*if (isDebug) "parspa_dev" else*/ "parspa"

object ParsPaDatabase {
    fun connectDatabase(): Database {
        return Database.connect(
            url = "jdbc:mariadb://$url:3306/$database?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Tehran",
            driver = "org.mariadb.jdbc.Driver",
            user = username,
            password = password,
        )
    }
}