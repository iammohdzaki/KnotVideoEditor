package com.zaki.knotvideoeditor.fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.zaki.knotvideoeditor.R
import com.zaki.knotvideoeditor.fragments.adapter.FeaturesAdapter
import com.zaki.knotvideoeditor.utils.Constants
import com.zaki.knotvideoeditor.utils.KnotUtils
import com.zaki.knotvideoeditor.utils.PermissionHelper
import com.zaki.knotvideoeditor.utils.PermissionHelper.callPermissionSettings
import com.zaki.knotvideoeditor.utils.VideoFrom
import com.zaki.knotvideoeditor.utils.interfaces.FFMpegCallback
import com.zaki.knotvideoeditor.utils.interfaces.OnFeaturesClickListener
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.fragment_main.view.*
import kotlinx.android.synthetic.main.toolbar_common.*
import kotlinx.android.synthetic.main.toolbar_common.view.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainFragment : Fragment(), View.OnClickListener,OnFeaturesClickListener ,FFMpegCallback,BaseCreatorDialogFragment.CallBacks{
    private var videoFile: File? = null
    private var videoUri: Uri? = null
    private var masterVideoFile: File? = null
    private var tagName: String = MainFragment::class.java.simpleName
    private var ePlayer: PlayerView? = null
    private var exoPlayer: SimpleExoPlayer? = null
    private var videoOptions: ArrayList<String> = ArrayList()
    private var builder: AlertDialog.Builder ?= null
    var dialog: Dialog ?= null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ePlayer = view?.findViewById(R.id.ePlayer)

        //load FFmpeg
        try {
            FFmpeg.getInstance(activity).loadBinary(object : FFmpegLoadBinaryResponseHandler {
                override fun onFailure() {
                    Log.v("FFMpeg", "Failed to load FFMpeg library.")
                }

                override fun onSuccess() {
                    Log.v("FFMpeg", "FFMpeg Library loaded!")
                }

                override fun onStart() {
                    Log.v("FFMpeg", "FFMpeg Started")
                }

                override fun onFinish() {
                    Log.v("FFMpeg", "FFMpeg Stopped")
                }
            })
        } catch (e: FFmpegNotSupportedException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        builder = AlertDialog.Builder(activity)
        builder!!.setView(R.layout.progress)
        dialog = builder!!.create()

        view.ivCamera.setOnClickListener(this)
        view.ivGallery.setOnClickListener(this)
        view.ivRightImage.setOnClickListener(this)
        setFeatures()
    }

    private fun setFeatures(){
        videoOptions.add(Constants.TRIM)
        videoOptions.add(Constants.PLAYBACK)
        /*videoOptions.add(Constants.MUSIC)
        videoOptions.add(Constants.TEXT)
        videoOptions.add(Constants.OBJECT)*/
        videoOptions.add(Constants.MERGE)

        rvVideoOptions.apply {
            layoutManager = LinearLayoutManager(context,LinearLayoutManager.HORIZONTAL,false)
            adapter = FeaturesAdapter(videoOptions,context,this@MainFragment)
        }
    }


    override fun onClick(view: View?) {
        when (view?.id) {
            ivCamera.id -> {
                openCamera(Constants.PERMISSION_CAMERA)
            }
            ivGallery.id -> {
                openGallery(Constants.PERMISSION_STORAGE)
            }
            ivRightImage.id -> {
                AlertDialog.Builder(context!!)
                    .setTitle(Constants.APP_NAME)
                    .setMessage(getString(R.string.save_video))
                    .setPositiveButton(getString(R.string.Continue)) { _, _ ->
                        if (masterVideoFile != null) {
                            val outputFile = createSaveVideoFile()
                            KnotUtils.copyFile(masterVideoFile, outputFile)
                            Toast.makeText(context, R.string.successfully_saved, Toast.LENGTH_SHORT).show()
                            KnotUtils.refreshGallery(outputFile.absolutePath, context!!)
                            //ivRightImage!!.visibility = View.GONE
                        }
                    }
                    .setNegativeButton(R.string.cancel) { _, _ -> }
                    .show()
            }
        }
    }

    private fun createSaveVideoFile(): File {
        val timeStamp: String = SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault()).format(Date())
        val imageFileName: String = Constants.APP_NAME + timeStamp + "_"

        val path =
            Environment.getExternalStorageDirectory().toString() + File.separator + Constants.APP_NAME + File.separator + Constants.MY_VIDEOS + File.separator
        val folder = File(path)
        if (!folder.exists())
            folder.mkdirs()

        return File.createTempFile(imageFileName, Constants.VIDEO_FORMAT, folder)
    }

    override fun videoOptions(option: String) {
        when(option){
            Constants.TRIM -> {
                masterVideoFile?.let {file ->
                    val trimFragment = VideoTrimFragment()
                    trimFragment.setHelper(this@MainFragment)
                    trimFragment.setFilePathFromSource(file, exoPlayer?.duration!!)
                    showBottomSheetDialogFragment(trimFragment)
                }

                if (masterVideoFile == null) {
                    Toast.makeText(context,getString(R.string.error_crop),Toast.LENGTH_LONG).show()
                }
            }
            Constants.MERGE -> {
                MergeVideoFragment.newInstance().apply {
                    setHelper(this@MainFragment)
                }.show(fragmentManager!!, "MergeFragment")
            }
            Constants.PLAYBACK -> {
                masterVideoFile?.let { file ->
                    releasePlayer()

                    PlaybackDialogFragment.newInstance().apply {
                        setHelper(this@MainFragment)
                        setFilePathFromSource(file)
                    }.show(fragmentManager!!, "PlaybackSpeedDialogFragment")
                }

                if (masterVideoFile == null) {
                   Toast.makeText(
                        activity!!,
                        getString(R.string.error_speed),
                       Toast.LENGTH_LONG
                    ).show()
                }
            }

        }
    }

    private fun openCamera(permission: Array<String>) {
        val blockedPermission = PermissionHelper.checkHasPermission(activity, permission)
        if (blockedPermission.size > 0) {
            val isBlocked = PermissionHelper.isPermissionBlocked(activity, blockedPermission)
            if (isBlocked) {
                PermissionHelper.callPermissionSettings(this)
            } else {
                requestPermissions(permission, Constants.RECORD_VIDEO)
            }
        } else {
            val cameraIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            videoFile = KnotUtils.createVideoFile(context!!)
            Log.v(tagName, "videoPath1: " + videoFile!!.absolutePath)
            videoUri = FileProvider.getUriForFile(
                context!!,
                "com.zaki.knotvideoeditor.provider", videoFile!!
            )
            cameraIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 240) //4 minutes
            cameraIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoFile)
            startActivityForResult(cameraIntent, Constants.RECORD_VIDEO)
        }
    }

    private fun openGallery(permission: Array<String>) {
        checkPermission(Constants.VIDEO_GALLERY, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun checkPermission(requestCode: Int, permission: String) {
        requestPermissions(arrayOf(permission), requestCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Constants.VIDEO_GALLERY -> {
                for (permission in permissions) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            activity as Activity,
                            permission
                        )
                    ) {
                        Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
                        break
                    } else {
                        if (ActivityCompat.checkSelfPermission(
                                activity as Activity,
                                permission
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            //call the gallery intent
                            KnotUtils.refreshGalleryAlone(context!!)
                            val i = Intent(
                                Intent.ACTION_PICK,
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            )
                            i.type = "video/*"
                            i.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("video/*"))
                            startActivityForResult(i, Constants.VIDEO_GALLERY)
                        } else {
                            callPermissionSettings(this)
                        }
                    }
                }
            }
            Constants.RECORD_VIDEO -> {
                for (permission in permissions) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(activity as Activity, permission)) {
                        Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
                        break
                    } else {
                        if (ActivityCompat.checkSelfPermission(
                                context!!,
                                permission
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            //call the camera intent
                            val cameraIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                            videoFile = KnotUtils.createVideoFile(context!!)
                            Log.v(tagName, "videoPath1: " + videoFile!!.absolutePath)
                            videoUri = FileProvider.getUriForFile(
                                context!!,
                                "com.zaki.knotvideoeditor.provider", videoFile!!
                            )
                            cameraIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 240) //4 minutes
                            cameraIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
                            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoFile)
                            startActivityForResult(cameraIntent, Constants.RECORD_VIDEO)
                        } else {
                            callPermissionSettings(this)
                        }
                    }
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
                    if (mode == Constants.VIDEO_GALLERY) {
                        Log.v(tagName, "filePath: $filePath")
                        masterVideoFile = File(filePath)
                        Toast.makeText(context, "Got Video", Toast.LENGTH_SHORT).show()
                        initializePlayer()
                    }
                }
            } catch (e: Exception) {

            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            Constants.VIDEO_GALLERY -> {
                data?.let {
                    setFilePath(resultCode, it, Constants.VIDEO_GALLERY)
                }
            }
            Constants.RECORD_VIDEO -> {
                data?.let {
                    Log.v(tagName, "data: " + data.data)

                    if (resultCode == Activity.RESULT_OK) {
                        masterVideoFile = KnotUtils.writeIntoFile(context!!, data, videoFile)
                        Toast.makeText(context, "Got Video", Toast.LENGTH_SHORT).show()
                        initializePlayer()
                    }
                }
            }
        }
    }

    private fun initializePlayer() {
        try{

            if(exoPlayer != null){
                exoPlayer?.release()
            }
            ePlayer?.useController = true
            exoPlayer = ExoPlayerFactory.newSimpleInstance(
                activity,
                DefaultRenderersFactory(activity),
                DefaultTrackSelector(), DefaultLoadControl()
            )

            ePlayer?.player = exoPlayer

            exoPlayer?.playWhenReady = false

            exoPlayer?.addListener(playerListener)

            exoPlayer?.prepare(KnotUtils.buildMediaSource(Uri.fromFile(masterVideoFile), VideoFrom.LOCAL))

            exoPlayer?.seekTo(0)

            //exoPlayer?.seekTo(currentWindow, playbackPosition)
        } catch (exception: Exception){
            Log.v(tagName, "exception: " + exception.localizedMessage)
        }
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
    }
    private fun releasePlayer() {
        if (exoPlayer != null) {
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    private val playerListener = object : Player.EventListener {
        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
        }

        override fun onSeekProcessed() {
        }

        override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
        }

        override fun onPlayerError(error: ExoPlaybackException?) {
            Log.v(tagName, "onPlayerError: ${error.toString()}")
            Toast.makeText(activity, "Video format is not supported", Toast.LENGTH_LONG).show()
        }

        override fun onLoadingChanged(isLoading: Boolean) {
            //pbLoading?.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        override fun onPositionDiscontinuity(reason: Int) {
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        }

        override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {

        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {

            if (playWhenReady && playbackState == Player.STATE_READY) {
                // Active playback.
            } else if (playWhenReady) {
                // Not playing because playback ended, the player is buffering, stopped or
                // failed. Check playbackState and player.getPlaybackError for details.
            } else {
                // Paused by app.
            }
        }
    }

    override fun onProgress(progress: String) {
        Log.v(tagName, "onProgress()")
        showLoading(true)
    }

    override fun onFailure(error: Exception) {
        Log.v(tagName, "onFailure() ${error.localizedMessage}")
        Toast.makeText(context, "Video processing failed", Toast.LENGTH_LONG).show()
        showLoading(false)
    }

    override fun onFinish() {
        Log.v(tagName, "onFinish()")
        showLoading(false)
    }

    override fun onNotAvailable(error: Exception) {
        Log.v(tagName, "onNotAvailable() ${error.localizedMessage}")
    }

    override fun onSuccess(convertedFile: File, type: String) {
        Log.v(tagName, "onSuccess()")
        showLoading(false)
        onFileProcessed(convertedFile)
    }

    override fun onDidNothing() {
        initializePlayer()
    }

    override fun onFileProcessed(file: File) {
        masterVideoFile = file
        initializePlayer()
    }

    override fun getFile(): File? {
        return File("")
    }

    override fun reInitPlayer() {
        initializePlayer()
    }

    override fun onAudioFileProcessed(convertedAudioFile: File) {

    }

    override fun showLoading(isShow: Boolean) {
        if (isShow)
            dialog!!.show()
        else {
            if (dialog!!.isShowing)
            dialog!!.dismiss()
        }

    }

    override fun openGallery() {
        releasePlayer()
        checkPermission(Constants.VIDEO_GALLERY, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    override fun openCamera() {
        releasePlayer()
        openCamera(Constants.PERMISSION_CAMERA)
    }

    private fun showBottomSheetDialogFragment(bottomSheetDialogFragment: BottomSheetDialogFragment) {
        val bundle = Bundle()
        bottomSheetDialogFragment.arguments = bundle
        bottomSheetDialogFragment.show(fragmentManager!!, bottomSheetDialogFragment.tag)
    }


}
