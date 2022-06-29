package com.disguiser.appinit

import java.util.concurrent.CountDownLatch

abstract class AsyncFunIgniter: FunIgniter {

    val countDownLatch: CountDownLatch? by lazy {
        if (getDependencies()?.size != null) {
            CountDownLatch(getDependencies()?.size!!)
        } else {
            null
        }
    }

    fun awaitToWork() {
        countDownLatch?.await()
    }

    fun countDown() {
        countDownLatch?.countDown()
    }

    open fun isInMainThreadInit() : Boolean = true

    open fun getDependencies(): List<FunIgniter>? = null

}