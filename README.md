# ALAS-Android

**碧蓝航线 (Azur Lane) 自动化脚本的安卓版。**

参照明日方舟小助手 **MAA (MaaAssistantArknights)** 的 Android 端技术方案，将 **ALAS (AzurLaneAutoScript)** 的 Python 架构移植到 Android 平台：

- **设备端自控**：`MediaProjection` 截图 + 原生 `InputManager`/无障碍手势注入，免电脑运行。
- **ADB 控制 / 无线调试**：`adb screencap` 截图 + `adb shell input`/`minitouch` 触控注入。
- **OpenCV 模板匹配** 识别层：`TM_CCOEFF_NORMED`，相似度阈值 0.85，二进制/亮度变体。
- **配置驱动调度器**：按 `next_run` + 优先级 + 启用状态取任务，成功后自动写回 `NextRun` 实现"无缝续跑"。

## 技术架构

| 层 | 位置 | 对应 MAA / ALAS 概念 |
| --- | --- | --- |
| Android 壳 | `ui/` `service/` | MaaTouch app / MAA GUI |
| 运行时引导 | `core/Runtime.kt` | ALAS `alas.py` |
| 设备控制 | `core/device/` | MAA `ControllerAPI` / ALAS `module/device` |
| 识别层 | `core/vision/` | MAA `Vision` / ALAS `module/base/template.py` |
| 调度层 | `core/scheduler/` `core/config/` | ALAS 中央调度器 / `module/config` |
| 基础层 | `core/base/` | ALAS `module/base` |

详细说明见 [docs/01-架构设计.md](docs/01-架构设计.md)，构建部署见 [docs/02-构建与部署.md](docs/02-构建与部署.md)。

## 目录结构

```
ALAS-Android/
├─ app/src/main/
│  ├─ AndroidManifest.xml
│  ├─ res/                      # 资源与无障碍配置
│  └─ java/com/alas/android/
│     ├─ AlasApplication.kt     # 入口，初始化 OpenCV/资源
│     ├─ ui/MainActivity.kt     # 主界面
│     ├─ service/               # 前台服务 / 无障碍 / MediaProjection
│     └─ core/
│        ├─ Runtime.kt          # 装配：config->device->scheduler
│        ├─ device/             # 截图源 + 输入注入 + ADB
│        ├─ vision/             # 模板匹配 / 按钮 / ROI
│        ├─ base/               # 日志 / 异常 / 定时器 / ModuleBase / 资源
│        ├─ config/             # 任务调度配置
│        ├─ scheduler/          # 中央调度器
│        └─ game/               # 玩法模块(委托/科研/每日/通用弹窗)
└─ docs/                        # 架构 / 构建部署 / 模块映射文档
```

## 快速开始

1. 安装 **Android Studio** + JDK 17 + Android SDK 34。
2. `./gradlew assembleDebug` 构建。
3. 选择运行模式：
   - **设备端自控**：开启无障碍服务，授予屏幕录制权限。
   - **ADB / 无线调试**：`adb connect <ip>:<port>` 后填写连接地址。
4. 将游戏截图模板放入 `assets/templates/<server>/<category>/`。
5. 校准各按钮坐标(当前代码中坐标为示意占位)。

## 许可与声明

本项目为学习研究用途，请勿用于违反游戏用户协议的用途。碧蓝航线图片资源版权归游戏厂商所有。

详见 [docs/](docs/) 目录。
