# XR-Demo1.0 迁移至 Pico Spatial SDK 项目计划

> **策略：平面窗口 WebView 嵌入（方案 A）**
> **约束：保留现有 Spatial 项目架构不变**
> **目标设备：Pico 4 Ultra / Pico OS 3.x+，Spatial SDK 0.13.3**

---

## 1. 项目概述与目标

### 1.1 背景
- **源项目**：`XR-Demo1.0` —— "灵宝 AI" 2D Web 交互演示界面（HTML/CSS/JS）。
- **目标项目**：`yidianmofa` —— 现有 Pico Spatial SDK Android 项目（Kotlin + Compose）。

### 1.2 迁移目标
将完整的灵宝 AI 2D 交互界面嵌入现有 Pico Spatial 项目中，实现：
1. 用户佩戴 Pico 眼镜后，可在平面空间窗口内看到并交互 2D 内容。
2. **不修改**现有 Spatial 项目架构（Planar WindowContainer、SpatialAppScope、DefaultWindowContainer 保持不变）。
3. 遵循**渐进式路径**：先实现平面 2D 窗口，后续再逐步增强空间特性。

### 1.3 本次迁移范围外（未来阶段）
- 将 2D UI 转换为原生 3D 空间 UI 组件（第三阶段）。
- 用纯 Compose UI 完全替代 WebView（仅在后续性能不达标时考虑）。

---

## 2. 现有架构分析

### 2.1 Pico Spatial 项目（`yidianmofa`）

| 组件 | 详情 |
|-----------|--------|
| **SDK 版本** | Pico Spatial SDK 0.13.3 (`com.pico.spatial:bom:0.13.3`) |
| **开发语言** | Kotlin 2.0.0 |
| **UI 框架** | Pico Spatial UI（声明式 Compose-like DSL），**非**标准 Android Compose |
| **构建系统** | Gradle Kotlin DSL（`build.gradle.kts`、`libs.versions.toml`） |
| **minSdk / targetSdk** | 35 / 35 |
| **ABI** | 仅 `arm64-v8a` |
| **窗口容器** | 平面窗口（`style=1`），默认尺寸 880×600 dp，动态世界缩放（`worldscaletype=1`） |

**入口调用链**：
```
SpatialApplication.onCreate()
  └── launch(::mainApp)
        └── mainApp(scope: SpatialAppScope)
              └── DefaultWindowContainer { PicoTheme { HomePage() } }
```

**关键文件**：
- `app/src/main/java/com/example/yidianmofa/Main.kt` —— 应用入口，定义 `mainApp()`。
- `app/src/main/java/com/example/yidianmofa/content/HomePage.kt` —— 当前根 UI（2D 图片 + SpatialView 3D 模型）。
- `app/src/main/AndroidManifest.xml` —— Planar WindowContainer 配置。
- `app/src/main/java/com/example/yidianmofa/platform/SpatialApplication.kt` —— Application 类。
- `app/src/main/java/com/example/yidianmofa/platform/LaunchActivity.kt` —— `SpatialLaunchActivity`。

### 2.2 XR-Demo1.0 资源清单

| 资源 | 类型 | 用途 |
|-------|------|---------|
| `lingbao_preview.html` | HTML + CSS + JS | 主界面，包含 5 个场景：唤醒 → 介绍 → 名称 → 确认 → 白底设备 |
| `assets/lingbao-large.png` | PNG | 灵宝待机头像 |
| `assets/lingbao-recording.gif` | GIF | 灵宝录音中状态 |
| `assets/lingbao-working.gif` | GIF | 灵宝处理中状态 |
| `assets/phone.png` | PNG | 设备图标（手机） |
| `assets/window.png` | PNG | 设备图标（窗口） |
| `assets/computer.png` | PNG | 设备图标（电脑） |
| `voice_samples/*.mp3` | MP3 | 预录语音（介绍、名称、确认、唤醒） |
| `voice_config.example.js` | JS | 火山引擎 TTS API 配置示例 |
| `start.sh` | Shell | Flask 后端启动脚本 |
| `tree_mem/` | 子模块 | Python Flask 后端，用于会议对话分析（Whisper + pipeline） |

