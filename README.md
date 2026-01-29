<div align="center">

# 🔗 LinkifyAll
### Turn Text into Action. System-Wide.

[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Xposed](https://img.shields.io/badge/Xposed-Module-orange?style=for-the-badge&logo=xposed&logoColor=white)](https://repo.xposed.info/)
[![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)

<p align="center">
  <b>LinkifyAll</b> is a powerful Xposed/LSPosed module that forces non-clickable text URLs to become clickable links system-wide.
  <br>It hooks into Android's <code>TextView</code> to revolutionize how you interact with text.
</p>

[📥 Download Latest APK](https://github.com/mohamed-zaitoon/LinkifyAll/releases) • [🐛 Report Bug](https://github.com/mohamed-zaitoon/LinkifyAll/issues)

</div>

---

## ✨ Why LinkifyAll?

| Feature | Description |
| :--- | :--- |
| 🌍 **System-Wide** | Works in almost ANY app (Social media, Notes, System logs). |
| 🧠 **Smart AI** | Uses advanced Regex to detect `http`, `https`, `www`, and emails. |
| ⚡ **Zero Lag** | Optimized hook logic ensures **minimal** impact on performance. |
| 🛡️ **Safe Core** | Excludes `systemui` and `android` packages to prevent bootloops. |
| 🔄 **Auto-Updates** | Built-in updater checks GitHub Releases automatically. |

## 🛠 Requirements

* ✅ **Rooted Device** (Magisk / KernelSU).
* ✅ **LSPosed Framework** (Recommended) or Xposed.
* ✅ Android 8.0 (Oreo) or higher.

## 📸 How It Works

The module intelligently hooks into `android.widget.TextView`:
1.  **Scan:** Detects URL patterns during `setText`.
2.  **Highlight:** Applies a **Cyan** span to make links visible.
3.  **Interact:** Intercepts touch events to open the link directly in your browser.

## 📦 Installation

1.  Download the **APK** from [Releases](https://github.com/mohamed-zaitoon/LinkifyAll/releases).
2.  Install & Open **LSPosed Manager**.
3.  Enable **LinkifyAll** module (Scope: System Framework + Target Apps).
4.  **Reboot** device.
5.  Enjoy clickable links everywhere!

---
<div align="center">
Made with ❤️ by <a href="https://mohamedzaitoon.com">Mohamed Zaitoon</a>
</div>
