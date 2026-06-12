# 亿点魔法 / yidianmofa

Pico Spatial SDK 上的「灵宝 AI」空间交互演示应用。

本项目将原有的 `XR-Demo1.0`（灵宝 AI 2D Web 交互界面）迁移到现有 Pico Spatial SDK Android 项目中，通过 WebView 嵌入平面空间窗口，并使用 Android 原生能力替换浏览器独占的语音、录音、TTS 与后端通信 API。

---

## 项目目标

- 在 Pico 4 Ultra / Pico OS 3.x+ 设备上以空间窗口形式展示灵宝 AI 界面
- 保留现有 Spatial 项目架构（Planar WindowContainer、SpatialAppScope、DefaultWindowContainer 不变）
- 通过 WebView + Android 原生桥接实现：
  - 语音唤醒（「灵宝」）
  - 录音与音频上传
  - 中文 TTS 播放
  - 后端会议分析结果轮询
  - 用户称呼持久化

---

## 当前进度

| 阶段 | 状态 | 说明 |
|------|------|------|
| Phase 0 准备 | ✅ | 资源盘点、环境准备 |
| Phase 1 资源迁移 + WebView 集成 | ✅ | 静态资源已迁移，WebView 已集成到 HomePage |
| Phase 2 语音与音频原生桥接 | ✅ | 桥接类、录音、TTS、后端 API、SharedPreferences 已实现 |
| Phase 3 空间特性增强 | 🔄 | 未来规划（3D 头像、多窗口、手势、空间音频等） |

详见 [MIGRATION_PLAN.md](MIGRATION_PLAN.md)。

---

## 技术栈

- **SDK**：Pico Spatial SDK 0.13.3
- **开发语言**：Kotlin 2.0.0
- **UI 框架**：Pico Spatial UI（声明式 Compose-like DSL）
- **构建系统**：Gradle Kotlin DSL
- **minSdk / targetSdk**：35 / 35
- **ABI**：`arm64-v8a`
- **网络**：OkHttp 4.12.0
- **Web 嵌入**：Android WebView + `androidx.webkit:webkit:1.11.0`

---

## 项目结构

```
yidianmofa/
├── app/src/main/
│   ├── assets/lingbao/          # 灵宝 UI 静态资源
│   │   ├── index.html           # 主页面
│   │   ├── native_bridge.js     # Android 原生 API 包装层
│   │   ├── assets/              # 图片、GIF
│   │   └── voice_samples/       # 预录语音
│   ├── java/com/example/yidianmofa/
│   │   ├── Main.kt              # Spatial 应用入口
│   │   ├── content/
│   │   │   ├── HomePage.kt      # 根 UI
│   │   │   └── LingbaoWebView.kt # WebView 包装器
│   │   ├── platform/            # Application / LaunchActivity
│   │   ├── bridge/LingbaoBridge.kt
│   │   ├── voice/WakeWordDetector.kt
│   │   ├── audio/AudioRecorder.kt / AudioPlayer.kt
│   │   ├── tts/TtsEngine.kt
│   │   ├── network/BackendApi.kt
│   │   └── storage/LingbaoPrefs.kt
│   └── AndroidManifest.xml      # Planar WindowContainer 配置
├── editor-asset/                # Spatial 3D 资产模块
├── gradle/libs.versions.toml    # 依赖版本管理
└── MIGRATION_PLAN.md            # 迁移计划
```

---

## 快速开始

> 详细的 Android Studio、Pico Spatial SDK、模拟器/真机配置步骤请参考 [docs/SETUP_AND_TEST.md](docs/SETUP_AND_TEST.md)。

1. 克隆仓库：

   ```bash
   git clone https://github.com/GOR6710/yidianmofa.git
   cd yidianmofa
   ```

2. 安装 Pico Spatial SDK 编辑器工具并配置 `local.properties`：

   ```properties
   spatial.tools.dir=D:\\PICO\\0.13\\editor
   sdk.dir=D:\\Android\\Sdk
   ```

3. 在 Android Studio 中打开项目，同步 Gradle。

4. 连接 Pico 设备或启动 Pico XR 模拟器。

5. 点击 **Run**（`Shift + F10`）部署应用。

6. （可选）启动后端服务：

   ```bash
   cd XR-Demo1.0
   ./start.sh live
   adb reverse tcp:5001 tcp:5001
   ```

---

## 主要功能

- **空间窗口展示**：在 Pico 平面窗口中展示灵宝 AI 2D 界面
- **语音唤醒**：通过 Android `SpeechRecognizer` 监听「灵宝」
- **录音上传**：使用 `MediaRecorder` 录制音频并通过 OkHttp 上传
- **中文 TTS**：使用 Android 系统 TTS 引擎朗读回复
- **预录语音播放**：播放 `assets/lingbao/voice_samples/` 中的 MP3
- **后端通信**：轮询 Flask 后端 `/api/upload_status` 获取会议分析结果
- **用户称呼记忆**：通过 `SharedPreferences` 持久化存储

---

## 已知问题与后续工作

详见 GitHub Issues：

- [Phase 1] 在 Pico 设备/模拟器上验证 WebView 静态渲染
- [Phase 2] 补充 Android 运行时录音权限申请
- [Phase 2] 验证并完善 Pico 上的唤醒词检测
- [Phase 2] 后端服务地址配置与调试流程
- [Phase 3] 空间特性增强（未来规划）

---

## 许可证

本项目内部演示使用，具体许可证待定。

---

*最后更新：2026-06-12*