### 2.3 需在 Android 中替换的浏览器 API

| Web API | 在 XR-Demo1.0 中的用途 | Android 替代方案 |
|---------|---------------------|---------------------|
| `SpeechRecognition` | 唤醒词检测（"灵宝"）+ 是/否意图识别 | Android `SpeechRecognizer`（未来可接入 Pico Sense 语音 SDK） |
| `MediaRecorder` | 录制会议分析所需音频 | Android `MediaRecorder` / `AudioRecord` |
| `speechSynthesis` / 火山 TTS | 播放灵宝语音回复 | Android `TextToSpeech`（或继续使用 MP3 播放） |
| `fetch('/api/...')` | 上传音频、查询分析状态 | `OkHttp` + Retrofit（或原生 `HttpURLConnection`） |
| `localStorage` | 存储用户名称、配置 | Android `SharedPreferences` / DataStore |

---

## 3. 第一阶段：资源迁移 + WebView 集成

**目标**：通过 WebView 在现有 Pico 平面窗口中展示 `lingbao_preview.html`。
**预计工期**：2–3 天
**交付物**：启动应用后，在空间窗口内看到灵宝 UI，基础交互正常。

### 3.1 分步实施

#### 步骤 1.1 —— 将静态资源复制到 Android 项目

在 `app/src/main/assets/lingbao/` 下创建以下目录结构：
```
app/src/main/assets/lingbao/
├── index.html              （由 lingbao_preview.html 重命名）
├── assets/
│   ├── computer.png
│   ├── lingbao-large.png
│   ├── lingbao-recording.gif
│   ├── lingbao-working.gif
│   ├── phone.png
│   └── window.png
├── voice_samples/
│   ├── lingbao_confirm_leader.mp3
│   ├── lingbao_intro.mp3
│   ├── lingbao_intro_simple.mp3
│   ├── lingbao_name.mp3
│   └── lingbao_wake.mp3
└── config/
    └── voice_config.js     （占位文件，后续由 Android 注入配置）
```

**操作**：复制上述所有文件。
**代码变更**：无（纯文件复制）。

#### 步骤 1.2 —— 添加 WebView 依赖

在 `app/build.gradle.kts` 中确认 WebView 可用（WebView 是 Android 系统组件，但如需现代 WebView 特性可添加）：

```kotlin
dependencies {
    // ... 现有 Pico Spatial 依赖 ...
    implementation("androidx.webkit:webkit:1.11.0")
}
```

> WebView 本身在现代 Android 上**不需要**额外依赖，但 `androidx.webkit` 提供了 `WebSettingsCompat`（强制深色模式、安全浏览等）。

#### 步骤 1.3 —— 创建基于 WebView 的 Compose 节点

由于 Pico Spatial UI 使用自有的 Compose DSL（非标准 Android Compose），需要通过 `AndroidView` 或平台互操作机制嵌入 Android `WebView`。

**新建文件**：`app/src/main/java/com/example/yidianmofa/content/LingbaoWebView.kt`

```kotlin
package com.example.yidianmofa.content

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LingbaoWebView(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                // 启用 JavaScript
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.cacheMode = WebSettings.LOAD_DEFAULT

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean = false
                }

                webChromeClient = WebChromeClient()

                // 从 assets 加载本地 HTML
                loadUrl("file:///android_asset/lingbao/index.html")
            }
        }
    )
}
```

> **重要**：Pico Spatial UI 的 `foundation` 模块可能不直接暴露 `AndroidView`。若不可用，请使用自定义 `ViewNode` 包装器，或参考 Pico SDK 示例中嵌入 Android View 的方法。核心逻辑不变：实例化 `WebView`、配置、加载 `file:///android_asset/lingbao/index.html`。

#### 步骤 1.4 —— 替换 HomePage 内容

修改 `HomePage.kt`，将当前的 2D 图片 + 3D 模型布局替换为 WebView。

**方案 A（完全替换）** —— 灵宝 UI 占据整个窗口：

