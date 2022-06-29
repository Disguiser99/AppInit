package com.disguiser.appinit

import android.os.Build
import android.util.Log

import java.util.regex.Pattern

/**
 * Description: util for simplify log
 * Created by Disguiser on 2021/10/14 18:33
 *
 */
object Degod  {

    private const val MAX_LOG_LENGTH = 4000
    private const val MAX_TAG_LENGTH = 23
    private const val CALL_STACK_INDEX = 4
    private val ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$")

    private const val DEFAULT_CONFIG = "default_config"
    private val configMap : HashMap<String, Config> = HashMap()
    private var mConfigName: String = DEFAULT_CONFIG


    init {
        configMap[DEFAULT_CONFIG] = Config()
    }

    class Config {
        /**
         * set temp tag, just use one time
         */
        var mTag: String? = null

        /**
         * set loggable priority to limit log minimum priority
         */
        var mLoggablePriority = Log.INFO

        /**
         * decide loggable
         */
        var mIsLoggable = true

        /**
         * choose TAG mode
         * example:
         *     TAG_MODE_DECLARING_CLASS com.*.MainActivity$setAvMediaListener
         *     TAG_MODE_FILE_NAME MainActivity
         */
        var mTagMode = TagMode.TAG_MODE_DECLARING_CLASS

        enum class TagMode{
            TAG_MODE_DECLARING_CLASS,
            TAG_MODE_FILE_NAME
        }

        /**
         * return whether a msg at 'priority' should be logged
         */
        open fun isLoggable(priority: Int): Boolean {
            return priority >= mLoggablePriority
        }

    }

    /**
     * log a verbose message with optional args
     */
    @JvmStatic
    fun v(vararg args: Any?) {
        preLog(Log.VERBOSE, *args)
    }

    /**
     * log a verbose message with message format optional args
     */
    @JvmStatic
    fun vM(message: String, vararg args: Any?) {
        preLogM(Log.VERBOSE, message, *args)
    }

    /**
     * log a debug message with optional args
     */
    @JvmStatic
    fun d(vararg args: Any?) {
        preLog(Log.DEBUG, *args)
    }

    /**
     * log a debug message with message format optional args
     */
    @JvmStatic
    fun dM(message: String, vararg args: Any?) {
        preLogM(Log.DEBUG, message, *args)
    }

    /**
     * log a info message with optional args
     */
    @JvmStatic
    fun i(vararg args: Any?) {
        preLog(Log.INFO, *args)
    }

    /**
     * log a info message with message format optional args
     */
    @JvmStatic
    fun iM(message: String, vararg args: Any?) {
        preLogM(Log.INFO, message, *args)
    }

    /**
     * log a warn message with optional args
     */
    @JvmStatic
    fun w(vararg args: Any?) {
        preLog(Log.WARN, *args)
    }

    /**
     * log a warn message with message format optional args
     */
    @JvmStatic
    fun wM(message: String, vararg args: Any?) {
        preLogM(Log.WARN, message, *args)
    }

    /**
     * log a error message with optional args
     */
    @JvmStatic
    fun e(vararg args: Any?) {
        preLog(Log.ERROR, *args)
    }

    /**
     * log a error message with message format optional args
     */
    @JvmStatic
    fun eM(message: String, vararg args: Any?) {
        preLogM(Log.ERROR, message, *args)
    }

    /**
     * log a asset message with optional args
     */
    fun a(vararg args: Any?) {
        preLog(Log.ASSERT, *args)
    }

    /**
     * log a asset message with message format optional args
     */
    fun a(message: String, vararg args: Any?) {
        preLogM(Log.ASSERT, message, *args)
    }

    /**
     * set one time Tag
     * Java use it lint will toast Static member 'com.youdao.ydlive.activity.Degod.e(java.lang.Object...)' accessed via instance reference
     */
    @JvmStatic
    fun tag(tag: String): Degod {
        configMap[mConfigName]?.mTag = tag
        return this
    }

    @JvmStatic
    fun addConfig(configName: String, config: Config) {
        configMap[configName] = config
    }

    @JvmStatic
    fun chooseConfig(configName: String) : Boolean {
        return if (configMap.containsKey(configName)) {
            mConfigName = configName
            true
        } else {
            false
        }
    }

