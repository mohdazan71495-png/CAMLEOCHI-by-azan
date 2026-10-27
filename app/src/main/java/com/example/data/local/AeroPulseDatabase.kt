package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FieldStationEntity::class,
        ActivityProfileEntity::class,
        BarometerLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AeroPulseDatabase : RoomDatabase() {

    abstract fun aeroPulseDao(): AeroPulseDao

    companion object {
        @Volatile
        private var INSTANCE: AeroPulseDatabase? = null

        fun getInstance(context: Context): AeroPulseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AeroPulseDatabase::class.java,
                    "aeropulse_telemetry.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