```kotlin
// HomePage.kt
@Composable
fun HomePage() {
    LingbaoWebView(modifier = Modifier.fillMaxSize())
}
```

**方案 B（并排展示，仅用于测试）** —— 保留原有演示：

```kotlin
@Composable
fun HomePage() {
    Column(modifier = Modifier.fillMaxSize()) {
        // 可选：上半部分保留原有演示
        // ...
        // 下半部分：灵宝 UI
        LingbaoWebView(modifier = Modifier.weight(1f))
    }
}
```

**建议**：迁移使用**方案 A**；原 HomePage 可从 git 历史恢复。

#### 步骤 1.5 —— AndroidManifest.xml 权限配置

添加麦克风和网络权限（第二阶段才需要，但现阶段添加无风险）：

```xml
<manifest ...>
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />

    <application ...>
        <!-- 现有内容 -->
    </application>
</manifest>
```

> 在 Pico OS 上，`RECORD_AUDIO` 可能需要运行时权限申请（取决于系统版本和签名）；如系统未自动授权，需补充运行时请求流程。

### 3.2 第一阶段验收清单

- [ ] 应用在 Pico 设备/模拟器上编译并启动。
- [ ] 平面窗口（880×600 dp）正常出现，灵宝 UI 渲染成功。
- [ ] 所有图片正常加载（灵宝头像、设备图标）。
- [ ] GIF 动画正常播放（录音中、处理中状态）。
- [ ] 预录 MP3 音频在 JS 触发时正常播放。
- [ ] 鼠标/视线/手柄交互正常（悬停、点击）。

---

## 4. 第二阶段：语音与音频原生桥接

**目标**：将浏览器独占的语音/音频 API 替换为 Android 原生实现，通过桥接注入 WebView。
**预计工期**：5–7 天
**交付物**：在空间窗口内实现完整的语音唤醒、录音、TTS 播放、后端通信。

### 4.1 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                    Pico 平面窗口                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                  WebView（灵宝 UI）                      │  │
│  │  ┌─────────┐    ┌─────────┐    ┌─────────────────┐   │  │
│  │  │  唤醒   │ -> │  介绍   │ -> │  白底（设备入口）│   │  │
│  │  │  场景   │    │  场景   │    │     场景        │   │  │
│  │  └─────────┘    └─────────┘    └─────────────────┘   │  │
│  │        ↑ JS 调用                                       │  │
│  │        "startRecording()"                             │  │
│  │        "playTTS(text)"                                │  │
│  │        "checkWakeWord()"                              │  │
│  └────────┬──────────────────────────────────────────────┘  │
│           │ JS 接口 (@JavascriptInterface)                  │
│  ┌────────▼──────────────────────────────────────────────┐  │
│  │         Android 原生桥接层（LingbaoBridge.kt）          │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐  │  │
│  │  │SpeechRecognizer│  │MediaRecorder │  │  TextToSpeech│  │  │
│  │  │  （唤醒词）    │  │ （音频录制） │  │   （TTS）    │  │  │
│  │  └──────────────┘  └──────┬───────┘  └─────────────┘  │  │
│  │                           │                          │  │
│  │                      ┌────▼────┐                     │  │
│  │                      │  OkHttp  │  <--HTTP--> Flask  │  │
│  │                      │ (上传)   │      (tree_mem)     │  │
│  │                      └─────────┘                     │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 分步实施

#### 步骤 2.1 —— JavaScript 桥接类

**新建文件**：`app/src/main/java/com/example/yidianmofa/bridge/LingbaoBridge.kt`

