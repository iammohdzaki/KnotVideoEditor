package com.zaki.knotvideoeditor.utils.interfaces

import java.io.File

/**
 * Developer : Mohammad Zaki
 * Created On : 01-08-2020
 */

interface FFMpegCallback {
    fun onProgress(progress: String)

    fun onSuccess(convertedFile: File, type: String)

    fun onFailure(error: Exception)

    fun onNotAvailable(error: Exception)

    fun onFinish()
}