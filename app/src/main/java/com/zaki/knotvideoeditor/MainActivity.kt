package com.zaki.knotvideoeditor

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.zaki.knotvideoeditor.fragments.MainFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_container, MainFragment()).commit()
    }
}