```kotlin
package com.example.yidianmofa.bridge

import android.content.Context
import android.webkit.JavascriptInterface
import android.widget.Toast

class LingbaoBridge(private val context: Context) {

    @JavascriptInterface
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun startWakeWordListener(): String {
        // TODO：启动 Android SpeechRecognizer 持续监听
        // 立即返回；结果通过 JS 注入推送
        return "started"
    }

    @JavascriptInterface
    fun stopWakeWordListener(): String {
        // TODO：停止监听
        return "stopped"
    }

    @JavascriptInterface
    fun startRecording(): String {
        // TODO：启动 MediaRecorder，保存到 /cache/recording.wav
        return "recording"
    }

    @JavascriptInterface
    fun stopRecording(): String {
        // TODO：停止 MediaRecorder，返回文件路径
        return "/cache/recording.wav"
    }

    @JavascriptInterface
    fun uploadAudio(filePath: String): String {
        // TODO：通过 OkHttp 上传至 Flask 后端
        // 返回 hash / 任务 ID
        return "job_123"
    }

    @JavascriptInterface
    fun queryStatus(hash: String): String {
        // TODO：轮询 /api/upload_status?hash=
        // 返回 JSON 字符串
        return "{}"
    }

    @JavascriptInterface
    fun playTTS(text: String): String {
        // TODO：使用 Android TTS 引擎（或火山 TTS HTTP 接口）
        return "playing"
    }

    @JavascriptInterface
    fun playPreRecorded(name: String): String {
        // TODO：播放 assets/voice_samples/ 下的 MP3
        return "playing"
    }

    @JavascriptInterface
    fun getUserName(): String {
        // TODO：从 SharedPreferences 读取
        return ""
    }

    @JavascriptInterface
    fun setUserName(name: String) {
        // TODO：保存到 SharedPreferences
    }
}
```

#### 步骤 2.2 —— 将桥接类绑定到 WebView

更新 `LingbaoWebView.kt`：

```kotlin
// 在 factory 代码块内
val bridge = LingbaoBridge(context)
addJavascriptInterface(bridge, "LingbaoAndroid")
```

#### 步骤 2.3 —— 适配 `lingbao_preview.html` JavaScript

新建桥接包装 JS 文件 `app/src/main/assets/lingbao/native_bridge.js`：

```javascript
// native_bridge.js —— 注入 WebView，将浏览器独占 API 替换为 Android 桥接调用

(function() {
    const isAndroid = typeof LingbaoAndroid !== 'undefined';

    // ─── 唤醒词检测 ───
    window.startWakeWordListener = function() {
        if (isAndroid) {
            LingbaoAndroid.startWakeWordListener();
        } else {
            // 桌面调试回退：浏览器 SpeechRecognition
            const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;
            if (Recognition) {
                const r = new Recognition();
                r.lang = "zh-CN";
                r.continuous = true;
                r.onresult = (e) => {
                    const t = e.results[e.resultIndex][0].transcript.trim();
                    if (t.includes("灵宝")) window.onWakeWordDetected && window.onWakeWordDetected();
                };
                r.start();
            }
        }
    };

    window.stopWakeWordListener = function() {
        if (isAndroid) LingbaoAndroid.stopWakeWordListener();
    };

    // ─── 音频录制 ───
    window.startRecording = function() {
        if (isAndroid) LingbaoAndroid.startRecording();
        // 回退：浏览器 MediaRecorder ...
    };

    window.stopRecording = function() {
        if (isAndroid) {
            const path = LingbaoAndroid.stopRecording();
            return path;
        }
        // 回退 ...
    };

    // ─── 上传 ───
    window.uploadAudio = async function(filePath) {
        if (isAndroid) {
            return LingbaoAndroid.uploadAudio(filePath);
        }
        // 桌面调试回退：fetch()
    };

    window.queryUploadStatus = async function(hash) {
        if (isAndroid) {
            const json = LingbaoAndroid.queryStatus(hash);
            return JSON.parse(json);
        }
        // 回退
    };

    // ─── TTS ───
    window.playTTS = function(text) {
        if (isAndroid) {
            LingbaoAndroid.playTTS(text);
        } else {
            const u = new SpeechSynthesisUtterance(text);
            u.lang = 'zh-CN';
            window.speechSynthesis.speak(u);
        }
    };

    window.playPreRecorded = function(name) {
        if (isAndroid) {
            LingbaoAndroid.playPreRecorded(name);
        } else {
            // 回退：<audio> 标签
        }
    };

    // ─── 存储 ───
    window.getStoredName = function() {
        if (isAndroid) return LingbaoAndroid.getUserName();
        return localStorage.getItem('user_name') || '';
    };

    window.setStoredName = function(name) {
        if (isAndroid) LingbaoAndroid.setUserName(name);
        else localStorage.setItem('user_name', name);
    };
})();
```

