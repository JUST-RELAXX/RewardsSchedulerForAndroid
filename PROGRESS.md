# RewardsScheduler for Android — Progress Tracker

## Build Steps Checklist

- [x] **Step 1 — SPIKE A (Isolation):** ✅ DONE. Device: samsung SCG02 (BlueStacks), Android 9 (API 28), WebView: com.android.chrome v129.0.6668.70. Multi-Profile SUPPORTED. Isolation PROVEN: Profile A cookies invisible to Profile B (Profile B reads null).
- [~] **Step 2 — SPIKE B (Background Execution):** Foreground service calling evaluateJavascript every ~30s on a collapsed (1dp) WebView, app in background, screen off, 10+ min. Built and ready in "Spike B ⏳" tab.
- [x] **Step 3 — Engine:** ✅ DONE. Models, tracker, backend client (auth/prompts/gate/single-flight/retry/fallback), delay generator, search runner + unit tests passed (spec rounding table, bounds, E in 3..7, delay sum == T, queue assembly).
- [x] **Step 4 — Compose Theme + Portrait Dashboard:** ✅ DONE. Exact PC dark theme tokens (#0a0e1a, #111827, #1a2235, #00b0ff, #ffab00, #ff1744, #00e676), live clock status banner, nutshell overview, color-coded console terminal, holistic controls and settings.
- [x] **Step 5 — Account Slots:** ✅ DONE. Add/rename/delete slots, isolated Microsoft sign-in dialog per profile slot, local JSON persistence synchronized with daily tracker.
- [x] **Step 6 — Session Screen:** ✅ DONE. Non-lazy Session Tiles layout (Column + verticalScroll), Collapse Previews toggle ("Send to void" 1dp collapse keeping WebViews attached), Nutshell click-to-locate with 2.5s glow, Canvas pacing graph dialog, per-profile Config dialog.
- [x] **Step 7 — Foreground Service Integration:** ✅ DONE. SearchForegroundService with Partial WakeLock, specialUse type, battery optimization exemption prompt, smart notification reminders ported from scheduler.js (1 min initial, 2h recurring, casual→medium→high→critical approaching 10:30 PM, snooze actions).
- [x] **Step 8 — Per-profile Overrides:** ✅ DONE. Max points cap (default 60), timeframe minutes, custom delay bounds, search topic categories, and manual "Refresh Prompts" button.
- [ ] **Step 9 — Real-device Verification:** 3 accounts, full 30-minute run in BlueStacks / physical device; confirm searches ran in background.

## Handoff Notes

### 2026-10-21 — Session Update (Full App Built & Packaged)
- Built complete Portrait native Android app matching PC app "MS Rewards Scheduler".
- Unit tests (`DelayGeneratorTest`, `PromptQueueTest`) executed and passed (26 tasks clean).
- Debug APK successfully compiled: `app/build/outputs/apk/debug/app-debug.apk` (32.6 MB).
- App includes top navigation tabs:
  1. `🚀 Dashboard`: The complete native app with Nutshell, Console, Controls, Holistic Settings, 2-column Account Cards grid, and Session Tiles.
  2. `Spike B ⏳`: The dedicated background execution test for testing evaluateJavascript every 30s with screen off.
  3. `Spike A ✅`: The isolation test verified on BlueStacks.

## Handoff Notes

### 2026-10-21 — Initial Session
- Created PROGRESS.md
- Read PC repo (master branch): styles.css, extension-logic.js, search-engine.js, tracker.js, scheduler.js, main.js, renderer/app.js
- Extracted design tokens: bg-primary=#0a0e1a, bg-secondary=#111827, bg-card=#1a2235, accent=#00b0ff, warning=#ffab00, danger=#ff1744, success=#00e676, radius=12px, font=Segoe UI/system-ui
- Extracted backend: POST {backend}/auth with {key} → token; GET {backend}/getPrompts?topics=… with Bearer token
- Verified WebView multi-profile API: `ProfileStore.getInstance()`, `getOrCreateProfile(name)`, `WebViewCompat.setProfile(webView, name)`, gated by `WebViewFeature.MULTI_PROFILE`; requires `androidx.webkit` 1.8.0+
- Verified foreground service type: `specialUse` is correct (sideload-only, not Play Store)
- FALLBACK_QUERIES extracted from extension-logic.js (57 queries)
- PC delay logic differences noted: PC uses Math.ceil, automize random start index, +10/+15 padding — spec says do NOT copy, implement spec exactly
- Installing Android CLI for project scaffolding
