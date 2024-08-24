package example.com.plugins

import org.jetbrains.exposed.sql.Database

private const val url: String = "localhost"
private const val database: String = "parspa"
private const val username: String = "parspa"
private const val password: String = "G]=+\$yk}T%pb"

object ParsPaDatabase {
    fun connectDatabase(): Database {
        //TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_REPEATABLE_READ
        return Database.connect(
            url = "jdbc:mysql://$url:3306/$database?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Tehran",
            driver = "com.mysql.cj.jdbc.Driver",
            user = username,
            password = password,
        )
    }
}