**HTML 修改**：在 `index.html` 中，于主脚本之前加载 `native_bridge.js`，或将其内联。

#### 步骤 2.4 —— 实现 `SpeechRecognizer` 唤醒词检测

**新建文件**：`app/src/main/java/com/example/yidianmofa/voice/WakeWordDetector.kt`

使用 Android `SpeechRecognizer`，通过 `EXTRA_PARTIAL_RESULTS` 持续检测"灵宝"。

关键注意点：
- 在 Pico OS 上，需确认 `SpeechRecognizer` 是否可用（取决于是否内置 Google 服务 / Pico 系统组件）。
- 若不可用，实现轻量级关键词检测：使用 `AudioRecord` + 端侧微模型（如 Pico Sense SDK 语音模块，或开源方案 Porcupine / Snowboy，需注意授权）。
- 第二阶段 MVP 阶段，使用 `SpeechRecognizer` 循环重启策略；准确率可能受限，但可快速验证。

#### 步骤 2.5 —— 实现 `MediaRecorder` + OkHttp 上传

**新建文件**：`app/src/main/java/com/example/yidianmofa/audio/AudioRecorder.kt`

```kotlin
// 简化结构
class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun start(): File {
        outputFile = File(context.cacheDir, "recording_${System.currentTimeMillis()}.wav")
        recorder = MediaRecorder(context).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile!!.absolutePath)
            prepare()
            start()
        }
        return outputFile!!
    }

    fun stop(): File? {
        recorder?.apply { stop(); release() }
        recorder = null
        return outputFile
    }
}
```

**新建文件**：`app/src/main/java/com/example/yidianmofa/network/BackendApi.kt`

使用 Retrofit 或 OkHttp 实现：
- `POST /api/audio_transcribe` —— multipart 上传音频文件。
- `GET /api/upload_status?hash={hash}` —— 轮询直到处理完成。

**Base URL**：Flask 后端运行在本地设备或局域网服务器上，地址配置在 `BuildConfig` 或 `SharedPreferences` 中。

#### 步骤 2.6 —— 语音合成（TTS）

**方案 A**：Android 内置 `TextToSpeech`（最简单，无需网络）。

```kotlin
val tts = TextToSpeech(context) { status ->
    if (status == TextToSpeech.SUCCESS) {
        tts.language = Locale.CHINESE
    }
}
tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
```

**方案 B**：火山引擎 TTS HTTP 接口（与原项目完全一致，需要 API Key）。

实现 OkHttp 调用火山 TTS API，将返回的 MP3 保存到缓存，通过 `MediaPlayer` 播放。

**决策**：第二阶段先用**方案 A**；方案 B 作为可配置回退，在第二阶段后期加入。

### 4.3 第二阶段验收清单

- [ ] 唤醒词"灵宝"触发 UI 跳转（唤醒场景 → 介绍场景）。
- [ ] 通过 WebView 按钮点击可正常开始/停止录音。
- [ ] 音频文件成功上传后端并返回 hash。
- [ ] 轮询 `/api/upload_status` 正常；最终收到结果 JSON。
- [ ] TTS 在 Pico 设备上正常朗读中文。
- [ ] 预录 MP3 正常播放。
- [ ] 用户名称在应用重启后保持记忆。
- [ ] `cache` 和 `live` 两种模式全部正常工作。

---

## 5. 第三阶段：空间特性增强（未来规划）

**目标**：利用 Pico Spatial SDK 能力，将 2D 灵宝体验升级为真正的空间应用。
**预计工期**：1–2 周（取决于创意方向）
**交付物**：超越平面窗口的空间应用。

### 5.1 潜在增强方向

