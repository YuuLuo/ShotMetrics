package com.shotmetrics.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.shotmetrics.app.data.local.entity.ImpactEntity
import com.shotmetrics.app.data.local.entity.SessionEntity

@Database(
    entities = [SessionEntity::class, ImpactEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao

    companion object {
        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "shotmetrics.db"
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
