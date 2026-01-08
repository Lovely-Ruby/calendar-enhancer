package com.example.calendarenhancer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendarenhancer.data.BirthdayEntity
import com.example.calendarenhancer.data.DatabaseProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class BirthdayViewModel(application: Application) : AndroidViewModel(application) {
    // 拿到 DAO
    private val dao = DatabaseProvider.get(application).birthdayDao()

    // 暴露给 UI 的数据流
    val allBirthdays: Flow<List<BirthdayEntity>> = dao.getAll()

    // 封装新增/修改业务
    fun saveBirthday(id: Int, name: String, dateStr: String) {
        viewModelScope.launch {
            val entity = BirthdayEntity(id = id, name = name, dateStr = dateStr)
            if (id == 0) dao.insert(entity) else dao.update(entity)
        }
    }

    // 封装删除业务
    fun deleteBirthday(entity: BirthdayEntity) {
        viewModelScope.launch {
            dao.delete(entity)
        }
    }
}