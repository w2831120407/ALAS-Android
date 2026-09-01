package com.alas.android.core.base

/**
 * ALAS 异常体系(对齐 ALAS `module/exception.py`)。
 */
open class AlasException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** 任务正常结束(用于主动跳出任务循环)。 */
class TaskEnd(message: String = "task ended") : AlasException(message)

/** 任务进入死循环/卡死(需要重启游戏或终止任务)。 */
class GameStuckError(message: String = "game stuck") : AlasException(message)

/** 请求人工接管(连续失败次数过多)。 */
class RequestHumanTakeover(message: String = "too many failures, requesting human") : AlasException(message)

/** 脚本逻辑错误(配置/资源缺失)。 */
class ScriptError(message: String = "script error") : AlasException(message)
