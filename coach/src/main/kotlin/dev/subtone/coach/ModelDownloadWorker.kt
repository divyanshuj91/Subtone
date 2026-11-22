package dev.subtone.coach

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Worker Domain 3: Background Model Download Worker.
 * - HTTP Range headers for resumable downloads
 * - Reports progress (0..100) via setProgress
 * - SHA-256 checksum verification
 * - Atomic rename from cache/tmp to files/models/
 */
@HiltWorker
class ModelDownloadWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val modelUrlStr = inputData.getString(KEY_MODEL_URL) ?: return@withContext Result.failure()
        val expectedSha256 = inputData.getString(KEY_EXPECTED_SHA256) ?: return@withContext Result.failure()
        val targetFilename = inputData.getString(KEY_TARGET_FILENAME) ?: "model.bin"

        val tmpDir = File(appContext.cacheDir, "tmp").apply { mkdirs() }
        val tmpFile = File(tmpDir, "$targetFilename.download")

        val modelsDir = File(appContext.filesDir, "models").apply { mkdirs() }
        val finalFile = File(modelsDir, targetFilename)

        try {
            var downloadedBytes = if (tmpFile.exists()) tmpFile.length() else 0L

            val url = URL(modelUrlStr)
            val connection = url.openConnection() as HttpURLConnection
            if (downloadedBytes > 0) {
                connection.setRequestProperty("Range", "bytes=$downloadedBytes-")
            }
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.connect()

            val responseCode = connection.responseCode
            val isPartial = responseCode == HttpURLConnection.HTTP_PARTIAL
            val isOk = responseCode == HttpURLConnection.HTTP_OK

            if (!isPartial && !isOk) {
                return@withContext Result.retry()
            }

            val totalBytes = if (isPartial) {
                downloadedBytes + connection.contentLengthLong
            } else {
                downloadedBytes = 0L
                connection.contentLengthLong
            }

            connection.inputStream.use { input ->
                FileOutputStream(tmpFile, isPartial).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (totalBytes > 0) {
                            val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                            setProgress(workDataOf(KEY_PROGRESS to progress))
                        }
                    }
                }
            }

            // Verify SHA-256 Checksum
            val calculatedSha256 = computeSha256(tmpFile)
            if (!calculatedSha256.equals(expectedSha256, ignoreCase = true)) {
                tmpFile.delete()
                return@withContext Result.failure(workDataOf("error" to "Checksum mismatch"))
            }

            // Atomic move to final destination
            if (finalFile.exists()) finalFile.delete()
            val moved = tmpFile.renameTo(finalFile)
            if (!moved) {
                tmpFile.copyTo(finalFile, overwrite = true)
                tmpFile.delete()
            }

            Result.success(workDataOf(KEY_MODEL_PATH to finalFile.absolutePath))
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buf = ByteArray(64 * 1024)
            var n: Int
            while (stream.read(buf).also { n = it } != -1) {
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val KEY_MODEL_URL = "model_url"
        const val KEY_EXPECTED_SHA256 = "expected_sha256"
        const val KEY_TARGET_FILENAME = "target_filename"
        const val KEY_PROGRESS = "progress"
        const val KEY_MODEL_PATH = "model_path"
    }
}
