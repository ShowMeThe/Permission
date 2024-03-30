package com.show.example

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.show.permission.PermissionInject
import com.show.permission.PermissionResult
import om.show.example.R

class InjectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inject)

        PermissionInject.inject(this)

        findViewById<View>(R.id.btn).setOnClickListener {
            startActivity(Intent(this,MainActivity::class.java))
        }

    }


    @PermissionResult([android.Manifest.permission.ACCESS_FINE_LOCATION])
    fun aPermission(map: HashMap<String, Boolean>):Boolean{
        Log.e("222222","aPermission $map")
        return true
    }

}