# Project: Minimal Android TV Launcher

## Target Hardware & OS Rules
- Target device: TCL Android TV running Android 11 (API 30) on 32-bit ARM (`armeabi-v7a` / `armv7l` kernel).
- ABI compatibility: Always keep `armeabi-v7a` supported in Gradle build configurations.
- Memory efficiency: Keep background memory light. Avoid decoding large bitmaps on the main thread; cache app banners/icons safely and release unused resources cleanly.

## 10-Foot TV UI & Navigation Rules
- Remote-only navigation: Support D-pad (`DPAD_UP`, `DPAD_DOWN`, `DPAD_LEFT`, `DPAD_RIGHT`, `DPAD_CENTER`) and `BACK`. Do not rely on touch gestures.
- Focus visibility: Ensure every interactive item is focusable and shows a clear focus indicator (scale/border/contrast) so the user always knows where focus is on the screen.
- Overscan safety: Keep content padded away from extreme screen edges.
- Fast startup: Render cached/initial UI quickly without blocking the main thread on slow `PackageManager` scans.

## Architecture & Coding Standards
- Pattern: Clean MVVM (`ui` -> `ViewModel` -> `Repository`). Keep Android `Context` / `PackageManager` calls out of `@Composable` functions.
- Beginner-friendly clarity: Prefer straightforward, readable Kotlin over clever one-liners. Add concise comments on non-obvious Android/Kotlin APIs.
- Manifest rules: Ensure the launcher activity declares `MAIN`, `LEANBACK_LAUNCHER`, `HOME`, and `DEFAULT`, and marks touchscreen hardware as not required (`android:required="false"`).
