package com.zaki.knotvideoeditor.fragments.adapter

import android.content.Context
import android.graphics.Color

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zaki.knotvideoeditor.R
import com.zaki.knotvideoeditor.utils.Constants
import com.zaki.knotvideoeditor.utils.interfaces.PlaybackSpeedListener

class PlaybackSpeedAdapter(private val playbackList: ArrayList<String>, val context: Context, PlaybackSpeedListener: PlaybackSpeedListener) :
    RecyclerView.Adapter<PlaybackSpeedAdapter.MyPostViewHolder>() {

    private var tagName: String = PlaybackSpeedAdapter::class.java.simpleName
    private var myPlaybackList = playbackList
    private var myPlaybackSpeedListener = PlaybackSpeedListener
    private var selectedPosition: Int = -1
    private var selectedPlayback: Float = 0F
    private var selectedTempo: Float = 0F

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): MyPostViewHolder {
        return MyPostViewHolder(LayoutInflater.from(context).inflate(R.layout.playback_view, p0, false))
    }

    override fun getItemCount(): Int {
        return myPlaybackList.size
    }

    override fun onBindViewHolder(holder: MyPostViewHolder, position: Int) {

        holder.tvSpeed.text = playbackList[position]

        if (selectedPosition == position) {
            holder.tvSpeed.setBackgroundColor(Color.WHITE)
            holder.tvSpeed.setTextColor(Color.BLACK)
        } else {
            holder.tvSpeed.setBackgroundColor(Color.BLACK)
            holder.tvSpeed.setTextColor(Color.WHITE)
        }

        holder.tvSpeed.setOnClickListener {

            selectedPosition = position

            //based on selected play back speed - playback & tempo is selected for processing
            when (playbackList[position]) {
                Constants.SPEED_0_25 -> {
                    selectedPlayback = 1.75F
                    selectedTempo = 0.50F
                }

                Constants.SPEED_0_5 -> {
                    selectedPlayback = 1.50F
                    selectedTempo = 0.50F
                }

                Constants.SPEED_0_75 -> {
                    selectedPlayback = 1.25F
                    selectedTempo = 0.75F
                }

                Constants.SPEED_1_0 -> {
                    selectedPlayback = 1.0F
                    selectedTempo = 1.0F
                }

                Constants.SPEED_1_25 -> {
                    selectedPlayback = 0.75F
                    selectedTempo = 1.25F
                }

                Constants.SPEED_1_5 -> {
                    selectedPlayback = 0.50F
                    selectedTempo = 2.0F
                }
            }
            notifyDataSetChanged()
        }
    }

    fun setPlayback() {
        Log.v(tagName, "selectedPlayback: $selectedPlayback, selectedTempo: $selectedTempo")
        myPlaybackSpeedListener.processVideo(selectedPlayback.toString(), selectedTempo.toString())
    }

    class MyPostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvSpeed: TextView = itemView.findViewById(R.id.tv_speed)
    }
}