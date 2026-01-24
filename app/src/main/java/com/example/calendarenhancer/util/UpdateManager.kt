package com.example.calendarenhancer.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.calendarenhancer.BuildConfig
import com.example.calendarenhancer.data.GithubRelease
import com.example.calendarenhancer.network.GithubApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object UpdateManager {
    private const val BASE_URL = "https://api.github.com/"
    
    private val api: GithubApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GithubApi::class.java)
    }

    suspend fun checkUpdate(): GithubRelease? {
        return try {
            val release = api.getLatestRelease()
            val currentVersion = BuildConfig.VERSION_NAME
            // Remove 'v' prefix if exists for comparison
            val remoteVersion = release.tagName.removePrefix("v")
            
            if (isNewerVersion(currentVersion, remoteVersion)) {
                release
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun isNewerVersion(current: String, remote: String): Boolean {
        // Simple string comparison for now, or split by dots for semantic versioning
        // Assuming semantic versioning X.Y.Z
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val remoteParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
        
        val length = maxOf(currentParts.size, remoteParts.size)
        for (i in 0 until length) {
            val c = if (i < currentParts.size) currentParts[i] else 0
            val r = if (i < remoteParts.size) remoteParts[i] else 0
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    fun downloadApk(context: Context, url: String, fileName: String): Flow<Float> = flow {
        try {
            val responseBody = api.downloadFile(url)
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            
            saveFile(responseBody, file) { progress ->
                emit(progress)
            }
            
            emit(1.0f) // Done
            installApk(context, file)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun saveFile(body: ResponseBody, file: File, onProgress: suspend (Float) -> Unit) {
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        
        try {
            val fileSize = body.contentLength()
            inputStream = body.byteStream()
            outputStream = FileOutputStream(file)
            
            val data = ByteArray(4096)
            var count: Int
            var total: Long = 0
            
            while (inputStream.read(data).also { count = it } != -1) {
                total += count
                outputStream.write(data, 0, count)
                if (fileSize > 0) {
                    onProgress(total.toFloat() / fileSize)
                }
            }
            outputStream.flush()
        } finally {
            inputStream?.close()
            outputStream?.close()
        }
    }

    private fun installApk(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
