package com.show.example

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.show.permission.PermissionFactory
import om.show.example.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        findViewById<View>(R.id.btn).setOnClickListener {
            val dialog = CheckDialog()
            dialog.show(supportFragmentManager,"CheckDialog")


        }

    }


}