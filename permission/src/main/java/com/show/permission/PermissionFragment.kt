package com.show.permission

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * PackageName : com.show.permission
 * Date: 2020/12/30
 * Author: ShowMeThe
 */

class PermissionFragment : Fragment() {

    companion object {

        private const val EXTRA_PERMISSION = "permissions"



        fun get(permissions: ArrayList<String>): PermissionFragment {
            val fragment = PermissionFragment()
            val bundle = Bundle()
            bundle.putStringArrayList(EXTRA_PERMISSION, permissions)
            fragment.arguments = bundle
            return fragment
        }


    }


    private val finalRequestPermission = ArrayList<String>()
    private val finalResultHash = HashMap<String, Boolean>()
    private val requestMultiple = ActivityResultContracts.RequestPermission()
    private val register = registerForActivityResult(requestMultiple) {
        launchNextOrOut(it)

    }

    private fun launchNextOrOut(result: Boolean) {
        if (finalRequestPermission.isNotEmpty()) {
            val permission = finalRequestPermission.removeAt(0)
            finalResultHash[permission] = result
            if (finalRequestPermission.isNotEmpty()) {
                register.launch(finalRequestPermission.first())
            } else {
                onCallPermission?.invoke(finalResultHash)
            }
        }

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
        val permissions = requireArguments().getStringArrayList(EXTRA_PERMISSION) ?: return
        if (permissions.isNotEmpty()) {
            finalRequestPermission.clear()
            permissions.toCollection(finalRequestPermission)
            val first = finalRequestPermission.first()
            register.launch(first)
        } else {
            onCallPermission?.invoke(HashMap())
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