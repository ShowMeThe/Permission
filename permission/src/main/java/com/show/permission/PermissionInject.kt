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
import java.util.HashMap

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
        fun with(fragment: Fragment,vararg permissions:String):PermissionInject{
            mManager.inject(fragment)
            mManager.requestPermission(fragment, permissions)
            return mManager
        }

        @JvmStatic
        fun with(fragment: FragmentActivity,vararg permissions:String):PermissionInject{
            mManager.inject(fragment)
            mManager.requestPermission(fragment, permissions)
            return mManager
        }

        @JvmStatic
        fun inject(lifecycleOwner : LifecycleOwner){
            mManager.inject(lifecycleOwner)
        }

        @JvmStatic
        fun inject(clazz: Class<*>){
            mManager.inject(clazz)
        }

    }



    fun requestPermission(fragment: Fragment,permissions:Array<out String>) {
        PermissionFactory.with(fragment).requestEach(*permissions){
            pathResult(it)
        }
    }


    fun requestPermission(activity: FragmentActivity,permissions:Array<out String>) {
        PermissionFactory.with(activity).requestEach(*permissions){
            pathResult(it)
        }
    }

    /**
     * 放返回值为true时候，且该类未销毁不再接受收权限请求的通知，即消费完毕，且通知事件不是沾粘的
     */
    private fun pathResult(map: HashMap<String, Boolean>?) {
        lifeOwnerKeeper.forEach { entry ->
            val pair = entry.value
            if (pair.result) {
                val methodResult = pair.method?.invoke(entry.key, map)
                if (methodResult is Boolean && methodResult) {
                    pair.result = false
                }
            }
        }

        clazzKeeper.forEach { entry ->
            val pair = entry.value
            if (pair.result) {
                val methodResult = pair.method?.invoke(entry.key, map)
                if ((methodResult as Boolean)) {
                    pair.result = false
                }
            }
        }
    }



    fun inject(clazz: Class<*>): PermissionInject {
        if(clazzKeeper.contains(clazz)) return this
        clazzKeeper[clazz] = Pair(true, findAnoInClass(clazz::class.java))
        return this
    }

    fun unInject(clazz: Class<*>){
        clazzKeeper.remove(clazz)
    }

    fun inject(lifecycleOwner : LifecycleOwner): PermissionInject {
        if(lifeOwnerKeeper.contains(lifecycleOwner)) return this
        lifeOwnerKeeper[lifecycleOwner] = Pair(true, findAnoInClass(lifecycleOwner::class.java))
        lifecycleOwner.lifecycle.addObserver(object : LifeCompat() {
            override fun destroy() {
                lifeOwnerKeeper.remove(lifecycleOwner)
            }
        })
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

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            destroy()
        }

        abstract fun destroy()
    }
}