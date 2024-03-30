package com.show.permission

import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.lang.StringBuilder
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap

/**
 * PackageName : com.show.permission
 * Date: 2020/12/30
 * Author: ShowMeThe
 */

class PermissionFactory private constructor(
    private var weakReference: WeakReference<FragmentManager>? = null,
    private var weakActivity: WeakReference<FragmentActivity>? = null
) {

    companion object {
        private const val REQUEST_BACKGROUND_LOCATION =
            "android.permission.ACCESS_BACKGROUND_LOCATION"
        private const val REQUEST_ACCESS_FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION"
        private const val REQUEST_ACCESS_COARSE_LOCATION =
            "android.permission.ACCESS_COARSE_LOCATION"
        private const val FRAGMENT_ADD_TAG = "com.show.permission.PermissionFragment_Add"
        private val factoryStore = HashMap<LifecycleOwner, PermissionFactory>()

        @JvmStatic
        fun with(activity: FragmentActivity): PermissionFactory {
            var factory = factoryStore[activity]
            return if (factory == null) {
                factory = PermissionFactory(
                    WeakReference(activity.supportFragmentManager),
                    WeakReference(activity)
                )
                factoryStore[activity] = factory
                factory
            } else {
                factory
            }
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
        ): ArrayList<DenyResult> {
            return isAlwaysFalse(activity, *permissions)
        }

        @JvmStatic
        fun checkPermissionsIsAlwaysFalse(
            fragment: Fragment,
            vararg permissions: String
        ): ArrayList<DenyResult> {
            return isAlwaysFalse(fragment.requireActivity(), *permissions)
        }


        private fun isAlwaysFalse(
            activity: FragmentActivity,
            vararg permissions: String
        ): ArrayList<DenyResult> {
            val list = ArrayList<DenyResult>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                for (permission in permissions) {
                    list.add(isAlwaysFalseCheck(activity, permission))
                }
            }
            return list
        }


        @RequiresApi(Build.VERSION_CODES.M)
        private fun isAlwaysFalseCheck(
            activity: FragmentActivity,
            permission: String
        ): DenyResult {
            return DenyResult(
                activity.shouldShowRequestPermissionRationale(permission).not(),
                permission
            )
        }

    }


    private var isAdded = false
    private val requestPermission = ArrayList<String>()
    private val alreadyGranted = ArrayList<String>()


    data class DenyResult(var alwaysFalse: Boolean, val permission: String)

    private val requestCallBack = LinkedHashMap<String,
                (allGranted: Boolean, grantedList: MutableList<String>, denyList: MutableList<DenyResult>) -> Unit>()

    private val listener = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) {
            clear()
        }
    }

    fun request(
        vararg permissions: String,
        result: (allGranted: Boolean, grantedList: MutableList<String>, denyList: MutableList<DenyResult>) -> Unit
    ) {
        val activity = weakActivity?.get() ?: return
        if (requestPermission.isEmpty()) {
            alreadyGranted.clear()
            requestPermission.clear()
            activity.lifecycle.addObserver(listener)
            if (permissions.isEmpty()) {
                return
            } else {
                val copyPermissions = ArrayList<String>(permissions.size)
                permissions.toCollection(copyPermissions)
                copyPermissions.setLocationSpecial()
                copyPermissions.forEach {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && activity.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestPermission.add(it)
                    } else {
                        alreadyGranted.add(it)
                    }
                }
                if (requestPermission.isNotEmpty()) {
                    invoke(requestPermission, result)
                } else {
                    result.invoke(true, copyPermissions, arrayListOf())
                }
            }
        } else {
            requestCallBack[permissions.toKey()] = result
        }
    }

    private fun ArrayList<String>.setLocationSpecial() {
        var indexOfLocation = this.indexOf(REQUEST_BACKGROUND_LOCATION)
        if (indexOfLocation != -1) {
            this.add(this.lastIndex, this.removeAt(indexOfLocation))
            if (this.contains(REQUEST_ACCESS_FINE_LOCATION).not()) {
                indexOfLocation = this.indexOf(REQUEST_BACKGROUND_LOCATION)
                this.add(indexOfLocation, REQUEST_ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun Array<out String>.toKey() = run {
        val key = StringBuilder()
        forEach {
            key.append(it).append(",")
        }
        if (key.isNotEmpty()) {
            key.deleteAt(key.length - 1)
        }
        key.toString()
    }


    private fun invoke(
        permissions: ArrayList<String>,
        result: (allGranted: Boolean, grantedList: MutableList<String>, denyList: MutableList<DenyResult>) -> Unit
    ) {
        val fragmentManager = weakReference?.get()?:return
        val findFragment = fragmentManager.findFragmentByTag(FRAGMENT_ADD_TAG)
        if (findFragment != null && fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStack()
        } else if (findFragment != null) {
            fragmentManager.beginTransaction().remove(findFragment).commitNowAllowingStateLoss()
        }
        val permissionFragment = PermissionFragment.get(permissions)
        fragmentManager
            .beginTransaction().add(permissionFragment, FRAGMENT_ADD_TAG)
            .commitNowAllowingStateLoss()
        permissionFragment.setOnCallPermissionResult {
            if (it.isEmpty()) {
                result.invoke(true, arrayListOf(), arrayListOf())
            } else {
                var allOk = true
                val denyList = arrayListOf<String>()
                val grantedList = arrayListOf<String>()
                for (entry in it.entries) {
                    allOk = allOk && entry.value
                    if (entry.value.not()) {
                        denyList.add(entry.key)
                    } else {
                        grantedList.add(entry.key)
                    }
                }
                grantedList.addAll(alreadyGranted)
                result.invoke(allOk, grantedList, denyList.map { permission ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        isAlwaysFalseCheck(weakActivity?.get()!!, permission)
                    } else {
                        DenyResult(false, permission)
                    }
                }.toMutableList())

                alreadyGranted.clear()
                requestPermission.clear()

                /**
                 * 处理等待任务
                 */
                synchronized(requestCallBack) {
                    if (requestCallBack.isNotEmpty()) {
                        val key = requestCallBack.keys.first()
                        val callback = requestCallBack[key]
                        if (callback != null) {
                            request(*key.split(",").toTypedArray(), result = callback)
                            requestCallBack.remove(key)
                        }
                    }
                }
            }
            fragmentManager.beginTransaction()
                .remove(permissionFragment)
                .commitAllowingStateLoss()
        }
        isAdded = true
    }


    private fun clear() {
        weakActivity?.get()?.apply { factoryStore.remove(this) }
        alreadyGranted.clear()
        requestPermission.clear()
        isAdded = false
        weakActivity?.get()?.lifecycle?.removeObserver(listener)
        weakReference = null
    }


}
