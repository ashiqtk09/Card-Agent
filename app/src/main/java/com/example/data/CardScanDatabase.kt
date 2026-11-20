package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ContactEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class CardScanDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao

    companion object {
        @Volatile
        private var instance: CardScanDatabase? = null

        fun getInstance(context: Context): CardScanDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CardScanDatabase::class.java,
                    "cardscan.db",
                ).build().also { instance = it }
            }
        }
    }
}
