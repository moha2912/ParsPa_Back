package example.com.plugins

import org.jetbrains.exposed.sql.Database

private const val url: String = "localhost"
private const val database: String = "selfmpi2_physio"
private const val username: String = "selfmpi2_physio"
private const val password: String = "G]=+\$yk}T%pb"

object ParsPaDatabase {
    fun connectDatabase(): Database {
        //TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_REPEATABLE_READ
        return Database.connect(
            url = "jdbc:mysql://$url:3306/$database?useSSL=false&serverTimezone=UTC",
            driver = "com.mysql.cj.jdbc.Driver",
            user = username,
            password = password,
        )
    }
}