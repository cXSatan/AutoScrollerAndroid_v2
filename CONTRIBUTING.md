# Contributing to AutoScroller Android

Welcome! This document explains how we work on this project.

---

## 📌 Context
- Purpose: Android AutoScroller with AccessibilityService and floating overlay indicator (▶️/⏹).
- State: Builds & installs on emulator. Foreground overlay service + onboarding SetupActivity working.
- Repo: https://github.com/cXSatan/AutoScrollerAndroid

---

## 📂 Code structure
- MainActivity.kt — minimal launcher stub
- ui/setup/SetupActivity.kt — onboarding menu (open Accessibility, overlay, notifications, start overlay indicator)
- OverlayIndicatorService.kt — floating bubble + foreground service
- util/NotificationHelper.kt — notification channel creation
- util/Permissions.kt — permission helpers
- es/layout/activity_setup.xml — setup UI
- es/layout/overlay_indicator.xml — bubble
- es/xml/accessibility_service_config.xml — service metadata
- AndroidManifest.xml — declares service + required permissions

---

## ✅ Contribution Guidelines
- **One change per PR** → e.g. “indicator fade animation”.
- Don’t move files without discussion.
- All new UI → ui/..., services → root/service, helpers → util/....
- Use **UTF-8 (no BOM)** for XML/Kotlin.
- Commit small, focused changes with descriptive messages.

---

## 🚀 Development
- Build/Install: ./gradlew :app:installDebug
- Run setup activity:  
  db shell am start -n com.example.autoscroller/.ui.setup.SetupActivity
- Logs:  
  db logcat | findstr /i "autoscroller"

---

## 🔜 Next steps
- Wire bubble icon toggle to actual scroll start/stop.
- Fade-in/out polish for indicator.
- SetupActivity should show live permission status.
- Confirm service shows up under **Accessibility → Installed services → AutoScroll Accessibility Service**.
