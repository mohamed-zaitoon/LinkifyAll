# 🔗 LinkifyAll

### Turn Text into Action. System-Wide.

[![Version](https://img.shields.io/badge/Version-0.5.0--alpha02-2ea44f?style=for-the-badge&logo=github)](https://github.com/mohamed-zaitoon/LinkifyAll/releases)
[![Android](https://img.shields.io/badge/Android-11.0%2B%20(API%2030%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://www.android.com/)
[![Target](https://img.shields.io/badge/Target-Android%2017%20(API%2037)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://www.android.com/)
[![Root](https://img.shields.io/badge/Root-Required-red?style=for-the-badge)](https://github.com/topjohnwu/Magisk)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20--Beta1-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Xposed](https://img.shields.io/badge/Xposed-Module-orange?style=for-the-badge&logo=xposed&logoColor=white)](https://repo.xposed.info/)

**LinkifyAll** is a powerful Xposed/LSPosed module that forces non-clickable text URLs to become clickable links system-wide.
This release targets Android 17 while keeping the module hidden from the launcher and accessible from LSPosed/Xposed module settings.

[📥 Download Latest APK](https://github.com/mohamed-zaitoon/LinkifyAll/releases) • [🐛 Report Bug](https://github.com/mohamed-zaitoon/LinkifyAll/issues)

---

## ✨ Why LinkifyAll?

| Feature | Description |
| :--- | :--- |
| 🌍 **System-Wide** | Works in almost ANY app (Social media, Notes, System logs). |
| 🧠 **Smart Parsing** | Uses advanced Regex to detect `http`, `https`, `www`, and common domains. |
| 🖱️ **Custom View Support** | Attempts to open visible links exposed through accessibility nodes in custom UI. |
| ⚡ **Zero Lag** | Optimized hook logic ensures **minimal** impact on performance. |
| 🛡️ **Safe Core** | Excludes `systemui` and `android` packages to prevent bootloops. |
| 🔄 **Auto-Updates** | Built-in updater checks GitHub Releases automatically. |

## 🛠 Requirements

* ✅ **Rooted Device** (Magisk / KernelSU).
* ✅ **LSPosed Framework** (Recommended) or Xposed.
* ✅ Android 11.0 (API 30) or higher.

## 📸 How It Works

The module intelligently hooks into `android.widget.TextView`:
1. **Scan:** Detects URL patterns during `setText`.
2. **Highlight:** Applies a **Cyan** span to make links visible.
3. **Interact:** Intercepts touch events to open the link directly in your browser.
4. **Fallback:** Checks accessibility text for custom views that do not use standard `TextView`.

## 📦 Installation

1. Download the **APK** from [Releases](https://github.com/mohamed-zaitoon/LinkifyAll/releases).
2. Install & Open **LSPosed Manager**.
3. Enable **LinkifyAll** module (Scope: System Framework + Target Apps).
4. **Reboot** device.
5. Enjoy clickable links everywhere!

---
Made with ❤️ by [Mohamed Zaitoon](https://mohamedzaitoon.com)
