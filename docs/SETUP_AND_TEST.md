# yidianmofa 配置与测试指南

本指南面向开发者，详细介绍如何在 Android Studio 中打开本项目、配置 Pico Spatial SDK 开发环境、使用 Pico XR 模拟器或真机进行测试，以及如何配合后端服务完成完整流程验证。

---

## 目录

1. [开发环境要求](#1-开发环境要求)
2. [安装 Pico Spatial SDK 编辑器工具](#2-安装-pico-spatial-sdk-编辑器工具)
3. [配置 Android Studio](#3-配置-android-studio)
4. [配置 local.properties](#4-配置-localproperties)
5. [运行与测试](#5-运行与测试)
   - [5.1 使用 Pico XR 模拟器](#51-使用-pico-xr-模拟器)
   - [5.2 使用 Pico 4 Ultra 真机](#52-使用-pico-4-ultra-真机)
6. [后端服务配置](#6-后端服务配置)
7. [功能测试清单](#7-功能测试清单)
8. [常用 ADB 命令](#8-常用-adb-命令)
9. [常见问题排查](#9-常见问题排查)

---

## 1. 开发环境要求

| 组件 | 推荐版本 | 说明 |
|------|---------|------|
| Android Studio | 最新稳定版（建议 Ladybug 或更新） | 支持 Kotlin 2.0 与 Gradle 8.13 |
| JDK | 17 或 21 | Gradle 8.13 需要 JDK 17+ |
| Android SDK | API 35 | `compileSdk = 35`，`minSdk = 35` |
| Gradle | 8.13 | 由 `gradle/wrapper/gradle-wrapper.properties` 指定 |
| Pico Spatial SDK | 0.13.3 | 由 `gradle/libs.versions.toml` 中的 `bom` 指定 |
| Pico 编辑器工具 | 0.13.x | 用于 Spatial Pack 与资产打包 |
| 操作系统 | Windows 10/11 | 当前项目配置基于 Windows 路径 |

### 1.1 必须安装的 Android SDK 组件

打开 Android Studio 的 **SDK Manager**（`Tools > SDK Manager`），在 **SDK Platforms** 中安装：

- ✅ `Android 15.0 (API 35)` —— 必需

在 **SDK Tools** 中安装：

- ✅ `Android SDK Build-Tools 35`
- ✅ `Android SDK Platform-Tools`（包含 adb）
- ✅ `Android Emulator`（如使用模拟器）
- ✅ `Intel x86 Emulator Accelerator (HAXM)` 或 Windows Hypervisor Platform（如使用 x86 模拟器）

---

## 2. 安装 Pico Spatial SDK 编辑器工具

Pico Spatial SDK 除了 Gradle 依赖外，还需要安装桌面端编辑器工具，用于处理 Spatial Pack 和 3D 资产。

### 2.1 下载与安装

1. 访问 [Pico 开发者平台](https://developer.pico-interactive.com/) 或 Pico 官方文档站点。
2. 下载 **Pico Spatial SDK 0.13.x** 对应的编辑器工具包。
3. 解压到固定目录，例如：

   ```text
   D:\PICO\0.13\editor
   ```

   目录下应包含：

   ```text
   D:\PICO\0.13\editor\SpatialEditor\
   D:\PICO\0.13\editor\package.xml
   ```

### 2.2 验证编辑器工具

打开命令行，检查 `package.xml` 中的版本：

```bash
cat "D:\PICO\0.13\editor\package.xml"
```

应能看到类似：

```xml
<package version="0.13.4" ... />
```

> 注意：编辑器工具版本与 SDK BOM 版本可能略有差异（如 0.13.4 vs 0.13.3），这通常是正常的。

---

## 3. 配置 Android Studio

### 3.1 打开项目

1. 启动 Android Studio。
2. 选择 **Open**，然后选择 `yidianmofa` 项目根目录。
3. 等待 Gradle 同步完成。首次同步可能需要下载 Gradle 8.13 和各种依赖，耗时数分钟。

### 3.2 检查 Gradle JDK

如果同步报错提示 JDK 版本过低，请检查：

1. 打开 `File > Settings > Build, Execution, Deployment > Build Tools > Gradle`。
2. 在 **Gradle JDK** 中选择 JDK 17 或更高版本。
3. 重新同步：`File > Sync Project with Gradle Files`。

### 3.3 检查插件

确保 Android Studio 已启用：

- Android
- Kotlin
- Gradle

---

## 4. 配置 local.properties

项目根目录需要 `local.properties` 文件，用于指定 Android SDK 和 Pico 编辑器工具路径。

### 4.1 文件位置

`local.properties`（项目根目录，已存在，请勿提交到 git）

### 4.2 示例内容

```properties
# Android SDK 路径（根据你的实际安装位置修改）
sdk.dir=D:\\Android\\Sdk

# Pico Spatial Editor 工具路径（根据你的实际安装位置修改）
spatial.tools.dir=D:\\PICO\\0.13\\editor
```

### 4.3 路径说明

- `sdk.dir`：指向你的 Android SDK 根目录。
- `spatial.tools.dir`：指向 Pico 编辑器工具的根目录，即包含 `SpatialEditor` 文件夹的目录。

### 4.4 验证配置

同步 Gradle 后，在 Build 输出窗口中应能看到类似日志：

```text
[SpatialPackPlugin]  [SpatialEditor] resolved home = D:\PICO\0.13\editor\SpatialEditor
[SpatialPackPlugin]  [SpatialEditor] validation passed
[SpatialPackPlugin]  Resolved real Spatial Editor version: 0.13.4
```

如果看到 `validation failed` 或路径找不到，请检查 `spatial.tools.dir` 是否正确。

---

## 5. 运行与测试

### 5.1 使用 Pico XR 模拟器

Pico 提供了基于 Android Emulator 的 XR 模拟器，可用于无真机开发调试。

#### 5.1.1 安装 Pico XR 模拟器

1. 在 Pico 开发者文档中下载 Pico XR Emulator 系统镜像和模拟器皮肤。
2. 将系统镜像放到 Android SDK 的 `system-images/` 目录下，或在 Android Studio 的 **AVD Manager** 中选择 Pico 提供的镜像。
3. 打开 Android Studio 的 **Device Manager**（`View > Tool Windows > Device Manager`）。
4. 点击 **Create Device**。
5. 在 **Category** 中选择 **XR** 或搜索 **Pico**。
6. 选择 Pico 4 Ultra 或对应的模拟器设备定义。
7. 选择系统镜像（API 35，arm64-v8a）。
8. 完成创建并启动模拟器。

#### 5.1.2 启动模拟器

1. 在 **Device Manager** 中点击模拟器旁的 **启动** 按钮。
2. 等待 Pico 空间环境加载完成。
3. 在 Android Studio 顶部工具栏中选择该模拟器作为目标设备。
4. 点击 **Run**（`Shift + F10`）部署应用。

#### 5.1.3 模拟器交互

- 使用鼠标模拟头控/视线
- 使用键盘快捷键模拟手柄按钮（具体快捷键参考 Pico 模拟器文档）
- 在空间窗口中点击灵宝 UI 进行交互

### 5.2 使用 Pico 4 Ultra 真机

#### 5.2.1 开启开发者模式

1. 在 Pico 设备上打开 **设置**。
2. 进入 **关于手机 / 关于本机**。
3. 连续点击 **版本号** 7 次，开启开发者模式。
4. 返回设置，进入 **开发者选项**。
5. 开启 **USB 调试**。

#### 5.2.2 连接设备

1. 使用 USB-C 数据线将 Pico 4 Ultra 连接到开发电脑。
2. 在 Pico 设备上允许 USB 调试授权。
3. 在 Android Studio 的 **Device Manager** 中应能看到 `PICO` 设备。
4. 选择该设备，点击 **Run** 部署。

#### 5.2.3 无线调试（可选）

如果不想一直插线：

```bash
# 1. 通过 USB 连接后设置端口
adb tcpip 5555

# 2. 查看设备 IP（在 Pico 设置 > WLAN > 当前网络详情）
adb connect <PICO_IP>:5555

# 3. 拔掉 USB，在 Android Studio 中选择无线设备运行
```

---

## 6. 后端服务配置

项目中的会议分析后端对应原 `XR-Demo1.0/tree_mem`（Flask + Whisper + pipeline）。开发期通常运行在 PC 上，通过 `adb reverse` 让 Pico 访问。

### 6.1 后端目录

```text
XR-Demo1.0/
├── start.sh
├── tree_mem/          # Python Flask 后端（子模块或独立仓库）
└── voice_config.example.js
```

> `XR-Demo1.0/` 未包含在当前 git 仓库中（已在 `.gitignore` 中排除），请单独维护。

### 6.2 启动后端

```bash
cd XR-Demo1.0
./start.sh live
```

默认监听端口 `5001`。启动成功后应能看到：

```text
 * Running on http://127.0.0.1:5001
```

### 6.3 配置 ADB 反向端口转发

让 Pico 上的 `localhost:5001` 映射到 PC 的 `localhost:5001`：

```bash
adb reverse tcp:5001 tcp:5001
```

### 6.4 验证后端连接

在 Pico 浏览器或 PC 浏览器中访问：

```text
http://localhost:5001/api/upload_status?hash=test
```

应返回 JSON 响应。

### 6.5 修改后端地址

默认后端地址为 `http://localhost:5001`，定义在 `LingbaoPrefs.kt` 中。如需修改为局域网 IP：

1. 在应用中提供设置入口修改 `LingbaoPrefs.backendBaseUrl`
2. 或直接修改代码中的 `DEFAULT_BACKEND_URL`
3. 后续可在 UI 中动态配置

---

## 7. 功能测试清单

### 7.1 Phase 1 静态渲染测试

- [ ] 应用在 Pico 设备/模拟器上编译并启动
- [ ] 平面窗口（880×600 dp）正常出现
- [ ] 灵宝 UI 渲染成功，背景为白色
- [ ] 灵宝头像（`lingbao.gif` / `lingbao-large.png`）正常加载
- [ ] 设备图标（phone / window / computer）正常显示
- [ ] GIF 动画正常播放（录音中、处理中状态）
- [ ] 预录 MP3 音频在 JS 触发时正常播放
- [ ] 鼠标/视线/手柄交互正常（悬停、点击）

### 7.2 Phase 2 语音与音频测试

- [ ] 点击灵宝图标进入录音状态
- [ ] 再次点击停止录音并进入处理状态
- [ ] Android TTS 能朗读中文回复
- [ ] 预录语音（intro / wake / name / confirm）正常播放
- [ ] 输入称呼后重启应用，称呼能记住
- [ ] `cache` 模式：停止录音后显示固定回复
- [ ] `live` 模式：录音上传后端并返回 hash
- [ ] 轮询 `/api/upload_status` 正常，最终收到结果
- [ ] 唤醒词「灵宝」能触发录音（受 SpeechRecognizer 可用性影响）

### 7.3 切换 demo 模式

在 `index.html` 的 URL 中可通过 `mode` 参数切换：

```text
file:///android_asset/lingbao/index.html?mode=cache
file:///android_asset/lingbao/index.html?mode=live
```

当前默认值为 `cache`。

---

## 8. 常用 ADB 命令

```bash
# 查看已连接设备
adb devices

# 安装 APK
adb install app/build/outputs/apk/debug/app-debug.apk

# 查看应用日志
adb logcat -s LingbaoBridge:D WakeWordDetector:D AudioRecorder:D TtsEngine:D BackendApi:D

# 反向端口转发（后端调试）
adb reverse tcp:5001 tcp:5001

# 清除反向转发
adb reverse --remove tcp:5001

# 进入设备 shell
adb shell

# 查看应用数据目录
adb shell run-as com.example.yidianmofa ls -R files/

# 拉取录音文件
adb shell run-as com.example.yidianmofa cp /data/data/com.example.yidianmofa/cache/lingbao_recording_*.m4a /sdcard/
adb pull /sdcard/lingbao_recording_*.m4a ./
```

---

## 9. 常见问题排查

### 9.1 Gradle 同步失败：找不到 Spatial SDK

**现象**：同步时报错 `Could not find com.pico.spatial:bom:0.13.3`。

**排查**：

1. 检查 `settings.gradle.kts` 中的仓库配置是否包含 Pico 仓库。
2. 确认网络能访问 `https://artifact.bytedance.com/repository/Volcengine`。
3. 尝试在 `gradle.properties` 中禁用离线模式：

   ```properties
   org.gradle.offline=false
   ```

### 9.2 SpatialPackPlugin validation failed

**现象**：Build 日志显示 `[SpatialPackPlugin] validation failed`。

**排查**：

1. 检查 `local.properties` 中的 `spatial.tools.dir` 是否指向正确目录。
2. 确认目录下存在 `SpatialEditor/SpatialEditor.exe` 或对应可执行文件。
3. 重新同步 Gradle。

### 9.3 模拟器无法启动或找不到 Pico 镜像

**现象**：AVD Manager 中没有 Pico 设备选项。

**排查**：

1. 确认已安装 Pico XR Emulator 系统镜像。
2. 镜像需要与项目 `minSdk = 35` 匹配。
3. 如 Pico 未提供 API 35 镜像，可尝试使用真机调试。

### 9.4 WebView 不显示内容

**现象**：空间窗口出现但空白。

**排查**：

1. 检查 `app/src/main/assets/lingbao/index.html` 是否存在。
2. 查看 logcat 是否有 `ERR_FILE_NOT_FOUND` 或 JavaScript 错误。
3. 确认 `LingbaoWebView.kt` 中加载的 URL 为 `file:///android_asset/lingbao/index.html`。

### 9.5 录音无反应

**现象**：点击灵宝图标后没有录音状态。

**排查**：

1. 检查 `RECORD_AUDIO` 权限是否被授予。
2. 查看 logcat 是否有 `SecurityException`。
3. 当前版本未实现运行时权限申请，需先在系统设置中手动授予权限，或实现 #3 issue。

### 9.6 TTS 不发声

**现象**：UI 显示在说话但没有声音。

**排查**：

1. 检查 Pico 设备音量。
2. 在系统设置中确认已安装中文 TTS 引擎。
3. 查看 logcat 中 `TtsEngine` 初始化状态。

### 9.7 后端连接失败

**现象**：live 模式下上传失败或轮询超时。

**排查**：

1. 确认后端已启动：`./start.sh live`
2. 确认 ADB 反向转发：`adb reverse tcp:5001 tcp:5001`
3. 在 Pico 浏览器中访问 `http://localhost:5001` 测试连通性
4. 检查 logcat 中 `BackendApi` 的 HTTP 错误码

---

## 附录：相关链接

- [Pico 开发者平台](https://developer.pico-interactive.com/)
- [Pico Spatial SDK 官方文档](https://developer.pico-interactive.com/document/)
- [项目迁移计划](MIGRATION_PLAN.md)
- [GitHub Issues](https://github.com/GOR6710/yidianmofa/issues)

---

*最后更新：2026-06-12*
