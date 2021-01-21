package com.show.permission

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.*

import java.lang.ref.WeakReference

import java.util.ArrayList
import java.util.HashMap

/**
 * PackageName : com.show.permission
 * Date: 2020/12/30
 * Author: ShowMeThe
 */

class PermissionFactory private constructor(
    private var weakReference: WeakReference<FragmentManager>? = null,
    private var weakActivity: WeakReference<FragmentActivity>? = null
) : LifecycleObserver {

    companion object {

        @JvmStatic
        fun with(activity: FragmentActivity): PermissionFactory {
            return PermissionFactory(
                WeakReference(activity.supportFragmentManager),
                WeakReference(activity)
            )
        }

        @JvmStatic
        fun with(fragment: Fragment): PermissionFactory {
            return PermissionFactory(
                WeakReference(fragment.childFragmentManager),
                WeakReference(fragment.requireActivity())
            )
        }

        @JvmStatic
        fun checkPermissionIsAlwaysFalse(
            activity: FragmentActivity,
            vararg permissions: String
        ): ArrayList<Pair<String, Boolean>> {
            return isAlwaysFalse(activity, *permissions)
        }

        @JvmStatic
        fun checkPermissionIsAlwaysFalse(
            fragment: Fragment,
            vararg permissions: String
        ): ArrayList<Pair<String, Boolean>> {
            return isAlwaysFalse(fragment.requireActivity(), *permissions)
        }

        @RequiresApi(Build.VERSION_CODES.M)
        @JvmStatic
        fun checkPermissionIsAlwaysFalse(
            fragment: Fragment,
            permissions: String
        ): Pair<String, Boolean> {
            return isAlwaysFalseCheck(fragment.requireActivity(), permissions)
        }

        private fun isAlwaysFalse(activity: FragmentActivity, vararg permissions: String): ArrayList<Pair<String, Boolean>> {
            val list = ArrayList<Pair<String, Boolean>>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                for (permission in permissions) {
                    list.add(isAlwaysFalseCheck(activity, permission))
                }
            }
            return list
        }


        @RequiresApi(Build.VERSION_CODES.M)
        private fun isAlwaysFalseCheck(activity: FragmentActivity, permission: String): Pair<String, Boolean> {
            return permission to activity.shouldShowRequestPermissionRationale(permission)

        }

    }


    private var isAdded = false
    private val requestPermission = ArrayList<String>()
    private var fragment: PermissionFragment? = null

    fun requestAll(
        vararg permissions: String,
        result: (allGranted: Boolean, denyList: ArrayList<String>) -> Unit
    ) {
        weakActivity?.get()?.lifecycle?.addObserver(this)
        weakReference?.get()?.apply {
            if (permissions.isEmpty()) {
                return
            } else {
                permissions.forEach {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && weakActivity?.get()
                            ?.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestPermission.add(it)
                    }
                }
                if (requestPermission.isNotEmpty()) {
                    invoke(requestPermission.toArray(arrayOfNulls(requestPermission.size)), result)
                } else {
                    result.invoke(true, arrayListOf())
                }
            }

        }
    }


    private fun invoke(
        permissions: Array<String>,
        result: (allGranted: Boolean, denyList: ArrayList<String>) -> Unit
    ) {
        weakReference?.get()?.apply {
            fragment = PermissionFragment.get(permissions)
            beginTransaction()
                .add(fragment!!, fragment!!::class.java.name)
                .commitNow()
            isAdded = true
            fragment?.apply {
                fragment?.setOnCallPermissionResult {
                    if (it == null) {
                        result.invoke(true, arrayListOf())
                    } else {
                        var allok = true
                        val fail = arrayListOf<String>()
                        for (entry in it.entries) {
                            allok = allok && entry.value
                            if (!entry.value) {
                                fail.add(entry.key)
                            }
                        }
                        result.invoke(allok, fail)
                    }
                    beginTransaction().remove(fragment!!)
                }
            }
        }
    }


    fun requestEach(vararg permissions: String, result: (map: HashMap<String, Boolean>?) -> Unit) {
        weakActivity?.get()?.lifecycle?.addObserver(this)
        weakReference?.get()?.apply {
            if (permissions.isEmpty()) {
                return
            } else {
                invokeEach(permissions, result)
            }
        }
    }

    private fun invokeEach(
        permissions: Array<out String>,
        result: (map: HashMap<String, Boolean>?) -> Unit
    ) {
        weakReference?.get()?.apply {
            fragment = PermissionFragment.get(permissions)
            beginTransaction().add(fragment!!, fragment!!::class.java.name).commitNow()
            isAdded = true
            fragment?.apply {
                fragment?.setOnCallPermissionResult {
                    result.invoke(it)
                }
            }
        }
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        requestPermission.clear()
        if (isAdded) {
            weakReference?.get()?.beginTransaction()?.remove(fragment!!)
        }
        isAdded = false
        weakActivity?.get()?.lifecycle?.removeObserver(this)
        weakReference = null
    }


}
