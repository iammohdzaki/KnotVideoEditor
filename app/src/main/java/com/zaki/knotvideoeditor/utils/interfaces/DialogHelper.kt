package com.zaki.knotvideoeditor.utils.interfaces

import com.zaki.knotvideoeditor.fragments.BaseCreatorDialogFragment
import java.io.File

/**
 * Developer : Mohammad Zaki
 * Created On : 01-08-2020
 */

interface DialogHelper {
    fun setHelper(helper: BaseCreatorDialogFragment.CallBacks)
    fun setMode(mode: Int)
    fun setFilePathFromSource(file: File)
    fun setDuration(duration: Long)
}