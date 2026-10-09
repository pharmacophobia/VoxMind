# 🎙️ VoxMind — Voice AI Reminders, Tasks & DeepSeek Transcription for Android

> **Turn messy voice streams into structured thoughts, actionable checklists, exact alarms, and SMS/email reminders powered by DeepSeek AI.**

VoxMind is an open-source, private Android productivity powerhub designed for hands-free thinking, rapid speech-to-text dictation, and cognitive AI organization. Speak freely without worrying about structure: VoxMind records live audio with real-time waveform visualization, continuous speech recognition, and sends transcripts to DeepSeek (`deepseek-chat` or `deepseek-reasoner`) to instantly organize chaotic thoughts, generate bullet-point checklists, summarize takeaways, and detect scheduling commitments.

---

## 📥 Direct APK Download & Install

| Direct 1-Tap Download | Scan QR Code with Phone Camera to Install |
| :---: | :---: |
| [![Download APK](https://img.shields.io/badge/Download-VoxMind.apk-6366F1?style=for-the-badge&logo=android&logoColor=white)](https://github.com/pharmacophobia/VoxMind/raw/main/VoxMind.apk)<br><br>👉 **[Click here to download VoxMind.apk (11.0 MB)](https://github.com/pharmacophobia/VoxMind/raw/main/VoxMind.apk)**<br><br>📦 Alternate: [Official GitHub Release v1.2.0](https://github.com/pharmacophobia/VoxMind/releases/tag/v1.2.0) | <img src="https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=https://github.com/pharmacophobia/VoxMind/raw/main/VoxMind.apk" width="180" height="180" alt="Scan to install VoxMind APK" /><br>*(Point your phone camera to download directly)* |

---

## ✨ Key Features

### ⚡ Autonomous DeepSeek Text & Email Reminders Engine
- **Automatic Multi-Channel Scheduling**: DeepSeek analyzes voice dictations and written thoughts in real-time or over time, detects dates, relative times (*"in 30 minutes"*, *"tomorrow at 9 AM"*, *"Friday 5 PM"*), and schedules exact Android alarms automatically.
- **Intelligent Text Message (SMS) Dispatch**: Detects texting intentions and contacts (e.g. *"text mom at 555-1234"* or *"send text reminder"*). Automatically routes and dispatches background SMS when the alarm triggers.
- **Automatic Email Integration**: Detects email instructions and addresses (e.g. *"email team@work.com about the report"*). Wires a high-priority 1-tap **"✉️ Send Email"** action on the notification shade and pre-fills email drafts ready to send.
- **Natural Language Quick-Scheduler**: Type or dictate natural language reminder instructions directly in the Reminders tab (e.g. *"Text reminder to 555-0199 tomorrow at 3 PM to pick up groceries"*) for 1-tap instant AI arming.
- **Overtime Automatic Background Scheduling**: Every voice note saved can automatically detect commitments and arm alarms without manual entry.

### 🤖 Autonomous DeepSeek "Overtime" Auto-Organizer
- **Continuous Categorization**: Over time, as you dictate or write thoughts, DeepSeek analyzes all raw writings and automatically routes data into dedicated, categorized task lists.
- **Context-Aware List Matching**: Intelligently appends items to existing checklists (e.g., Grocery, Work Tasks, Ideas) without creating duplicates, or auto-spawns vibrant new category lists when fresh topics emerge.
- **Zero-Friction Background Execution**: Triggers automatically whenever you save a transcription or on-demand via the "✨ Auto-Sort All Writings" button in the Lists tab.
- **Full Traceability**: Every note tracks its sorting status and lists to which items were filed.

### 🎙️ Clean, Silent Speech Transcription & Noise Elimination
- **Zero Microphone Beeps & Earcons**: System sound streams (`STREAM_SYSTEM`, `STREAM_NOTIFICATION`, `STREAM_MUSIC`) are muted during continuous listening loops with a 500ms trailing buffer on stop, completely eliminating irritating speech recognition chimes.
- **Ambient Noise Gate**: Filters out mic hiss, room fan noise, and desk rumble by rejecting audio fluctuations below an RMS threshold (0.12).
- **Extended Silence Tolerance**: Generous 3.5s–4.0s pause detection ensures you won't get cut off mid-thought.
- **Live Real-Time Streaming Speech Recognition**: Words appear on screen in real time as you speak.
- **Dynamic Waveform Visualizer**: Glowing cyber-wave responds to microphone decibels (RMS) in real time with a clean flat baseline during silence.
- **Editable & Shareable**: Edit transcribed text, copy to clipboard in 1 tap, or save to persistent local notes.

### 🧠 DeepSeek AI Cognitive Superpowers
- **🧠 Organize Thoughts**: Transforms disorganized thoughts into structured themes, insights, context, and clear conclusions.
- **📋 Bullet Point Lists**: Extracts crisp, actionable bullet points from voice notes.
- **📝 Executive Summaries**: Generates high-impact TL;DR summaries with key takeaways.
- **⏰ Automatic Schedule Extraction**: Detects dates, times, deadlines, and tasks mentioned in speech and creates 1-click reminders.
- **💬 Freeflow DeepSeek Chat**: Interactive bottom drawer to ask follow-up questions, refine ideas, or draft emails from your transcripts.
- **Configurable Models**: Switch effortlessly between `deepseek-chat` (fast) and `deepseek-reasoner` (deep reasoning).

### ⏰ Exact Reminders, Alarms & Timers
- **Scheduled Reminders**: Exact alarms via `AlarmManager.setExactAndAllowWhileIdle()` with high-priority notifications, custom vibration, snooze, and completion actions.
- **Clock Alarms with Attached Tasks**: Set clock alarms with attached reminder notes displayed directly on screen when ringing.
- **Interactive Timers**: Multi-duration countdown timers with progress bars, attached task notes, and completion chimes.
- **Persistent Rescheduling**: Survives device reboots via `BootReceiver`.

### ✉️ Text Message (SMS) & Email Reminders
- **Background SMS Dispatch**: Automatically sends a text message reminder to a specified phone number when a reminder or timer triggers.
- **Email Reminder Integrations**: Prefilled `mailto:` dispatch for sending tasks to personal or work inboxes.

### 📋 Interactive Task Lists & Checklists
- **1-Tap AI Checklist Generation**: Convert DeepSeek bullet points directly into an interactive checklist with checkboxes and progress bars.
- **Custom Categories**: Organize lists (Groceries, Sprint Goals, Ideas, Errands) with completion counters and strike-throughs.

---

## 🛠️ Architecture & Tech Stack

- **Platform**: Android 8.0+ (API 26 to API 34)
- **Language**: 100% Kotlin
- **UI Toolkit**: Jetpack Compose with Material 3 (Dark Cyberpunk Theme)
- **Speech**: Android Native `SpeechRecognizer` + Intent Fallback
- **Alarms**: Android `AlarmManager` with `setAlarmClock()` and `setExactAndAllowWhileIdle()`
- **Persistence**: Thread-safe atomic JSON file storage with Mutex and StateFlow
- **AI Backend**: OkHttp + Gson connecting directly to DeepSeek API (`https://api.deepseek.com/v1/chat/completions`)

---

## 🚀 Building from Source

```bash
# Clone the repository
git clone https://github.com/pharmacophobia/VoxMind.git
cd VoxMind

# Build Debug APK
./gradlew assembleDebug

# Build Release APK
./gradlew assembleRelease
```
The compiled APK will be located at `app/build/outputs/apk/release/app-release.apk` (or directly in the root directory as `VoxMind.apk`).

---

## 📜 Permissions Used
- `RECORD_AUDIO`: Voice transcription and audio visualizer.
- `POST_NOTIFICATIONS`: High-priority reminder, alarm, and timer notifications.
- `SCHEDULE_EXACT_ALARM`: Precise alarm and reminder triggers.
- `SEND_SMS`: Optional background SMS dispatch for reminders.
- `RECEIVE_BOOT_COMPLETED`: Reschedule active alarms after device reboot.
- `INTERNET`: Secure connection to DeepSeek API.

---

## 📄 License
MIT License. Free and open source.
