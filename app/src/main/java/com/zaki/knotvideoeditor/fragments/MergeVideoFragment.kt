package com.zaki.knotvideoeditor.fragments

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.zaki.knotvideoeditor.R
import com.zaki.knotvideoeditor.editor.VideoEditor
import com.zaki.knotvideoeditor.utils.Constants
import com.zaki.knotvideoeditor.utils.KnotUtils
import com.zaki.knotvideoeditor.utils.interfaces.DialogHelper
import com.zaki.knotvideoeditor.utils.interfaces.FFMpegCallback
import java.io.File

/**
 * Developer : Mohammad Zaki
 * Created On : 01-08-2020
 */

class MergeVideoFragment : BottomSheetDialogFragment(),FFMpegCallback,DialogHelper {

    private var tagName: String = MergeVideoFragment::class.java.simpleName
    private lateinit var rootView: View
    private lateinit var ivClose: ImageView
    private lateinit var ivDone: ImageView
    private lateinit var ivVideoOne: SimpleDraweeView
    private lateinit var ivVideoTwo: SimpleDraweeView
    private var videoFileOne: File? = null
    private var videoFileTwo: File? = null
    private var bmThumbnailOne: Bitmap? = null
    private var bmThumbnailTwo: Bitmap? = null
    private var helper: BaseCreatorDialogFragment.CallBacks? = null
    private var mContext: Context? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.layout_merge_fragment, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ivClose = rootView.findViewById(R.id.iv_close)
        ivDone = rootView.findViewById(R.id.iv_done)
        ivVideoOne = rootView.findViewById(R.id.iv_video_one)
        ivVideoTwo = rootView.findViewById(R.id.iv_video_two)

        mContext = context

        ivClose.setOnClickListener {
            dismiss()
        }

        ivDone.setOnClickListener {

            if (videoFileOne != null && videoFileTwo != null) {
                dismiss()

                //output file is generated and send to video processing
                val outputFile = KnotUtils.createVideoFile(context!!)
                Log.v(tagName, "outputFile: ${outputFile.absolutePath}")

                VideoEditor.with(context!!)
                    .setType(Constants.MERGE_VIDEO)
                    .setFile(videoFileOne!!)
                    .setFileTwo(videoFileTwo!!)
                    .setOutputPath(outputFile.path)
                    .setCallback(this)
                    .main()

                helper?.showLoading(true)
            } else {
                Toast.makeText(activity!!, getString(R.string.error_merge),Toast.LENGTH_LONG).show()
            }
        }

        ivVideoOne.setOnClickListener {
            checkPermission(Constants.VIDEO_MERGE_1, Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        ivVideoTwo.setOnClickListener {
            checkPermission(Constants.VIDEO_MERGE_2, Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun checkPermission(requestCode: Int, permission: String) {
        requestPermissions(arrayOf(permission), requestCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Constants.VIDEO_MERGE_1 -> {
                for (permission in permissions) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(activity as Activity, permission)) {
                        Toast.makeText(activity, "Permission Denied", Toast.LENGTH_SHORT).show()
                    } else {
                        if (ActivityCompat.checkSelfPermission(activity as Activity, permission) == PackageManager.PERMISSION_GRANTED) {
                            //call the gallery intent
                            val i = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                            i.setType("video/*")
                            i.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/*", "video/*"))
                            startActivityForResult(i, Constants.VIDEO_MERGE_1)
                        } else {
                            callPermissionSettings()
                        }
                    }
                }
                return
            }

            Constants.VIDEO_MERGE_2 -> {
                for (permission in permissions) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(activity as Activity, permission)) {
                        Toast.makeText(activity, "Permission Denied", Toast.LENGTH_SHORT).show()
                    } else {
                        if (ActivityCompat.checkSelfPermission(activity as Activity, permission) == PackageManager.PERMISSION_GRANTED) {
                            //call the gallery intent
                            val i = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                            i.setType("video/*")
                            i.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/*", "video/*"))
                            startActivityForResult(i, Constants.VIDEO_MERGE_2)
                        } else {
                            callPermissionSettings()
                        }
                    }
                }
                return
            }
        }
    }

    private fun callPermissionSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", context!!.applicationContext.packageName, null)
        intent.data = uri
        startActivityForResult(intent, 300)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_CANCELED) return

        when (requestCode) {
            Constants.VIDEO_MERGE_1 -> {
                data?.let {
                    setFilePath(resultCode, it, Constants.VIDEO_MERGE_1)
                }
            }

            Constants.VIDEO_MERGE_2 -> {
                data?.let {
                    setFilePath(resultCode, it, Constants.VIDEO_MERGE_2)
                }
            }
        }
    }

    private fun setFilePath(resultCode: Int, data: Intent, mode: Int) {

        if (resultCode == Activity.RESULT_OK) {
            try {
                val selectedImage = data.data
                //  Log.e("selectedImage==>", "" + selectedImage)
                val filePathColumn = arrayOf(MediaStore.MediaColumns.DATA)
                val cursor = context!!.contentResolver
                    .query(selectedImage!!, filePathColumn, null, null, null)
                if (cursor != null) {
                    cursor.moveToFirst()
                    val columnIndex = cursor
                        .getColumnIndex(filePathColumn[0])
                    val filePath = cursor.getString(columnIndex)
                    cursor.close()
                    if (mode == Constants.VIDEO_MERGE_1) {
                        videoFileOne = File(filePath)
                        Log.v(tagName, "videoFileOne: " + videoFileOne!!.absolutePath)

                        //get thumbnail of selected video
                        bmThumbnailOne = ThumbnailUtils.createVideoThumbnail(
                            videoFileOne!!.absolutePath,
                            MediaStore.Images.Thumbnails.FULL_SCREEN_KIND
                        )

                        ivVideoOne.setImageBitmap(bmThumbnailOne)
                    } else if (mode == Constants.VIDEO_MERGE_2) {
                        videoFileTwo = File(filePath)
                        Log.v(tagName, "videoFileTwo: " + videoFileTwo!!.absolutePath)

                        //get thumbnail of selected video
                        bmThumbnailTwo = ThumbnailUtils.createVideoThumbnail(
                            videoFileTwo!!.absolutePath,
                            MediaStore.Video.Thumbnails.FULL_SCREEN_KIND
                        )

                        ivVideoTwo.setImageBitmap(bmThumbnailTwo)
                    }
                }
            } catch (e: Exception) {
                Log.e(tagName, "Exception: ${e.localizedMessage}")
            }
        }
    }

    companion object {
        fun newInstance() = MergeVideoFragment()
    }

    override fun onProgress(progress: String) {
        Log.v(tagName, "onProgress()")
    }

    override fun onSuccess(convertedFile: File, type: String) {
        Log.v(tagName, "onSuccess()")
        helper?.showLoading(false)
        helper?.onFileProcessed(convertedFile)
    }

    override fun onFailure(error: Exception) {
        Log.v(tagName, "onFailure() ${error.localizedMessage}")
        Toast.makeText(mContext, "Video processing failed", Toast.LENGTH_LONG).show()
        helper?.showLoading(false)
    }

    override fun onNotAvailable(error: Exception) {
        Log.v(tagName, "onNotAvailable() ${error.localizedMessage}")
    }

    override fun onFinish() {
        Log.v(tagName, "onFinish()")
        helper?.showLoading(false)
    }

    override fun setMode(mode: Int) {

    }

    override fun setFilePathFromSource(file: File) {

    }

    override fun setHelper(helper: BaseCreatorDialogFragment.CallBacks) {
        this.helper = helper
    }

    override fun setDuration(duration: Long) {

    }

}