| 增强项 | 描述 | 复杂度 |
|-------------|-------------|--------|
| **3D 灵宝头像** | 将 2D GIF 头像替换为 3D 模型（VRM / USDZ），在 UI 面板旁通过 `SpatialView` 展示。 | 高 |
| **多窗口布局** | 使用 `Volumetric` WindowContainer 或生成二级空间面板，用于展示"会议总结"结果。 | 中 |
| **手势追踪交互** | 用手势替代鼠标悬停/点击（捏合点击、空中点击）。 | 中 |
| **空间音频** | 将 TTS 语音定位到 3D 空间（例如：灵宝的声音从头像位置传来）。 | 低 |
| **透视背景** | 使用 Pico 透视 API，让 UI 悬浮在真实环境中，而非黑色虚空。 | 低 |
| **近场触发** | 当用户看向/靠近窗口时，灵宝自动唤醒。 | 中 |

### 5.2 第三阶段推荐首步

1. **将 WindowContainer 切换为 Volumetric**（`style=2`），赋予窗口物理深度感。
2. **添加二级空间面板**，用于展示会议总结结果，放置在主窗口右侧。
3. **启用透视背景**，提升沉浸感。

---

## 6. 文件映射与资源迁移清单

### 6.1 需新建的文件

| 文件 | 用途 |
|------|---------|
| `app/src/main/assets/lingbao/index.html` | 由 `lingbao_preview.html` 迁移（含桥接 JS） |
| `app/src/main/assets/lingbao/native_bridge.js` | 注入 WebView 的 Android 原生 API 包装层 |
| `app/src/main/java/com/example/yidianmofa/content/LingbaoWebView.kt` | WebView Compose 包装器 |
| `app/src/main/java/com/example/yidianmofa/bridge/LingbaoBridge.kt` | `@JavascriptInterface` 桥接类 |
| `app/src/main/java/com/example/yidianmofa/voice/WakeWordDetector.kt` | Android SpeechRecognizer 包装器 |
| `app/src/main/java/com/example/yidianmofa/audio/AudioRecorder.kt` | MediaRecorder 包装器 |
| `app/src/main/java/com/example/yidianmofa/audio/AudioPlayer.kt` | MediaPlayer，用于播放 MP3 / TTS 结果 |
| `app/src/main/java/com/example/yidianmofa/network/BackendApi.kt` | OkHttp / Retrofit API 客户端 |
| `app/src/main/java/com/example/yidianmofa/tts/TtsEngine.kt` | Android TTS + 火山 TTS 回退 |
| `app/src/main/java/com/example/yidianmofa/storage/LingbaoPrefs.kt` | SharedPreferences 包装器 |

### 6.2 需修改的文件

| 文件 | 变更内容 |
|------|--------|
| `app/src/main/java/com/example/yidianmofa/content/HomePage.kt` | 内容替换为 `LingbaoWebView()` |
| `app/src/main/AndroidManifest.xml` | 添加 `INTERNET`、`RECORD_AUDIO` 权限 |
| `app/build.gradle.kts` | 添加 `androidx.webkit:webkit` 及网络库依赖（OkHttp、Retrofit 如需） |
| `gradle/libs.versions.toml` | 新增依赖的版本号 |

### 6.3 直接复制无需修改的文件

所有图片、GIF、MP3 资源从 `XR-Demo1.0/` → `app/src/main/assets/lingbao/`。

---

## 7. 风险点与缓解方案

| 风险 | 影响 | 可能性 | 缓解方案 |
|------|--------|------------|--------|
| **Pico Spatial UI 未暴露 `AndroidView`** | 无法嵌入 WebView | 低 | 使用自定义 `ViewNode` 互操作；Pico SDK 0.13.3 的平面窗口支持嵌入 Android View。 |
| **空间窗口内 WebView 性能不佳** | UI 卡顿、延迟高 | 中 | 启用硬件加速；减少 CSS 动画；使用 Pico GPU 工具分析性能。 |
| **Android `SpeechRecognizer` 在 Pico 上不可用** | 唤醒词失效 | 中 | 回退为按钮触发录音；如 Pico Sense 语音 SDK 可用则接入；使用端侧关键词检测库（Porcupine）。 |
| **Flask 后端无法被 Android 访问** | 实时模式不可用 | 中 | 后端与设备处于同一局域网；开发期使用 `adb reverse tcp:5001 tcp:5001`；长期可考虑将后端部署到局域网服务器或云端。 |
| **录音权限被拒绝** | 无法录音 | 低 | 添加运行时权限申请；在 UI 中引导用户授权。 |
| **WebView 音频自动播放被阻止** | MP3 / TTS 静音 | 中 | 设置 `mediaPlaybackRequiresUserGesture = false`；确保首次交互发生在音频播放之前。 |
| **标准 Compose 与 Pico Compose 冲突** | 导入错误 | 低 | **不要**添加 `androidx.compose.ui` 依赖；仅使用 Pico Spatial UI DSL + `AndroidView`。 |

