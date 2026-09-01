# 模板资源目录

将碧蓝航线游戏截图模板放入此目录，目录结构对齐 ALAS 的 `assets/<server>/<category>/`：

```
assets/templates/
├─ cn/                  # 服务器：cn / en / jp / tw
│  ├─ handler/          # 通用弹窗(登录、公告、信息栏、掉落实体)
│  │  ├─ LOGIN_ANNOUNCE.png
│  │  └─ MAIN_MENU.png
│  ├─ commission/       # 委托
│  │  ├─ HARVEST_ALL.png
│  │  └─ DISPATCH.png
│  ├─ research/         # 科研
│  │  └─ START_RESEARCH.png
│  ├─ reward/           # 奖励收获
│  │  └─ CLAIM.png
│  └─ daily/            # 每日
│     └─ CLAIM.png
├─ en/
├─ jp/
└─ tw/
```

## 模板规格

- 格式：PNG
- 尺寸：以游戏内实际截取元素为准(建议 720p 下截取)。
- 命名：大写，与代码中 `template("<category>/<NAME>.png")` 一致。
- 分辨率：识别坐标系固定为 **1280×720**(对齐 ALAS)，运行时自动校正旋转与缩放。

## 校准说明

当前代码中部分按钮坐标为**示意占位**，实际使用需对照你的设备分辨率与游戏 UI 校准。
通过 `Runtime` 选择不同连接模式后，模板会自动从本目录加载。
