package com.zaki.knotvideoeditor.utils

import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.Environment
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.FileDataSource
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Developer : Mohammad Zaki
 * Created On : 26-07-2020
 */

object KnotUtils {

    fun secToTime(totalSeconds: Long): String {
        return String.format(
            "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(totalSeconds),
            TimeUnit.MILLISECONDS.toMinutes(totalSeconds) -
                    TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(totalSeconds)), // The change is in this line
            TimeUnit.MILLISECONDS.toSeconds(totalSeconds) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(totalSeconds))
        )
    }

    fun createVideoFile(context: Context): File {
        val timeStamp: String = SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault()).format(
            Date()
        )
        val imageFileName: String = Constants.APP_NAME + timeStamp + "_"
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        if (storageDir != null) {
            if (!storageDir.exists()) storageDir.mkdirs()
        }
        return File.createTempFile(imageFileName, Constants.VIDEO_FORMAT, storageDir)
    }

    fun refreshGalleryAlone(context: Context) {
        try {
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            context.sendBroadcast(mediaScanIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //write intent data into file
    fun writeIntoFile(
        context: Context,
        data: Intent,
        file: File?
    ): File? {
        var videoAsset: AssetFileDescriptor? = null
        try {
            videoAsset = context.contentResolver.openAssetFileDescriptor(data.data!!, "r")
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        val `in`: FileInputStream
        try {
            `in` = videoAsset!!.createInputStream()
            var out: OutputStream? = null
            out = FileOutputStream(file)
            // Copy the bits from instream to outstream
            val buf = ByteArray(1024)
            var len: Int
            while (`in`.read(buf).also { len = it } > 0) {
                out.write(buf, 0, len)
            }
            `in`.close()
            out.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return file
    }

    fun buildMediaSource(uri: Uri, fromWho: String, userAgent: String = ""): MediaSource? {

        when (fromWho) {

            VideoFrom.LOCAL -> {

                val dataSpec = DataSpec(uri)

                val fileDataSource = FileDataSource()
                try {
                    fileDataSource.open(dataSpec)
                } catch (e: FileDataSource.FileDataSourceException) {
                    e.printStackTrace()
                }
                val factory = object : DataSource.Factory {
                    override fun createDataSource(): DataSource {
                        return fileDataSource
                    }
                }

                return ExtractorMediaSource.Factory(factory).createMediaSource(fileDataSource.uri)
            }

            VideoFrom.REMOTE -> {
                return ExtractorMediaSource.Factory(
                    DefaultHttpDataSourceFactory(userAgent)
                ).createMediaSource(uri)
            }

            else -> {
                return null
            }

        }
    }
}

class VideoFrom {
    companion object {
        const val LOCAL = "LOCAL"
        const val REMOTE = "REMOTE"
    }
}

