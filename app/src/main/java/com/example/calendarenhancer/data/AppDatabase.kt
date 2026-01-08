package com.example.calendarenhancer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BirthdayEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun birthdayDao(): BirthdayDao
}

// 单例模式，确保只创建一个数据库实例
object DatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "birthday_db"
            )
                .fallbackToDestructiveMigration() // 当数据库结构改变时，允许清空旧数据重建
                .build()
                .also { instance = it }
        }
    }
}