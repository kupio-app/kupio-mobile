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

    private val Migration2To3 = object : Migration(2, 3) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSql(
                """
                CREATE TABLE IF NOT EXISTS `cached_categories` (
                    `id` INTEGER NOT NULL,
                    `name` TEXT NOT NULL,
                    `iconSlug` TEXT,
                    `depth` INTEGER NOT NULL,
                    `parentId` INTEGER,
                    `updatedAtMs` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            connection.execSql("CREATE INDEX IF NOT EXISTS `index_cached_categories_parentId` ON `cached_categories` (`parentId`)")
            connection.execSql("CREATE INDEX IF NOT EXISTS `index_cached_categories_depth` ON `cached_categories` (`depth`)")
            connection.execSql(
                """
                CREATE TABLE IF NOT EXISTS `cached_category_filters` (
                    `id` INTEGER NOT NULL,
                    `categoryId` INTEGER NOT NULL,
                    `slug` TEXT NOT NULL,
                    `label` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `optionsJson` TEXT NOT NULL,
                    `isRequired` INTEGER NOT NULL,
                    `displayOrder` INTEGER NOT NULL,
                    `updatedAtMs` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            connection.execSql("CREATE INDEX IF NOT EXISTS `index_cached_category_filters_categoryId` ON `cached_category_filters` (`categoryId`)")
        }
    }

    val All = arrayOf(Migration1To2, Migration2To3)
}

private fun SQLiteConnection.execSql(sql: String) {
    prepare(sql).use { statement ->
        statement.step()
    }
}
