package com.example.calendarenhancer.network

import com.example.calendarenhancer.data.GithubRelease
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface GithubApi {
    @GET("repos/Lovely-Ruby/calendar-enhancer/releases/latest")
    suspend fun getLatestRelease(): GithubRelease

    @Streaming
    @GET
    suspend fun downloadFile(@Url url: String): ResponseBody
}
