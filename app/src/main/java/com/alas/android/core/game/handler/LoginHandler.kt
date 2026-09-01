package com.alas.android.core.game.handler

import com.alas.android.core.base.AlasLog
import com.alas.android.core.base.ModuleBase
import com.alas.android.core.device.DeviceController
import com.alas.android.core.vision.Button
import com.alas.android.core.vision.Roi

/**
 * 登录/公告等全局界面处理(对齐 ALAS `module/handler/login.py` + `module/handler/info_handler.py`)。
 *
 * 在进入具体玩法前调用 [ensureLoggedIn]，处理登录公告、更新、维护等弹窗。
 */
class LoginHandler(device: DeviceController, server: String) : ModuleBase(device, server) {

    private val loginAnnounce = Button(
        area = Roi(0, 0, 1280, 720),
        name = "LOGIN_ANNOUNCE",
    )
    private val loginCheck = Button(
        area = Roi(0, 0, 1280, 720),
        name = "LOGIN_CHECK",
    )

    /** 等待并确保进入主界面(处理登录弹窗)。 */
    fun ensureLoggedIn(timeoutMs: Long = 120_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            refresh()
            // 依次处理已知弹窗
            if (handleLoginPopups()) {
                sleep(1.0)
                continue
            }
            // 到达主界面：检测主界面标志(如左上角港口信息)，简化为主界面按钮出现
            if (atMainMenu()) {
                AlasLog.i("Logged in, reached main menu")
                return
            }
            sleep(0.5)
        }
        AlasLog.e("Login timeout")
    }

    private fun handleLoginPopups(): Boolean {
        // 通用弹窗关闭逻辑：模板匹配到关闭/确定按钮则点击
        return closeIfPresent("LOGIN_ANNOUNCE", loginAnnounce) ||
            closeIfPresent("LOGIN_CHECK", loginCheck)
    }

    private fun closeIfPresent(name: String, button: Button): Boolean {
        val t = try {
            template("handler/$name.png")
        } catch (e: Exception) {
            return false
        }
        val shot = image ?: refresh()
        val hit = t.matchResult(shot.mat, 0.85, button.area)
        if (hit != null && hit.point != null) {
            device.click(hit.point!!)
            return true
        }
        return false
    }

    private fun atMainMenu(): Boolean {
        // 主界面标志按钮(如顶部信息栏)。模板缺失时直接视为到达。
        return try {
            val t = template("handler/MAIN_MENU.png")
            val shot = image ?: refresh()
            t.match(shot.mat, 0.9)
        } catch (e: Exception) {
            true
        }
    }
}

/**
 * 通用信息/掉落弹窗处理(对齐 ALAS `module/handler/info_handler.py`)。
 * 在有多个"确认/确定/关闭"按钮时选择性地点击。
 */
class InfoHandler(device: DeviceController, server: String) : ModuleBase(device, server) {

    /** 关闭当前屏幕上任意一个已定义的弹窗按钮；返回是否处理了任意一个。 */
    fun handleAny(): Boolean {
        refresh()
        val candidates = listOf(
            "INFO_BAR_1",
            "INFO_BAR_2",
            "INFO_BAR_3",
            "GET_ITEMS_SHIP_1",
        )
        for (name in candidates) {
            val t = try {
                template("handler/$name.png")
            } catch (e: Exception) {
                continue
            }
            val shot = image ?: refresh()
            val hit = t.matchResult(shot.mat, 0.85)
            if (hit != null && hit.point != null) {
                device.click(hit.point!!)
                return true
            }
        }
        return false
    }
}
