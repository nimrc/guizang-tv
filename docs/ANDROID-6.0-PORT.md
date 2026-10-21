# Android 6.0（API 23）移植说明

## 背景

原项目 `minSdk 26`（Android 8.0），无法安装到 Android 6.0.1（API 23）的小米电视（已实测 MiTV4-ANSM0）。本移植将其降到 API 23 并修复由此暴露的运行时兼容性问题。

## 改动清单

1. `app/build.gradle`：`minSdk 26` → `minSdk 23`
2. `XiaomiControls.java`：
   - `enabled()` 白名单加入 `"MiTV4-ANSM0"`（该机型同样具备 `com.xiaomi.mitv.tvplayer/.ExternalSourceActivity` 及 `EXTSRC_PLAY`，HDMI 直达可用）
   - 删除 `startStockHome()`（依赖系统桌面 `com.mitv.tvhome`，接入 Home 键后该包已被卸载）
   - 删除 `label()` 中 `com.mitv.tvhome` 分支
3. `FrameHomeActivity.java`：
   - 修复 `List.sort()` 在 API 23 的崩溃（见下），改用 `Collections.sort()`
   - `onBackPressed()` 不再跳转小米桌面（作为桌面时按返回不退出）
   - 移除 `stockHome()`、`ComponentName` 导入
   - 入口示例改为：Apple TV（HDMI 2）/ 哔哩哔哩 / 小米投屏，`selected` 上限相应调整

## 关键兼容性问题

`java.util.List.sort(Comparator)` 是 Java 8 默认接口方法，Android 需 **API 24+** 才有。原 `minSdk 26` 恰好掩盖了该问题；降到 API 23 后运行时会抛 `NoSuchMethodError`（触发点：`showLibrary()`）。已改用 `Collections.sort(list, comparator)`，全 API 可用。

## 构建

要求 JDK 17、Gradle 8.9、Android SDK Platform 34：

```sh
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/Android/sdk
gradle :app:assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

## 验证范围

- 已实测：MiTV4-ANSM0 / Android 6.0.1 / armeabi-v7a
- 已验证：桌面启动、HDMI 直达（`input=24` 对应 HDMI 2，经 CEC 名称确认）、应用列表、壁纸轮播、返回键
- 未验证：其他机型/固件

## 备注

接入 Home 键需卸载系统桌面 `com.mitv.tvhome`（`adb shell pm uninstall --user 0 com.mitv.tvhome`），
可随时用 `adb shell pm install-existing com.mitv.tvhome` 恢复。详见用户侧还原指南。
