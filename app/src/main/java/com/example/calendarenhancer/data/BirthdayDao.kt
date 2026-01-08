package com.example.calendarenhancer.data // 确保包名和你文件夹路径一致
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow // 用于 Flow 响应式数据
// 注意：BirthdayEntity 如果在同一个文件夹下不需要导入，否则需要导入它
@Dao
interface BirthdayDao {
    @Query("SELECT * FROM birthdays")
    fun getAll(): Flow<List<BirthdayEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(person: BirthdayEntity)
    @Delete
    suspend fun delete(person: BirthdayEntity)
    @Update
    suspend fun update(person: BirthdayEntity)
}
