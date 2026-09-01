package com.alas.android.core.game.daily

import com.alas.android.core.base.AlasLog
import com.alas.android.core.base.ModuleBase
import com.alas.android.core.device.DeviceController
import com.alas.android.core.scheduler.Task

/**
 * 每日任务(对齐 ALAS `module/daily/daily.py`)。
 *
 * 负责：每日任务领取、困难图、演习、潜艇图等日常玩法。
 * 此骨架演示如何用状态机循环组合多个子玩法；
 * 真实实现需大量模板与坐标配置。
 */
class DailyTask(
    device: DeviceController,
    server: String,
) : ModuleBase(device, server), Task {

    override val name = "Daily"

    override fun run() {
        AlasLog.i("Daily start")
        collectDailyMission()
        doHardMode()
        doExercise()
        AlasLog.i("Daily done")
    }

    private fun collectDailyMission() {
        // 进入每日任务界面并领取
        device.keyEvent(android.view.KeyEvent.KEYCODE_BACK)
        sleep(1.0)
        AlasLog.i("Collecting daily mission rewards")
        try {
            val claim = template("daily/CLAIM.png")
            val shot = refresh()
            val hit = claim.matchResult(shot.mat, 0.85)
            if (hit != null && hit.point != null) {
                device.click(hit.point!!)
                sleep(0.5)
            }
        } catch (e: Exception) {
            AlasLog.w("daily templates missing, skip")
        }
    }

    private fun doHardMode() {
        sleep(1.0)
        AlasLog.i("Hard mode ... (config placeholder)")
    }

    private fun doExercise() {
        sleep(1.0)
        AlasLog.i("Exercise ... (config placeholder)")
    }
}
