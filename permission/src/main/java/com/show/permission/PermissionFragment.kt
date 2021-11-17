package com.show.permission

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import java.util.HashMap

/**
 * PackageName : com.show.permission
 * Date: 2020/12/30
 * Author: ShowMeThe
 */

class PermissionFragment : Fragment() {

    companion object {

        fun get(permissions: Array<out String>): PermissionFragment {
            val fragment = PermissionFragment()
            val bundle = Bundle()
            bundle.putStringArray("permissions", permissions)
            fragment.arguments = bundle
            return fragment
        }
    }



    private lateinit var permissions: Array<String>
    private val requestMultiple = ActivityResultContracts.RequestMultiplePermissions()
    private val register = registerForActivityResult(requestMultiple) {
        onCallPermission?.invoke(HashMap(it))
    }
    private val listener = LifecycleEventObserver { source, event ->
        if (event == Lifecycle.Event.ON_CREATE) {
            onStartPermission()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return View(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycle.addObserver(listener)
    }


    private fun onStartPermission() {
        arguments?.apply {
            permissions = getStringArray("permissions")!!
        }
        if (permissions.isNotEmpty()) {
            register.launch(permissions)
        } else {
            onCallPermission?.invoke(HashMap<String, Boolean>())
        }
    }


    private var onCallPermission: ((result: HashMap<String, Boolean>) -> Unit)? = null
    fun setOnCallPermissionResult(onCallPermission: ((result: HashMap<String, Boolean>) -> Unit)) {
        this.onCallPermission = onCallPermission
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(listener)
    }

}