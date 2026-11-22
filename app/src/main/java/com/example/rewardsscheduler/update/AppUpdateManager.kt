package com.example.rewardsscheduler.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.rewardsscheduler.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

@Serializable
data class UpdateManifest(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val changelog: String = ""
)

/**
 * Handles checking for updates, downloading the APK into cache,
 * and prompting the user to install the update in-place without data loss.
 */
class AppUpdateManager(private val context: Context) {

    companion object {
        private const val TAG = "AppUpdateManager"
        private const val UPDATE_MANIFEST_URL =
            "https://raw.githubusercontent.com/JUST-RELAXX/RewardsSchedulerForAndroid/main/version.json"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Checks whether an update is available on GitHub / backend.
     * Returns the UpdateManifest if a newer versionCode exists, or null otherwise.
     */
    suspend fun checkForUpdate(): UpdateManifest? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(UPDATE_MANIFEST_URL)
                .header("Cache-Control", "no-cache")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Update check HTTP failed: ${response.code}")
                return@withContext null
            }

            val bodyString = response.body?.string() ?: return@withContext null
            val manifest = json.decodeFromString<UpdateManifest>(bodyString)

            if (manifest.versionCode > BuildConfig.VERSION_CODE) {
                Log.i(TAG, "Update available: current=${BuildConfig.VERSION_CODE}, server=${manifest.versionCode}")
                manifest
            } else {
                Log.i(TAG, "App is up to date: version=${BuildConfig.VERSION_CODE}")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking for update: ${e.message}")
            null
        }
    }

    /**
     * Downloads the APK file to cache with progress callbacks, then launches the installer.
     */
    suspend fun downloadAndInstall(
        manifest: UpdateManifest,
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val destinationFile = File(updateDir, "RewardsScheduler-v${manifest.versionName}.apk")

            val request = Request.Builder()
                .url(manifest.apkUrl)
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP error ${response.code} downloading APK"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty response body"))
            val contentLength = body.contentLength()

            body.byteStream().use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (contentLength > 0) {
                            val progress = (totalRead.toFloat() / contentLength).coerceIn(0f, 1f)
                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        }
                    }
                    output.flush()
                }
            }

            // Launch system package installer
            withContext(Dispatchers.Main) {
                launchInstaller(destinationFile)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download and install update: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun launchInstaller(apkFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val manageIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(manageIntent)
                }
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer: ${e.message}", e)
        }
    }
}