---

## 8. 后端部署策略

原 `tree_mem` 后端（Flask + Whisper + pipeline）需要 Python 环境，且 Whisper 建议 GPU 运行。

### 8.1 开发期
- 在开发 PC 上运行 Flask（`./start.sh live`）。
- 使用 `adb reverse tcp:5001 tcp:5001`，使 Android 的 `localhost:5001` 指向 PC 后端。

### 8.2 生产选项

| 选项 | 描述 | 工作量 |
|--------|-------------|--------|
| **A. 局域网服务器** | 将 Flask 部署到局域网服务器（PC / 树莓派 / 云服务器）。Android 通过局域网 IP 连接。 | 低 |
| **B. 云端 API** | 将 Whisper 替换为云端 API（OpenAI Whisper API、阿里云 ASR 等）。完全取消本地后端。 | 中 |
| **C. 端侧 ML** | 使用 Android 端侧 ASR（Google ML Kit Speech，或 Pico 端侧 ASR 如有）。 | 高 |

**建议**：内部演示先用**选项 A**；如需规模化再评估**选项 B**。

---

## 9. 迭代路线图

| 阶段 | 工期 | 关键交付物 |
|-------|----------|-----------------|
| **第零阶段**（准备） | 0.5 天 | 资源盘点、环境准备、创建独立分支 |
| **第一阶段 1.1** | 1 天 | 复制资源、添加 WebView 依赖、创建 `LingbaoWebView.kt` |
| **第一阶段 1.2** | 1 天 | 适配 HTML、绑定桥接、验证静态渲染 |
| **第一阶段 1.3** | 0.5 天 | AndroidManifest 权限、Pico 实机测试 |
| **第二阶段 2.1** | 1 天 | 实现 `LingbaoBridge.kt` 骨架 + JS 包装层 |
| **第二阶段 2.2** | 1.5 天 | `WakeWordDetector` + `AudioRecorder` 集成 |
| **第二阶段 2.3** | 1.5 天 | OkHttp 上传 + 状态轮询 |
| **第二阶段 2.4** | 1 天 | TTS 引擎（Android + 火山回退） |
| **第二阶段 2.5** | 1 天 | 端到端测试、打磨、边界情况处理 |
| **第三阶段**（未来） | 1–2 周 | 3D 头像、空间音频、手势追踪、体积窗口 |

---

## 10. 附录：快速参考

### 10.1 Pico Spatial SDK 关键 API

```kotlin
// 窗口容器入口
DefaultWindowContainer { PicoTheme { /* 内容 */ } }

// 嵌入 Android View（如支持互操作）
AndroidView(factory = { context -> WebView(context) })

// 3D 模型加载
AssetBundle.load("asset://editor-asset.bundle")
Entity.loadSuspend(modelName = "MyScene", bundle = bundle)
```

### 10.2 WebView 资源 URL

```
file:///android_asset/lingbao/index.html
```

### 10.3 JS 桥接调用模式

```javascript
// JS 侧
if (typeof LingbaoAndroid !== 'undefined') {
    LingbaoAndroid.startRecording();
}
```

```kotlin
// Android 侧
@JavascriptInterface
fun startRecording(): String { /* ... */ }
```

### 10.4 ADB 反向端口转发（本地后端调试）

```bash
adb reverse tcp:5001 tcp:5001
```

---

*文档版本：1.0*
*创建日期：2026-06-11*
*下次审阅：第二阶段启动前*
