package com.zaki.knotvideoeditor.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.zaki.knotvideoeditor.R
import com.zaki.knotvideoeditor.editor.VideoEditor
import com.zaki.knotvideoeditor.fragments.adapter.PlaybackSpeedAdapter
import com.zaki.knotvideoeditor.utils.Constants
import com.zaki.knotvideoeditor.utils.KnotUtils
import com.zaki.knotvideoeditor.utils.interfaces.DialogHelper
import com.zaki.knotvideoeditor.utils.interfaces.FFMpegCallback
import com.zaki.knotvideoeditor.utils.interfaces.PlaybackSpeedListener
import java.io.File

/**
 * Developer : Mohammad Zaki
 * Created On : 01-08-2020
 */

class PlaybackDialogFragment : BottomSheetDialogFragment(), DialogHelper,
    FFMpegCallback, PlaybackSpeedListener {

    private var tagName: String = PlaybackDialogFragment::class.java.simpleName
    private lateinit var rootView: View
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var rvPlaybackSpeed: RecyclerView
    private lateinit var playbackSpeedAdapter: PlaybackSpeedAdapter
    private var playbackSpeed: ArrayList<String> = ArrayList()
    private lateinit var ivClose: ImageView
    private lateinit var ivDone: ImageView
    private var masterFile: File? = null
    private var isHavingAudio = true
    private var helper: BaseCreatorDialogFragment.CallBacks ? = null
    private var mContext: Context? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_playback_speed_dialog, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvPlaybackSpeed = rootView.findViewById(R.id.rvPlaybackSpeed)
        ivClose = rootView.findViewById(R.id.iv_close)
        ivDone = rootView.findViewById(R.id.iv_done)
        linearLayoutManager = LinearLayoutManager(activity!!.applicationContext)

        mContext = context

        ivClose.setOnClickListener {
            dismiss()
        }

        ivDone.setOnClickListener {
            playbackSpeedAdapter.setPlayback()
        }

        linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        rvPlaybackSpeed.layoutManager = linearLayoutManager

        playbackSpeed.add(Constants.SPEED_0_25)
        playbackSpeed.add(Constants.SPEED_0_5)
        playbackSpeed.add(Constants.SPEED_0_75)
        playbackSpeed.add(Constants.SPEED_1_0)
        playbackSpeed.add(Constants.SPEED_1_25)
        playbackSpeed.add(Constants.SPEED_1_5)

        playbackSpeedAdapter = PlaybackSpeedAdapter(playbackSpeed, activity!!.applicationContext, this)
        rvPlaybackSpeed.adapter = playbackSpeedAdapter
        playbackSpeedAdapter.notifyDataSetChanged()
    }

    companion object {
        fun newInstance() = PlaybackDialogFragment()
    }

    override fun setHelper(helper: BaseCreatorDialogFragment.CallBacks) {
        this.helper = helper
    }

    override fun setMode(mode: Int) {
    }

    override fun setFilePathFromSource(file: File) {
        masterFile = file
        isHavingAudio = KnotUtils.isVideoHaveAudioTrack(file.absolutePath)
        Log.d(tagName, "isHavingAudio $isHavingAudio")
    }

    override fun setDuration(duration: Long) {

    }

    override fun onProgress(progress: String) {
        Log.d(tagName, "onProgress() $progress")
    }

    override fun onSuccess(convertedFile: File, type: String) {
        Log.d(tagName, "onSuccess()")
        helper?.showLoading(false)
        helper?.onFileProcessed(convertedFile)
    }

    override fun onFailure(error: Exception) {
        Log.d(tagName, "onFailure() " + error.localizedMessage)
        Toast.makeText(mContext, "Video processing failed", Toast.LENGTH_LONG).show()
        helper?.showLoading(false)
    }

    override fun onNotAvailable(error: Exception) {
        Log.d(tagName,"onNotAvailable() " + error.message)
        helper?.showLoading(false)
    }

    override fun onFinish() {
        Log.d(tagName, "onFinish()")
        helper?.showLoading(false)
    }

    override fun processVideo(playbackSpeed: String, tempo: String) {
        if(playbackSpeed != "0.0") {
            //output file is generated and send to video processing
            val outputFile = KnotUtils.createVideoFile(context!!)
            Log.v(tagName, "outputFile: ${outputFile.absolutePath}")

            VideoEditor.with(context!!)
                .setType(Constants.VIDEO_PLAYBACK_SPEED)
                .setFile(masterFile!!)
                .setOutputPath(outputFile.absolutePath)
                .setIsHavingAudio(isHavingAudio)
                .setSpeedTempo(playbackSpeed, tempo)
                .setCallback(this)
                .main()

            helper?.showLoading(true)
            dismiss()
        } else {
            Toast.makeText(activity!!, getString(R.string.error_select_speed),Toast.LENGTH_LONG).show()
        }
    }
}