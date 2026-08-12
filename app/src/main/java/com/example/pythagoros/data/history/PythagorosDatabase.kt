package com.example.pythagoros.data.history

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [HistoryEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class PythagorosDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var instance: PythagorosDatabase? = null

        fun get(context: Context): PythagorosDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PythagorosDatabase::class.java,
                    "pythagoros.db",
                ).build().also { instance = it }
            }
    }
}
