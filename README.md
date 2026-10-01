# Nowdex-Android

**在 Android 上查看 Codex、Claude、Cursor 等 AI 智能体的用量与额度。**

[Nowdex](https://nowdex.app) 安卓版：把 AI 剩余额度和重置时间放到 Android 主屏幕和通知栏上——不用开着 Mac，也不用打开网页。灵感来自 [CodexBar](https://github.com/steipete/CodexBar)。

## 功能

- **状态页**：一览所有已启用 AI 服务的额度用量（5 小时窗口 / 每周 / 每月）、剩余百分比、重置时间。
- **用量页**：今日 Token 数与费用、输入/输出/缓存命中/缓存写入构成、模型费用明细、每日活动热力图、连续活跃天数、月度预估。
- **主屏幕小组件**：单服务额度小组件，浅色/深色自适应，点击打开应用。
- **常驻通知**：通知栏常驻显示各服务额度，是 Mac 菜单栏在 Android 上的等价实现。
- **设置页**：服务开关、主题（跟随系统/浅色/深色）、通知与自动刷新、隐私说明。

## 支持的服务

Codex · Claude · Cursor · Grok · Kimi · GLM · MiniMax · Qoder · DeepSeek · OpenCode Go

## 隐私

- 凭证保存在设备本地（加密存储）。
- 由设备直接向各服务请求用量，Nowdex 不在自己的服务器上保存密码或用量。
- 无需注册 Nowdex 账号。

> 当前仓库使用 Mock 数据演示完整 UI。接入真实数据时，为每个服务实现 Provider（OAuth / Cookie / API Key / CLI 会话）即可，无需改动 UI 层。

## 技术栈

- Kotlin + Jetpack Compose + Material 3
- DataStore（偏好设置）
- Glance（主屏幕 App Widget）
- 前台服务（常驻通知）

## 构建

```bash
./gradlew assembleDebug      # Debug APK
./gradlew assembleRelease    # Release APK（未签名）
```

需要 JDK 17 + Android SDK 34。CI 见 `.github/workflows/build-apk.yml`。

## 目录结构

```
app/src/main/java/com/nowdex/android/
├─ NowdexApplication.kt       # 入口，通知渠道
├─ MainActivity.kt            # 底部导航：状态 / 用量 / 设置
├─ data/
│  ├─ model/                  # AiService / ServiceQuota / DailyUsage 等
│  ├─ repository/             # UsageRepository + MockUsageRepository
│  ├─ SettingsStore.kt        # DataStore 偏好
│  └─ AppContainer.kt         # 简易依赖容器
├─ ui/
│  ├─ theme/                  # Material 3 深浅色主题
│  ├─ components/             # ServiceCard / QuotaBar 等
│  ├─ status/                 # 状态页
│  ├─ usage/                  # 用量页
│  └─ settings/               # 设置页
├─ widget/                    # Glance 主屏幕小组件
└─ service/                   # 常驻通知前台服务
```
