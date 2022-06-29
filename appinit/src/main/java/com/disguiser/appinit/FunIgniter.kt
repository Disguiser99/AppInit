package com.disguiser.appinit


interface FunIgniter : Igniter {

    fun getPriority() : Int

    fun init()

}