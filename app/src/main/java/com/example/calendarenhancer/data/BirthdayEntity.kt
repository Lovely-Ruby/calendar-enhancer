package com.example.calendarenhancer.data
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "birthdays")
data class BirthdayEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val dateStr: String
)