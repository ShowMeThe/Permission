package com.show.example

import android.Manifest
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.DialogFragment
import com.show.permission.PermissionFactory

class CheckDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        val view = View.inflate(requireContext(),R.layout.dialog_check_permission,null)
        dialog.setContentView(view)
        view.findViewById<View>(R.id.btn).setOnClickListener {
            PermissionFactory.with(this)
                .request(
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION){ allGranted, grantedList, denyList ->
                    Log.e("22222","onCreateDialog $allGranted  ${grantedList} ${denyList}")
                }
        }
        return dialog
    }

}