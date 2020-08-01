package com.zaki.knotvideoeditor.fragments.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.zaki.knotvideoeditor.R
import com.zaki.knotvideoeditor.utils.Constants
import com.zaki.knotvideoeditor.utils.interfaces.OnFeaturesClickListener
import kotlinx.android.synthetic.main.option_view.view.*

/**
 * Developer : Mohammad Zaki
 * Created On : 31-07-2020
 */

class FeaturesAdapter(
    private var videoOptions: ArrayList<String>,
    private val context: Context,
    private var featuresClickListener: OnFeaturesClickListener
) : RecyclerView.Adapter<FeaturesAdapter.MyFeaturesViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyFeaturesViewHolder {
        return MyFeaturesViewHolder(LayoutInflater.from(context).inflate(R.layout.option_view, parent, false))
    }

    override fun onBindViewHolder(holder: MyFeaturesViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class MyFeaturesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(position: Int){
            itemView.apply {
                when(videoOptions[position]){
                    Constants.FLIRT -> {
                        iv_option.setImageResource(R.drawable.video_conference_24)
                        tvName.text = Constants.FLIRT
                    }

                    Constants.TRIM -> {
                        iv_option.setImageResource(R.drawable.video_trimming_24)
                        tvName.text = Constants.TRIM
                    }

                    Constants.MUSIC -> {
                        iv_option.setImageResource(R.drawable.music_video_24)
                        tvName.text = Constants.MUSIC
                    }

                    Constants.PLAYBACK -> {
                        iv_option.setImageResource(R.drawable.speed_skating_24)
                        tvName.text = Constants.PLAYBACK
                    }

                    Constants.TEXT -> {
                        iv_option.setImageResource(R.drawable.text_width_24)
                        tvName.text = Constants.TEXT
                    }

                    Constants.OBJECT -> {
                        iv_option.setImageResource(R.drawable.sticker_24)
                        tvName.text = Constants.OBJECT
                    }

                    Constants.MERGE -> {
                        iv_option.setImageResource(R.drawable.merge_vertical_24)
                        tvName.text = Constants.MERGE
                    }

                    Constants.TRANSITION -> {
                        iv_option.setImageResource(R.drawable.transition_24)
                        tvName.text = Constants.TRANSITION
                    }
                }

                iv_option.setOnClickListener {
                    featuresClickListener.videoOptions(videoOptions[position])
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return videoOptions.size
    }

}