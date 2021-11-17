package com.show.example

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.show.permission.PermissionFactory
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        btn.setOnClickListener {

            PermissionFactory.with(this).request(Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA
            ){ allGranted, grantedList, denyList ->
                Log.e("22222","111 $allGranted  ${grantedList} ${denyList}")
            }

            PermissionFactory.with(this).request(
                Manifest.permission.CAMERA){ allGranted, grantedList, denyList ->
                Log.e("22222","222 $allGranted  ${grantedList} ${denyList}")
            }

            PermissionFactory.with(this).request(
                Manifest.permission.CALL_PHONE,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_PHONE_STATE){ allGranted, grantedList, denyList ->
                Log.e("22222","333 $allGranted  ${grantedList} ${denyList}")
            }
            PermissionFactory.with(this).request(
                Manifest.permission.RECORD_AUDIO,Manifest.permission.CALL_PHONE){ allGranted, grantedList, denyList ->
                Log.e("22222","444 $allGranted  ${grantedList} ${denyList}")
            }
        }

    }


}