package com.alas.android.core.game.reward

import com.alas.android.core.base.AlasLog
import com.alas.android.core.base.ModuleBase
import com.alas.android.core.device.DeviceController
import com.alas.android.core.scheduler.Task

/**
 * 收获/奖励任务(对齐 ALAS `module/reward/reward.py`)。
 *
 * 负责领取主界面各类可领奖励(商店、每日抽卡、邮件等)。
 * 此为流程骨架，真实实现依赖模板资源与坐标配置。
 */
class RewardTask(
    device: DeviceController,
    server: String,
) : ModuleBase(device, server), Task {

    override val name = "Reward"

    override fun run() {
        AlasLog.i("Reward start")
        // 回到主界面
        device.keyEvent(android.view.KeyEvent.KEYCODE_HOME)
        sleep(1.0)
        collectMainRewards()
        AlasLog.i("Reward done")
    }

    private fun collectMainRewards() {
        try {
            val claim = template("reward/CLAIM.png")
            val shot = refresh()
            val hit = claim.matchResult(shot.mat, 0.85)
            if (hit != null && hit.point != null) {
                device.click(hit.point!!)
                sleep(0.5)
            }
        } catch (e: Exception) {
            AlasLog.w("reward templates missing, skip")
        }
    }
}
