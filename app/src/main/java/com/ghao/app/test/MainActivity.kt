package com.ghao.app.test

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.ghao.app.ui.TouchPullViewController

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        TouchPullViewController(findViewById(R.id.main_activity_container)).init()
    }
}
