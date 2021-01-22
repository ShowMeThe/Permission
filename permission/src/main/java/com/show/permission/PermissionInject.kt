package com.show.permission

import android.util.ArrayMap
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import java.lang.reflect.Method
import java.util.*

/**
 * PackageName : com.show.permission
 * Date: 2020/12/30
 * Author: ShowMeThe
 */

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PermissionResult()

class PermissionInject {

    inner class Pair(var result: Boolean, var method: Method?)

    companion object {
        private val lifeOwnerKeeper = ArrayMap<LifecycleOwner, Pair>()
        private val clazzKeeper = ArrayMap<Class<*>, Pair>()
        private val mManager by lazy { PermissionInject() }


        @JvmStatic
        fun with(fragment: Fragment, vararg permissions: String): PermissionInject {
            mManager.inject(fragment)
            mManager.requestPermission(fragment, permissions)
            return mManager
        }

        @JvmStatic
        fun with(fragment: FragmentActivity, vararg permissions: String): PermissionInject {
            mManager.inject(fragment)
            mManager.requestPermission(fragment, permissions)
            return mManager
        }

        @JvmStatic
        fun inject(lifecycleOwner: LifecycleOwner) {
            mManager.inject(lifecycleOwner)
        }

        @JvmStatic
        fun inject(clazz: Class<*>) {
            mManager.inject(clazz)
        }

    }


    fun requestPermission(fragment: Fragment, permissions: Array<out String>) {
        PermissionFactory.with(fragment).requestEach(*permissions) {
            pathResult(it)
        }
    }


    fun requestPermission(activity: FragmentActivity, permissions: Array<out String>) {
        PermissionFactory.with(activity).requestEach(*permissions) {
            pathResult(it)
        }
    }

    private var lastResultMap: HashMap<String, Boolean>? = HashMap<String, Boolean>()
    private var mVersion = 0

    /**
     * 放返回值为true时候，且该类未销毁不再接受收权限请求的通知，即消费完毕，且通知事件不是沾粘的
     */
    private fun pathResult(map: HashMap<String, Boolean>?) {
        mVersion++
        lastResultMap = map
        lifeOwnerKeeper.forEach { entry ->
            /**
             * LifeOwner State.STARTED 的内容才能收到
             */
            if (entry.key.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                val pair = entry.value
                if (pair.result) {
                    val methodResult = pair.method?.invoke(entry.key, map)
                    if (methodResult != null && methodResult is Boolean && methodResult) {
                        pair.result = false
                    }
                }
            }
        }

        clazzKeeper.forEach { entry ->
            val pair = entry.value
            if (pair.result) {
                val methodResult = pair.method?.invoke(entry.key, map)
                if (methodResult != null && methodResult is Boolean && methodResult) {
                    pair.result = false
                }
            }
        }
    }


    fun inject(clazz: Class<*>): PermissionInject {
        if (clazzKeeper.contains(clazz)) return this
        findAnoInClass(clazz::class.java)?.apply {
            clazzKeeper[clazz] = Pair(true, this)
        }
        return this
    }

    fun unInject(clazz: Class<*>) {
        clazzKeeper.remove(clazz)
    }

    fun inject(lifecycleOwner: LifecycleOwner): PermissionInject {
        if (lifeOwnerKeeper.contains(lifecycleOwner)) return this
        findAnoInClass(lifecycleOwner::class.java)?.apply {
            lifeOwnerKeeper[lifecycleOwner] = Pair(true, this)
            lifecycleOwner.lifecycle.addObserver(object : LifeCompat() {
                override fun destroy() {
                    lifeOwnerKeeper.remove(lifecycleOwner)
                }

                override fun startIfPresent() {
                    lifeOwnerKeeper[lifecycleOwner]?.also { pair ->
                        if (pair.result && lastResultMap?.size!! > 0 && version < mVersion) {
                            val methodResult = pair.method?.invoke(lifecycleOwner, lastResultMap)
                            if (methodResult != null && methodResult is Boolean && methodResult) {
                                pair.result = false
                            }
                            version++
                        }
                    }
                }
            })
        }
        return this
    }


    private fun findAnoInClass(clazz: Class<*>): Method? {
        try {
            for (method in clazz.methods) {
                val isAnnotation = method.isAnnotationPresent(PermissionResult::class.java)
                if (isAnnotation) {
                    return method
                }
            }
        } catch (e: Exception) {
            return null
        }
        return null
    }

    abstract inner class LifeCompat : LifecycleObserver {

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun onStart() {
            startIfPresent()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            destroy()
        }

        abstract fun destroy()

        abstract fun startIfPresent()

        var version = 0
    }
}