    @JvmStatic
    fun addConfigAndChoose(configName: String, config: Config) : Degod {
        addConfig(configName, config)
        chooseConfig(configName)
        return this
    }

    @JvmStatic
    fun setLoggable(loggable: Boolean) {
        configMap[mConfigName]?.mIsLoggable = loggable
    }

    @JvmStatic
    fun setLoggable(configName: String, loggable: Boolean) {
        configMap[configName]?.mIsLoggable = loggable
    }

    @JvmStatic
    fun setLoggableAndChoose(configName: String, loggable: Boolean) : Degod {
        setLoggable(configName, loggable)
        chooseConfig(configName)
        return this
    }

    @JvmStatic
    fun setTAGMode(tagMode: Config.TagMode) {
        configMap[mConfigName]?.mTagMode = tagMode
    }

    @JvmStatic
    fun setTAGMode(configName: String, tagMode: Config.TagMode) {
        configMap[configName]?.mTagMode = tagMode
    }

    private fun preLog(priority: Int, vararg args: Any?) {
        if (!(configMap[mConfigName]!!.mIsLoggable && configMap[mConfigName]!!.isLoggable(priority))) {
            return
        }
        if (args.isNullOrEmpty()) {
            return
        }
        val msg = formatMsg(*args)
        dealLog(getTag(), priority, msg)
    }

    private fun preLogM(priority: Int, message: String, vararg args: Any?) {
        if (!(configMap[mConfigName]!!.mIsLoggable && configMap[mConfigName]!!.isLoggable(priority))) {
            return
        }
        val msg = formatMsg(message, *args)
        dealLog(getTag(), priority, msg)
    }

    private fun getTag(): String? {
        val tag = blankTag()
        if (!tag.isNullOrEmpty()) {
            return tag
        }
        return getStackElementTag()
    }

    private fun getStackElementTag(): String {
        val stackTrace = Throwable().stackTrace
        check(stackTrace.size > CALL_STACK_INDEX) {
            "Synthetic stacktrace didn't have enough elements"
        }
        var tag = when(configMap[mConfigName]!!.mTagMode) {
            Config.TagMode.TAG_MODE_FILE_NAME -> {
                stackTrace[CALL_STACK_INDEX].fileName
            }
            else -> {
                stackTrace[CALL_STACK_INDEX].className
            }
        }

        val m = ANONYMOUS_CLASS.matcher(tag)
        if (m.find()) {
            tag = m.replaceAll("")
        }
        tag = tag.substring(tag.lastIndexOf('.') + 1)
        // tag length limit was removed in API 24
        return if (tag.length <= MAX_TAG_LENGTH || Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tag
        } else {
            tag.substring(0, MAX_TAG_LENGTH)
        }

    }

    private fun blankTag() : String? {
        return blankTag(configMap[mConfigName])
    }

    private fun blankTag(config: Config?) : String? {
        var tag: String? = null
        if (config != null) {
            tag = config.mTag
            config.mTag = null
        }
        return tag
    }

    private fun formatMsg(vararg args: Any?): String? {
        val msgBuilder = StringBuilder()
        if (args.isNullOrEmpty()) {
            return null
        }
        // TODO wait to improve format
        for (param in args) {
            msgBuilder.append(param.toString() + ", ")
        }
        return msgBuilder.toString()
    }

    private fun formatMsg(message: String?, vararg args: Any?): String? {
        if (message.isNullOrEmpty()) {
            return null
        }
        return String.format(message, *args)
    }

    private fun dealLog(tag: String?, priority: Int, msg: String?) {
        if (tag.isNullOrEmpty() || msg.isNullOrEmpty()) {
            return
        }

        var message = msg
        // if out of MAX_LOG_LENGTH, split
        while (message!!.length > MAX_LOG_LENGTH) {
            realLog(tag, priority, message.substring(0, MAX_LOG_LENGTH))
            message = message.substring(MAX_LOG_LENGTH)
        }
        realLog(tag, priority, message)
    }

    private fun realLog(tag: String, priority: Int, msg: String) {
        if (priority == Log.ASSERT) {
            Log.wtf(tag, msg)
        } else {
            Log.println(priority, tag, msg)
        }
    }


}