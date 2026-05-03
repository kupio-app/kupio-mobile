package kupio.mobile.core.offline.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun getKupioDatabaseBuilder(context: Context): RoomDatabase.Builder<KupioDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("kupio_offline.db")
    return Room.databaseBuilder<KupioDatabase>(
        context = appContext,
        name = dbFile.absolutePath,
    )
}
