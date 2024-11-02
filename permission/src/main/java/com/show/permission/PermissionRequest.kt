package com.show.permission

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.atomic.AtomicInteger

/**
 * PackageName : com.show.permission
 * Date: 2020/12/30
 * Author: ShowMeThe
 */

class PermissionRequest private constructor(
    private val activity: FragmentActivity,
    private val permissions:ArrayList<String>){
    companion object {

        fun get(activity: FragmentActivity,permissions: ArrayList<String>) = PermissionRequest(activity,permissions)
    }


    private val finalRequestPermission = ArrayList<String>()
    private val finalResultHash = HashMap<String, Boolean>()
    private val requestMultiple = ActivityResultContracts.RequestPermission()
    private val callback: (permission:String,result:Boolean) ->Unit = {  permission,result ->
        launchNextOrOut(permission,result)
    }


    private fun launchNextOrOut(permission:String,result: Boolean) {
        finalResultHash[permission] = result
        if (finalRequestPermission.isNotEmpty()) {
            val next = finalRequestPermission.removeAt(0)
            activity.requestSingle(next,callback)
        }else {
            onCallPermission?.invoke(finalResultHash)
        }

    }


    fun startPermission() {
        if (permissions.isNotEmpty()) {
            finalRequestPermission.clear()
            permissions.toCollection(finalRequestPermission)
            val next = finalRequestPermission.removeAt(0)
            activity.requestSingle(next,callback)
        } else {
            onCallPermission?.invoke(HashMap())
        }
    }



    private var onCallPermission: ((result: HashMap<String, Boolean>) -> Unit)? = null
    fun setOnCallPermissionResult(onCallPermission: ((result: HashMap<String, Boolean>) -> Unit)) {
        this.onCallPermission = onCallPermission
    }



    private val nextLocalRequestCode = AtomicInteger()
    private fun  FragmentActivity.requestSingle(
        permission: String,
        callback: (permission:String,result:Boolean)->Unit
    ) {
        val key = "activity_permission_for_result#${nextLocalRequestCode.getAndIncrement()}"
        val registry = activityResultRegistry
        var launcher: ActivityResultLauncher<String>? = null
        val observer = object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    launcher?.unregister()
                    lifecycle.removeObserver(this)
                }
            }
        }
        lifecycle.addObserver(observer)
        launcher = registry.register(key, requestMultiple) {
            launcher?.unregister()
            lifecycle.removeObserver(observer)
            callback.invoke(permission,it)
        }
        launcher.launch(permission)
    }

}