package com.alas.android.core.game.commission

import com.alas.android.core.base.AlasLog
import com.alas.android.core.base.GameStuckError
import com.alas.android.core.base.ModuleBase
import com.alas.android.core.base.TaskEnd
import com.alas.android.core.device.DeviceController
import com.alas.android.core.scheduler.Task
import com.alas.android.core.vision.Button
import com.alas.android.core.vision.Roi

/**
 * 委托任务(对齐 ALAS `module/commission/commission.py`)。
 *
 * 流程：
 *  1. 进入"作战 -> 委托"界面
 *  2. 若存在完成的委托则"收获"
 *  3. 若有可出发的委托则"派遣"
 *  4. 返回主界面
 *
 * 具体按钮坐标/模板依赖 assets/templates/<server>/commission/ 下的资源，
 * 此处给出流程骨架(坐标仅示意，需按实际游戏 UI 校准)。
 */
class CommissionTask(
    device: DeviceController,
    server: String,
) : ModuleBase(device, server), Task {

    override val name = "Commission"

    override fun run() {
        AlasLog.i("Commission start")
        try {
            enterCommission()
            harvestIfReady()
            dispatchIfPossible()
            backToMain()
        } catch (e: TaskEnd) {
            AlasLog.i("Commission ended: ${e.message}")
        }
        AlasLog.i("Commission done")
    }

    private fun enterCommission() {
        // 点击"作战"主按钮(示意坐标)
        device.click(clickStart())
        sleep(1.0)
        // 点击"委托"
        device.click(commissionTab())
        sleep(1.0)
    }

    private fun harvestIfReady() {
        val harvest = try {
            template("commission/HARVEST_ALL.png")
        } catch (e: Exception) {
            return
        }
        val shot = refresh()
        if (harvest.match(shot.mat, 0.85)) {
            device.click(harvestResultPoint(shot))
            sleep(1.0)
            AlasLog.i("Commission harvested")
        }
    }

    private fun dispatchIfPossible() {
        val dispatch = try {
            template("commission/DISPATCH.png")
        } catch (e: Exception) {
            return
        }
        val shot = refresh()
        if (dispatch.match(shot.mat, 0.85)) {
            device.click(dispatchResultPoint(shot))
            sleep(0.8)
        }
    }

    private fun backToMain() {
        device.keyEvent(android.view.KeyEvent.KEYCODE_BACK)
        sleep(1.0)
        device.keyEvent(android.view.KeyEvent.KEYCODE_BACK)
    }

    // ---- 示意坐标占位(真实项目中由 assets/JSON 配置驱动) ----
    private fun clickStart() = com.alas.android.core.vision.Point(640, 600)
    private fun commissionTab() = com.alas.android.core.vision.Point(400, 600)
    private fun harvestResultPoint(shot: com.alas.android.core.vision.Screenshot) =
        com.alas.android.core.vision.Point(shot.width / 2, shot.height / 2)
    private fun dispatchResultPoint(shot: com.alas.android.core.vision.Screenshot) =
        com.alas.android.core.vision.Point(shot.width / 2, shot.height / 2 + 50)
}
