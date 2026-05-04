package kupio.mobile.core.offline.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection

internal object KupioDatabaseMigrations {
    private val Migration1To2 = object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSql(
                """
                CREATE TABLE IF NOT EXISTS `cached_authenticated_user` (
                    `id` TEXT NOT NULL,
                    `username` TEXT,
                    `displayName` TEXT,
                    `email` TEXT NOT NULL,
                    `role` TEXT NOT NULL,
                    `needsUsername` INTEGER NOT NULL,
                    `balance` INTEGER NOT NULL,
                    `avatarUrl` TEXT,
                    `createdAt` TEXT,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
        }
    }

    val All = arrayOf(Migration1To2)
}

private fun SQLiteConnection.execSql(sql: String) {
    prepare(sql).use { statement ->
        statement.step()
    }
}
