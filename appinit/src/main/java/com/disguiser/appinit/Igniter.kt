package com.disguiser.appinit


interface Igniter {
    companion object {
        // 不需要隐私权限的
        const val MAX_PRIORITY = 10
        // 需要隐私权限的，不需要是主进程
        const val FORWARD_PRIORITY = 7
        // 需要是主进程
        const val NORMAL_PRIORITY = 5
        // 业务多入口
        const val BEHIND_PRIORITY = 3
        // 所有
        const val MIN_PRIORITY = 1
    }

    open fun getIgniterName() : String {
        return javaClass.simpleName
    }
}