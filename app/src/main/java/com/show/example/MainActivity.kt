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
//            val dialog = CheckDialog()
//            dialog.show(supportFragmentManager,"CheckDialog")
            PermissionFactory.with(this).request(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,
            ){ allGranted, grantedList, denyList ->
                Log.e("22222","111 $allGranted  ${grantedList} ${denyList}")
            }

        }

    }


}