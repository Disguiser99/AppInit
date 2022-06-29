package com.disguiser.appinit


import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList


object AppInitMgr {

    const val TAG = "AppInitMgr"

    private val funIgniters: LinkedHashMap<String, FunIgniter> = linkedMapOf()
    private val initedIgniters: MutableList<String> = mutableListOf()

    // 保存任务与其子任务的依赖关系
    private val igniterChildrenMap: MutableMap<String, ArrayList<String>> = hashMapOf()

    private val mExecutorService : ExecutorService by lazy  {
        getDefaultExecutor()
    }

    @Volatile
    private var finishTaskNum = 0

    private var startTime = 0L
    private var startTaskNum = 0


    fun addIgniterList(igniterList: List<Igniter>) : AppInitMgr {
        for(igniter in igniterList) {
            addIgniter(igniter)
        }
        return this
    }

    fun addIgniter(igniter: Igniter): AppInitMgr {
        if (igniter is ModuleIgniter) {
            Degod.e(TAG, "addModuleIgniter: ${igniter.getIgniterName()}" )
            val igniterList = igniter.getFunIgniterList()
            if (igniterList.isNotEmpty()) {
                for (igniterOfModule in igniterList) {
                    funIgniters[igniterOfModule.getIgniterName()] = igniterOfModule
                }
            }
        } else if (igniter is FunIgniter){
            Degod.e(TAG, "addIgniter: ${igniter.getIgniterName()}" )
            funIgniters[igniter.getIgniterName()] = igniter
        }
        return this
    }

    fun addIgniter(igniterName: String, priority: Int, initMethod: InitMethod): AppInitMgr {
        Degod.e(TAG, "addIgniter: $igniterName" )
        funIgniters[igniterName] = object : FunIgniter {
            override fun getPriority(): Int {
                return priority
            }

            override fun init() {
                initMethod.onInit()
            }

            override fun getIgniterName(): String {
                return igniterName
            }

        }
        return this
    }

    /**
     * init all Igniter
     */
    fun init() {
        init(Igniter.MIN_PRIORITY)
    }

    fun init(name : String) {
        if (initedIgniters.contains(name)) {
            Degod.e("hasInit: $name")
        }
        if (funIgniters.containsKey(name)) {
            funIgniters[name]?.init()
            funIgniters.remove(name)
        } else {
            Degod.e("has not add Igniter $name")
        }
    }

    fun init(priority: Int) {
        if (funIgniters.isNotEmpty()) {
            val funIgniterList = ArrayList<FunIgniter>(funIgniters.values)
            onAllInitTaskStart()
            // 保持不同优先级中的相对顺序
            Collections.sort(funIgniterList, FunIgniterComparator())
            // 建立任务之间的先行关系
            buildDependenciesRelation(funIgniterList)
            val iterator = funIgniterList.iterator()
            while (iterator.hasNext()) {
                val igniter = iterator.next()
                if (initedIgniters.contains(igniter.getIgniterName())) {
                    Degod.e(TAG, "hasInit: ${igniter.getIgniterName()}")
                    // 删除已经初始化的
                    funIgniters.remove(igniter.getIgniterName())
                    Degod.e("remain" + funIgniters.keys)
                } else {
                    if (igniter.getPriority() >= priority) {
                        if (igniter is AsyncFunIgniter) {
                            if (igniter.isInMainThreadInit()) {
                                igniter.awaitToWork()
                                callInit(igniter)
                            } else {
                                // 子线程
                                callInitOtherThread(igniter)
                            }
                        } else {
                            callInit(igniter)
                        }
                    } else {
                        // 排过序
                        break
                    }
                }
            }

        }
    }

    /**
     * 建立任务之间的先行关系
     */
    private fun buildDependenciesRelation(funIgniterList: ArrayList<FunIgniter>) {
        funIgniterList.forEach { funIgniter ->
            if (funIgniter is AsyncFunIgniter) {
                funIgniter.getDependencies()?.forEach { dependencies ->
                    var list = igniterChildrenMap[dependencies.getIgniterName()]
                    if (list == null) {
                        list = ArrayList()
                    }
                    list.add(funIgniter.getIgniterName())
                    igniterChildrenMap[dependencies.getIgniterName()] = list
                }
            }
        }
    }

    private fun callInit(igniter: FunIgniter) {
        val startTime = System.currentTimeMillis()
        igniter.init()
        val endTime = System.currentTimeMillis()
        initedIgniters.add(igniter.getIgniterName())
        // 删除已经初始化的
        funIgniters.remove(igniter.getIgniterName())
        Degod.e("init: ${igniter.getIgniterName()} total cost ${endTime - startTime} ms, in ${Thread.currentThread()}")//, now remain + ${funIgniters.keys}")
        notifyChildrenIgniter(igniter)
        onInitTaskFinished()
    }

    private fun callInitOtherThread(igniter: AsyncFunIgniter) {
        mExecutorService.execute {
            igniter.awaitToWork()
            callInit(igniter)
        }
    }

    private fun notifyChildrenIgniter(igniter: FunIgniter) {
        igniterChildrenMap[igniter.getIgniterName()]?.forEach { igniterName ->
            if (funIgniters[igniterName] != null && funIgniters[igniterName] is AsyncFunIgniter) {
                (funIgniters[igniterName] as? AsyncFunIgniter)?.countDown()
            }
        }
    }

    private fun onAllInitTaskStart() {
        startTaskNum = funIgniters.size
        startTime = System.currentTimeMillis()
    }

    private fun onInitTaskFinished() {
        if (++finishTaskNum == startTaskNum) {
            Degod.e("本次共初始化${startTaskNum}个Igniter，共耗时${System.currentTimeMillis() - startTime} ms")
        }
    }


    private open class FunIgniterComparator : Comparator<FunIgniter> {
        override fun compare(o1: FunIgniter?, o2: FunIgniter?): Int {
            return o2!!.getPriority() - o1!!.getPriority()
        }

    }

    interface InitMethod {
        fun onInit()
    }

    private fun getDefaultExecutor(): ExecutorService {
        return Executors.newSingleThreadExecutor()
    }
}