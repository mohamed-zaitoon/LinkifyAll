# LinkifyAll 🔗

![Android](https://img.shields.io/badge/Android-36-green.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3.0-purple.svg)
![Xposed](https://img.shields.io/badge/Module-Xposed-orange.svg)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

**LinkifyAll** is an Xposed/LSPosed module that forces non-clickable text URLs to become clickable links system-wide. It hooks into Android's `TextView` to detect and interact with web links in almost any application.

## ✨ Features

- **System-Wide Detection:** Works in almost any app that uses standard Android `TextView`.
- **Smart Parsing:** Uses Regex to detect URLs (http, https, www, and common domains).
- **Interactive:** Handles touch events to open links directly in your default browser.
- **Auto-Updates:** Built-in check for updates directly from GitHub Releases.
- **Lightweight:** Optimized hook logic to ensure minimal impact on system performance.
- **Safety Checks:** Excludes critical system packages (`android`, `systemui`) to prevent crashes.

## 🛠 Requirements

- **Rooted Android Device** (Android 8.0+ recommended).
- **Xposed Framework** (LSPosed is highly recommended for newer Android versions).

## 📥 Installation

1. Download the latest APK from the [Releases Page](https://github.com/mohamed-zaitoon/LinkifyAll/releases).
2. Install the APK on your device.
3. Open **LSPosed / Xposed Manager**.
4. Enable the **LinkifyAll** module.
5. **Reboot** your device.
6. Open the app to verify the status and check for updates.

## 🔧 How It Works

The module hooks two main methods in `android.widget.TextView`:

1.  **`setText`**: Scans the text for URL patterns. If found, it applies a `ForegroundColorSpan` (Cyan) to highlight the link.
2.  **`onTouchEvent`**: Intercepts touch gestures. If a user taps on the highlighted area, it resolves the URL and launches an `ACTION_VIEW` Intent.
