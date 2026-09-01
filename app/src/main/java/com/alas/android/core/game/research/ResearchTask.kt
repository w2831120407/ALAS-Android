package com.alas.android.core.game.research

import com.alas.android.core.base.AlasLog
import com.alas.android.core.base.ModuleBase
import com.alas.android.core.device.DeviceController
import com.alas.android.core.scheduler.Task
import com.alas.android.core.vision.Button
import com.alas.android.core.vision.Roi

/**
 * 科研任务(对齐 ALAS `module/research/research.py`)。
 *
 * 流程：进入科研界面 -> 若可进行则选择/开始科研 -> 返回主界面。
 * 真实项目由模板 + JSON 配置驱动按钮坐标；此处为流程骨架。
 */
class ResearchTask(
    device: DeviceController,
    server: String,
) : ModuleBase(device, server), Task {

    override val name = "Research"

    private val researchEntry = Button(
        area = Roi(0, 0, 1280, 720),
        buttonArea = Roi(0, 0, 1280, 720),
        name = "RESEARCH_ENTRY",
    )

    override fun run() {
        AlasLog.i("Research start")
        // 进入科研
        device.click(researchEntry.randomPoint())
        sleep(1.0)
        // 尝试开始科研(模板匹配)
        try {
            val startBtn = template("research/START_RESEARCH.png")
            val shot = refresh()
            val hit = startBtn.matchResult(shot.mat, 0.85)
            if (hit != null && hit.point != null) {
                device.click(hit.point!!)
                sleep(0.8)
                AlasLog.i("Research started")
            } else {
                AlasLog.i("No research available to start")
            }
        } catch (e: Exception) {
            AlasLog.w("research template missing, skip")
        }
        // 返回
        device.keyEvent(android.view.KeyEvent.KEYCODE_BACK)
        sleep(1.0)
        AlasLog.i("Research done")
    }
}
