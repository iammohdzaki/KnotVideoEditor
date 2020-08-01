package com.zaki.knotvideoeditor.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.github.guilhe.views.SeekBarRangedView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.zaki.knotvideoeditor.R
import com.zaki.knotvideoeditor.editor.VideoEditor
import com.zaki.knotvideoeditor.utils.Constants
import com.zaki.knotvideoeditor.utils.KnotUtils
import com.zaki.knotvideoeditor.utils.interfaces.FFMpegCallback
import kotlinx.android.synthetic.main.fragment_video_trim.view.*
import java.io.File

class VideoTrimFragment : BottomSheetDialogFragment() ,FFMpegCallback{

    private var tagName: String = VideoTrimFragment::class.java.simpleName
    private var videoFile: File? = null
    private var helper: BaseCreatorDialogFragment.CallBacks? = null
    private var duration: Long? = null
    private lateinit var rootView: View
    private lateinit var ivClose: ImageView
    private lateinit var ivDone: ImageView
    private var sbrvVideoTrim: SeekBarRangedView? = null
    private var actvStartTime: TextView? = null
    private var actvEndTime: TextView? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_video_trim, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ivClose = rootView.findViewById(R.id.iv_close)
        ivDone = rootView.findViewById(R.id.iv_done)
        sbrvVideoTrim = rootView.findViewById(R.id.sbrvVideoTrim)
        actvStartTime = rootView.findViewById(R.id.actvStartTime)
        actvEndTime = rootView.findViewById(R.id.actvEndTime)


        ivDone.setOnClickListener {
            //output file is generated and send to video processing
            val outputFile = KnotUtils.createVideoFile(context!!)
            Log.v(tagName, "outputFile: ${outputFile.absolutePath}")

            VideoEditor.with(context!!)
                .setType(Constants.VIDEO_TRIM)
                .setFile(videoFile!!)
                .setOutputPath(outputFile.path)
                .setStartTime(actvStartTime?.text.toString())
                .setEndTime(actvEndTime?.text.toString())
                .setCallback(this)
                .main()

            helper?.showLoading(true)
            dismiss()
        }

        view.iv_close.setOnClickListener {
            dismiss()
        }

        Log.v(tagName, "duration: $duration")
        Log.v(tagName, "duration: " + KnotUtils.secToTime(duration!!))

        sbrvVideoTrim?.minValue = 0f
        sbrvVideoTrim?.maxValue = duration?.toFloat()!!
        actvStartTime?.text = KnotUtils.secToTime(0)
        actvEndTime?.text = KnotUtils.secToTime(duration!!)

        sbrvVideoTrim?.setOnSeekBarRangedChangeListener(object : SeekBarRangedView.OnSeekBarRangedChangeListener {
            override fun onChanged(view: SeekBarRangedView?, minValue: Float, maxValue: Float) {
                //exoPlayer?.seekTo(minValue.toLong())
            }

            override fun onChanging(view: SeekBarRangedView?, minValue: Float, maxValue: Float) {
                Log.v(tagName, "minValue: $minValue, maxValue: $maxValue")
                actvStartTime?.text = KnotUtils.secToTime(minValue.toLong())
                actvEndTime?.text = KnotUtils.secToTime(maxValue.toLong())
            }
        })
    }


    fun setHelper(helper: BaseCreatorDialogFragment.CallBacks) {
        this.helper = helper
    }

    fun setFilePathFromSource(file: File, duration: Long) {
        videoFile = file
        this.duration = duration
    }

    override fun onProgress(progress: String) {
        Log.d(tagName, "onProgress() $progress")
        helper?.showLoading(true)
    }

    override fun onSuccess(convertedFile: File, type: String) {
        Log.d(tagName, "onSuccess()")
        helper?.showLoading(false)
        helper?.onFileProcessed(convertedFile)
    }

    override fun onFailure(error: Exception) {
        Log.d(tagName, "onFailure() " + error.localizedMessage)
        Toast.makeText(context, "Video processing failed", Toast.LENGTH_LONG).show()
        helper?.showLoading(false)
    }

    override fun onNotAvailable(error: Exception) {
        Log.d(tagName,"onNotAvailable() " + error.message)
        Log.v(tagName, "Exception: ${error.localizedMessage}")
    }

    override fun onFinish() {
        Log.d(tagName, "onFinish()")
        helper?.showLoading(false)
    }

}