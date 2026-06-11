// native_bridge.js — Android 原生桥接包装层
// 将浏览器独占 API 替换为 Android 原生实现（通过 LingbaoAndroid 接口）。
// 在桌面浏览器中运行时自动回退到原始 Web API。

(function () {
    const isAndroid = typeof LingbaoAndroid !== 'undefined';

    // ── 工具 ─────────────────────────────────────────────────────────
    function log(...args) {
        console.log('[NativeBridge]', ...args);
    }

    // 安全地执行 Android 桥接调用；调用失败时返回 null。
    function androidCall(name, ...args) {
        if (!isAndroid || !LingbaoAndroid[name]) return null;
        try {
            return LingbaoAndroid[name](...args);
        } catch (e) {
            console.error('[NativeBridge] Android call failed:', name, e);
            return null;
        }
    }

    // 标记桥接已就绪
    window.__nativeBridgeReady = true;
    log('isAndroid:', isAndroid);

    // ── 本地存储（替代 localStorage）──────────────────────────────────
    window.getStoredName = function () {
        if (isAndroid) return androidCall('getUserName') || '';
        return localStorage.getItem('lingbao_user_title') || '';
    };

    window.setStoredName = function (name) {
        if (isAndroid) {
            androidCall('setUserName', name);
        } else {
            localStorage.setItem('lingbao_user_title', name);
        }
    };

    // ── 唤醒词检测 ────────────────────────────────────────────────────
    window.startWakeWordListener = function () {
        if (isAndroid) {
            androidCall('startWakeWordListener');
            return;
        }
        const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;
        if (!Recognition) return;
        try {
            const r = new Recognition();
            r.lang = 'zh-CN';
            r.continuous = true;
            r.interimResults = true;
            r.onresult = function (e) {
                const t = Array.from(e.results).slice(e.resultIndex)
                    .map(result => result[0]?.transcript || '').join('');
                if (t.includes('灵宝') && window.onWakeWordDetected) window.onWakeWordDetected();
            };
            r.onend = function () {
                if (window.__wakeWordRunning) window.startWakeWordListener();
            };
            window.__wakeRecognizer = r;
            window.__wakeWordRunning = true;
            r.start();
        } catch (e) {
            console.warn('Wake word listener unavailable:', e);
        }
    };

    window.stopWakeWordListener = function () {
        window.__wakeWordRunning = false;
        if (isAndroid) {
            androidCall('stopWakeWordListener');
            return;
        }
        if (window.__wakeRecognizer) {
            try { window.__wakeRecognizer.stop(); } catch (_) { }
            window.__wakeRecognizer = null;
        }
    };

    window.__wakeWordRunning = false;

    // ── 录音 ──────────────────────────────────────────────────────────
    // Android 端由原生 MediaRecorder 处理，JS 端保留一个伪 recorder 状态对象，
    // 以便现有逻辑可以统一判断“是否正在录音”。
    window.startRecording = async function () {
        if (isAndroid) {
            androidCall('startRecording');
            window.mediaRecorder = { state: 'recording', android: true };
            window.recordedChunks = [];
            return;
        }
        // 浏览器回退
        if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === 'undefined') {
            window.mediaRecorder = null;
            return;
        }
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        window.recordedChunks = [];
        const recorder = new MediaRecorder(stream);
        recorder.addEventListener('dataavailable', e => {
            if (e.data?.size) window.recordedChunks.push(e.data);
        });
        recorder.addEventListener('stop', () => {
            stream.getTracks().forEach(t => t.stop());
        });
        recorder.start();
        window.mediaRecorder = recorder;
    };

    window.stopRecording = function () {
        if (isAndroid) {
            const path = androidCall('stopRecording') || '';
            window.__lastRecordingPath = path;
            window.mediaRecorder = null;
            return path;
        }
        if (window.mediaRecorder && window.mediaRecorder.state !== 'inactive') {
            window.mediaRecorder.stop();
        }
        return '';
    };

    // ── 后端上传与状态轮询 ────────────────────────────────────────────
    window.uploadAudio = async function (filePath) {
        if (isAndroid) {
            return androidCall('uploadAudio', filePath) || '';
        }
        // 浏览器回退：使用现有 MediaRecorder chunks 模拟
        if (!window.recordedChunks || window.recordedChunks.length === 0) {
            throw new Error('No recording data');
        }
        const audioBlob = new Blob(window.recordedChunks, { type: 'audio/webm' });
        const formData = new FormData();
        formData.append('audio', audioBlob, 'recording.webm');
        const resp = await fetch('/api/audio_transcribe', { method: 'POST', body: formData });
        if (!resp.ok) throw new Error('Upload failed: ' + resp.status);
        const data = await resp.json();
        if (data.error) throw new Error(data.error);
        return data.hash;
    };

    window.queryUploadStatus = async function (hash) {
        if (isAndroid) {
            const json = androidCall('queryStatus', hash);
            try {
                return JSON.parse(json || '{}');
            } catch (e) {
                return {};
            }
        }
        const resp = await fetch('/api/upload_status?hash=' + hash);
        return resp.json();
    };

    // ── TTS ───────────────────────────────────────────────────────────
    window.playTTS = function (text) {
        if (isAndroid) {
            androidCall('playTTS', text);
            return;
        }
        if (!('speechSynthesis' in window)) return;
        const u = new SpeechSynthesisUtterance(text);
        u.lang = 'zh-CN';
        window.speechSynthesis.cancel();
        window.speechSynthesis.speak(u);
    };

    window.playPreRecorded = function (name) {
        if (isAndroid) {
            androidCall('playPreRecorded', name);
            return;
        }
        const audio = new Audio(name);
        audio.volume = 0.92;
        audio.play().catch(() => { });
    };

    window.stopAudio = function () {
        if (isAndroid) {
            androidCall('stopAudio');
            return;
        }
        if ('speechSynthesis' in window) window.speechSynthesis.cancel();
    };

    // ── Android → JS 回调（由原生端调用）──────────────────────────────
    window.onWakeWordDetected = function () {
        log('Wake word detected');
        if (window.__originalOnWakeWord) window.__originalOnWakeWord();
    };

    window.onRecordingStarted = function () {
        log('Recording started');
    };

    window.onRecordingStopped = function (path) {
        log('Recording stopped:', path);
        window.__lastRecordingPath = path;
        // 如果存在原停止回调则触发
        if (window.__originalOnRecordingStopped) window.__originalOnRecordingStopped(path);
    };

    window.onRecordingError = function (message) {
        console.error('[NativeBridge] Recording error:', message);
    };

    window.onUploadSuccess = function (hash) {
        log('Upload success:', hash);
        if (window.__originalOnUploadSuccess) window.__originalOnUploadSuccess(hash);
    };

    window.onUploadError = function (message) {
        console.error('[NativeBridge] Upload error:', message);
        if (window.__originalOnUploadError) window.__originalOnUploadError(message);
    };

    window.onStatusResult = function (json) {
        log('Status result:', json);
        if (window.__originalOnStatusResult) window.__originalOnStatusResult(json);
    };

    window.onTtsCompleted = function () {
        log('TTS completed');
    };

    window.onAudioCompleted = function (name) {
        log('Audio completed:', name);
    };
})();
