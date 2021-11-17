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
    private var fragment: PermissionFragment? = null

    data class DenyResult(var alwaysFalse: Boolean, val permission: String)

    private val requestCallBack = LinkedHashMap<String,
                (allGranted: Boolean, grantedList: MutableList<String>, denyList: MutableList<DenyResult>) -> Unit>()

    private val listener = LifecycleEventObserver { source, event ->
        if (event == Lifecycle.Event.ON_DESTROY) {
            clear()
        }
    }

    fun request(
        vararg permissions: String,
        result: (allGranted: Boolean, grantedList: MutableList<String>, denyList: MutableList<DenyResult>) -> Unit
    ) {
        if (requestPermission.isEmpty()) {
            alreadyGranted.clear()
            requestPermission.clear()
            weakActivity?.get()?.lifecycle?.addObserver(listener)
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
                        } else {
                            alreadyGranted.add(it)
                        }
                    }
                    if (requestPermission.isNotEmpty()) {
                        invoke(
                            requestPermission.toArray(arrayOfNulls(requestPermission.size)),
                            result
                        )
                    } else {
                        result.invoke(true, permissions.toCollection(arrayListOf()), arrayListOf())
                    }
                }
            }
        } else {
            requestCallBack[permissions.toKey()] = result
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
        permissions: Array<String>,
        result: (allGranted: Boolean, grantedList: MutableList<String>, denyList: MutableList<DenyResult>) -> Unit
    ) {
        weakReference?.get()?.apply {
            fragment = PermissionFragment.get(permissions)
            beginTransaction()
                .add(fragment!!, fragment!!::class.java.name)
                .commitNow()
            isAdded = true
            fragment?.apply {
                fragment?.setOnCallPermissionResult {
                    if (it.isEmpty()) {
                        result.invoke(true, it.keys.toMutableList(), arrayListOf())
                    } else {
                        var allok = true
                        val denyList = arrayListOf<String>()
                        val grantedList = arrayListOf<String>()
                        for (entry in it.entries) {
                            allok = allok && entry.value
                            if (entry.value.not()) {
                                denyList.add(entry.key)
                            } else {
                                grantedList.add(entry.key)
                            }
                        }
                        grantedList.addAll(alreadyGranted)
                        result.invoke(allok, grantedList, denyList.map { permission ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                isAlwaysFalseCheck(weakActivity?.get()!!, permission)
                            } else {
                                DenyResult(false, permission)
                            }
                        }.toMutableList())
                    }
                    beginTransaction().remove(fragment!!)

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
            }
        }
    }


    private fun clear() {
        weakActivity?.get()?.apply { factoryStore.remove(this) }
        alreadyGranted.clear()
        requestPermission.clear()
        if (isAdded) {
            weakReference?.get()?.beginTransaction()?.remove(fragment!!)
        }
        isAdded = false
        weakActivity?.get()?.lifecycle?.removeObserver(listener)
        weakReference = null
    }


}
