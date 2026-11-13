# GLOVBLING

Root-gated Android companion panel for PUBG Mobile Global.

Features: KernelSU-compatible root authorization check, one-tap launch of the Global package com.tencent.ig, Android floating-window permission, animated expandable panel, two tabs, and a local training preview with adjustable tempo and FOV-circle illustration.

The training controls are app-only preferences. They do not control PUBG, inspect or modify game memory, automate aiming, or bypass anti-cheat.

## Build
GitHub Actions installs JDK 17, Gradle 8.13 and Android SDK 36, then builds the debug APK. Download the GLOVBLING-debug-apk artifact from the repository Actions tab.

## Device setup
1. Install the APK.
2. Grant GLOVBLING root access in KernelSU.
3. Open GLOVBLING and approve the root request.
4. Allow Display over other apps.
5. Press ЗАПУСТИТЬ GL.

The target package is com.tencent.ig. The project does not claim support for every regional game package or release.