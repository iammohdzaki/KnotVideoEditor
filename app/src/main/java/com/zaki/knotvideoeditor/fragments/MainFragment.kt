package com.zaki.knotvideoeditor.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
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
import java.io.File
import java.util.ArrayList

class MainFragment : Fragment(), View.OnClickListener,OnFeaturesClickListener ,FFMpegCallback,BaseCreatorDialogFragment.CallBacks{
    private var videoFile: File? = null
    private var videoUri: Uri? = null
    private var masterVideoFile: File? = null
    private var tagName: String = MainFragment::class.java.simpleName
    private var ePlayer: PlayerView? = null
    private var exoPlayer: SimpleExoPlayer? = null
    private var videoOptions: ArrayList<String> = ArrayList()


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

        view.ivCamera.setOnClickListener(this)
        view.ivGallery.setOnClickListener(this)
        setFeatures()
    }

    private fun setFeatures(){
        videoOptions.add(Constants.TRIM)
        videoOptions.add(Constants.MUSIC)
        videoOptions.add(Constants.PLAYBACK)
        videoOptions.add(Constants.TEXT)
        videoOptions.add(Constants.OBJECT)
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
        }
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
                "com.obs.marveleditor.provider", videoFile!!
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

    fun checkPermission(requestCode: Int, permission: String) {
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

    private fun releasePlayer() {
        if (exoPlayer != null) {
            /*playbackPosition = exoPlayer?.currentPosition!!
            currentWindow = exoPlayer?.currentWindowIndex!!
            playWhenReady = exoPlayer?.playWhenReady*/
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

    }

    override fun onFailure(error: Exception) {

    }

    override fun onFinish() {

    }

    override fun onNotAvailable(error: Exception) {

    }

    override fun onSuccess(convertedFile: File, type: String) {

    }

    override fun onDidNothing() {

    }

    override fun onFileProcessed(file: File) {

    }

    override fun getFile(): File? {
        return File("")
    }

    override fun reInitPlayer() {

    }

    override fun onAudioFileProcessed(convertedAudioFile: File) {

    }

    override fun showLoading(isShow: Boolean) {

    }

    override fun openGallery() {

    }

    override fun openCamera() {

    }

    private fun showBottomSheetDialogFragment(bottomSheetDialogFragment: BottomSheetDialogFragment) {
        val bundle = Bundle()
        bottomSheetDialogFragment.arguments = bundle
        bottomSheetDialogFragment.show(fragmentManager!!, bottomSheetDialogFragment.tag)
    }

}
