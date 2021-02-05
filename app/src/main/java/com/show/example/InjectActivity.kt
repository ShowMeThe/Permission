package com.show.example

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.show.permission.PermissionFactory
import com.show.permission.PermissionInject
import com.show.permission.PermissionResult
import kotlinx.android.synthetic.main.activity_inject.*
import java.util.HashMap
import java.util.jar.Manifest

class InjectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inject)

        PermissionInject.inject(this)

        btn.setOnClickListener {
            startActivity(Intent(this,MainActivity::class.java))
        }

    }


    @PermissionResult([android.Manifest.permission.WRITE_EXTERNAL_STORAGE])
    fun aPermission(map: HashMap<String, Boolean>):Boolean{
        Log.e("22222222222","$map")
        return true
    }

}