package com.example.calendarenhancer.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import java.util.TimeZone

object CalendarUtil {

    // 获取系统默认的主日历 ID
    private fun getPrimaryCalendarId(context: Context): Long {
        var calendarId = -1L
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY
        )
        // 查询可用的日历账户，优先查找 IS_PRIMARY = 1 的
        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.VISIBLE} = 1 AND ${CalendarContract.Calendars.IS_PRIMARY} = 1",
            null,
            CalendarContract.Calendars._ID
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val idIndex = it.getColumnIndex(CalendarContract.Calendars._ID)
                if (idIndex != -1) {
                    calendarId = it.getLong(idIndex)
                }
            }
        }

        // 如果没有找到 Primary，尝试找第一个可见的日历
        if (calendarId == -1L) {
             val fallbackCursor = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.VISIBLE} = 1",
                null,
                CalendarContract.Calendars._ID
            )
            fallbackCursor?.use {
                if (it.moveToFirst()) {
                    val idIndex = it.getColumnIndex(CalendarContract.Calendars._ID)
                    if (idIndex != -1) {
                        calendarId = it.getLong(idIndex)
                    }
                }
            }
        }

        return calendarId
    }

    fun addCalendarEvent(context: Context, title: String, description: String, startTimeMillis: Long) {
        val calendarId = getPrimaryCalendarId(context)
        if (calendarId == -1L) {
            Toast.makeText(context, "未找到可用的日历账户", Toast.LENGTH_SHORT).show()
            return
        }

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startTimeMillis)
            put(CalendarContract.Events.DTEND, startTimeMillis + 60 * 60 * 1000) // 默认持续1小时
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            // 设为全天事件可能更好，看需求，这里暂时设为普通定时事件
            put(CalendarContract.Events.ALL_DAY, 1) 
        }

        try {
            val uri: Uri? = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            if (uri != null) {
                Toast.makeText(context, "已成功添加到日历: $title", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "添加到日历失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "添加出错: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
