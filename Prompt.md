# TASK — Build RewardsScheduler for Android (portrait version of the PC app)

You are building a native Android app that reproduces my PC app "MS Rewards
Scheduler" (Electron, repo: https://github.com/JUST-RELAXX/RewardsScheduler).
READ THAT REPO FIRST. It is the source of truth for behavior and UI. Same UI,
same features — the PC layout is landscape, this one is portrait.

I do not write code. You are the engineer. Rules for working with me:
- If something is ambiguous or you cannot verify it, STOP AND ASK. Do not guess.
- Do not commit or push without my explicit approval.
- Stay in scope. Do not add features I didn't ask for.
- Never hardcode secrets. Read config from local.properties → BuildConfig.
  (The backend access key already sits in the public PC repo's app.js — never
  copy it into this repo's source; read it from local.properties.)

## Step 0 — resumable progress (do this before anything else)
You may be replaced mid-task by a different agent (usage limits). Look for
PROGRESS.md in the project root. If missing, create it with a checklist of
every step below (all unchecked) plus a "Handoff notes" section. If present,
read it, resume from the first unchecked step, and sanity-check that anything
marked done actually exists. Update PROGRESS.md the moment each step finishes,
not at the end. Add dated handoff notes whenever you stop.

## What it does (same as PC)
Automates Bing searches for multiple Microsoft accounts to earn Rewards points.
Each account = one isolated browser session. The app fetches AI-generated
search phrases from my backend, computes human-like delays, and runs the
searches while tracking points per account.

## Platform reality — read carefully, these are verified constraints
1. Android's sandbox means the app CANNOT auto-detect accounts signed into
   other apps/browsers. The PC app reads Edge's profile folders; there is no
   equivalent here. Replace it with manual "Add account" slots: the user signs
   in once inside this app, the session persists.
2. Isolation between accounts: Android's default CookieManager is a single
   process-wide store shared by ALL WebViews in an app, so multiple plain
   WebViews would leak logins into each other. Use WebView Profiles
   (androidx.webkit: ProfileStore / WebViewCompat.setProfile, gated by
   WebViewFeature.MULTI_PROFILE — one named Profile per account slot, each
   with its own cookie/storage). I am giving these names from memory and
   Chromium's docs — VERIFY exact API names, required androidx.webkit version
   and WebView version against current official docs before relying on them.
   Feature-detect at runtime and show a clear "update Android System WebView"
   message if unsupported. Do NOT fall back to AccessibilityService or app
   cloning — out of scope.
3. Sideload-only app (APK). Not for the Play Store. That is fine.
4. I cannot confirm Microsoft credits searches made inside an embedded
   WebView on mobile. (The PC app's webview searches do earn points.) Default
   to the system WebView user agent; make the user-agent a setting. Never
   claim points are being earned — only the real Rewards page can confirm it.
5. Mobile Rewards daily point caps may differ from PC. Keep "max points per
   profile" user-configurable, default 60.

## Architecture (decisions already made — follow them)
- Kotlin + Jetpack Compose. Single Activity. Dark theme replicating the PC
  design tokens (read :root in renderer/styles.css: accent #00b0ff, warning
  #ffab00, danger #ff1744, same card radii, glows, fonts). Portrait only.
- The ENGINE (prompt fetching, delay generation, search loop, tracker, timers)
  lives in a Kotlin foreground Service using coroutines — NOT in UI JS timers.
  The engine calls webView.evaluateJavascript() to perform each action.
  Reason: JS timers inside backgrounded WebViews can be throttled.
- WebViews are owned by an application-scoped WebViewManager, one per account
  slot, each bound to its own Profile. They must NEVER be created/destroyed by
  composition. A Compose item scrolling off-screen must not kill or pause a
  WebView. Use a NON-lazy layout (Column + verticalScroll) for session tiles
  (max ~7), not LazyColumn.
- "Send to void" equivalent: a Collapse Previews toggle that shrinks each
  tile's WebView to ~1dp height while keeping it attached and VISIBLE (do not
  use GONE, and do not detach it). Do not call WebView.onPause()/pauseTimers()
  when the app goes to background.
- Foreground service with persistent notification, PARTIAL_WAKE_LOCK while a
  session runs, and a one-time prompt to exempt the app from battery
  optimization. Declare the correct foreground-service type for the target SDK
  — verify against current Android docs, don't guess.
- Default concurrent profiles: 3 (phones have far less RAM than the PC).
  User-configurable.
- Persistence: JSON/DataStore/Room (your choice, keep it simple). Reuse the PC
  tracker schema: per profile per day { searchesDone, onlineTimeDone,
  completedAt, searchCount, onlineMinutes, inProgress }, keyed by LOCAL date
  (YYYY-MM-DD) — Rewards resets at local midnight.

## Backend (same as PC — read renderer/extension-logic.js for exact shapes)
- POST {backend}/auth with {key} → {token}. GET {backend}/getPrompts?topics=…
  with "Authorization: Bearer <token>" → { prompts: [...], key_used }.
  Backend base URL: read it from the PC code; keep it in BuildConfig.
- Auto-fetch is GATED: no profile fetches prompts on launch until the user
  taps the global "Fetch" button (it then relabels "Fetch Again"). The
  per-profile Config panel's own "Refresh Prompts" button always works,
  regardless of the gate. (Protects API limits when sessions are opened only
  for logging in.)
- Handle failures per profile: log the real reason, retry once, then fall back
  to the built-in offline query list (copy FALLBACK_QUERIES from the PC code).
  Single-flight per profile: if a fetch is already running for a profile,
  await it — never fire two.

## DELAY & SEARCH-COUNT SPEC — implement EXACTLY this
IMPORTANT: the PC app's current code does NOT match this spec (it has an
"automize" random start index, uses Math.ceil, and adds a fixed +10/+15
padding blended into the same time budget). Do NOT copy that logic. Implement
the spec below, and tell me anywhere your reading of the PC code differs.

1. Every run starts from prompt #1 (index 0). No random/serial start offset.
   (A manual "start from query N" field may exist as an explicit override.)
2. Each search = 3 points. Required searches N = round(maxPoints / 3), where
   maxPoints is the per-profile setting (default 60). Rounding: fraction < 0.5
   rounds down, ≥ 0.5 rounds up. For non-negative integers use
   N = (maxPoints + 1) / 3 with INTEGER division. Required test cases:
   60→20, 30→10, 15→5, 43→14, 44→15, 41→14, 40→13, 42→14, 3→1, 2→1, 1→0.
3. Timeframe T: per-profile custom timeframe (minutes) if set, else the global
   "Completion Time" setting (default 30 min). Generate exactly N delays that
   SUM to T seconds, each within [minDelay, maxDelay] (read the PC code for
   those bounds and for the "chaos" generation algorithm — start every delay
   at min, distribute the remainder randomly respecting max, shuffle, then
   randomly shift small amounts between adjacent pairs so neighbors don't
   look uniform). If T is infeasible for N (N*min > T or N*max < T), clamp T to
   the nearest feasible value, and log the adjustment in the console.
4. After the N required searches finish, do E EXTRA searches, E chosen
   uniformly at random from the integers 3..7 inclusive, so the total isn't a
   suspiciously exact number. Extra searches are a separate tail AFTER the
   timeframe-fitting phase — they are not squeezed into T. Give them delays
   drawn from the same [minDelay, maxDelay] range.
5. If N + E exceeds the fetched prompt count (typically ~25), take the
   remaining queries from the offline list, shuffled, never repeating a query
   already used in this session. Never stall for lack of prompts.
6. Unit-test items 2–5 (rounding table, delay sum == T after clamping, every
   delay within bounds, E always within 3..7, index starts at 0).

## UI — port every PC module, stacked for portrait (single vertical scroll)
Order: status banner → Dashboard (total points, "Nutshell" live per-profile
list with points) → Console (timestamped, colour-typed log lines) → Controls
→ Holistic Settings (global completion time, Flash button, profile-level
settings dropdown) → account cards grid ("Select Profiles" 2 columns:
email/label, badge, progress bar, "Xm / 30m" online time, selection glow;
green when done; selected+done must still glow) → in a session: one tile per
running account.
- Controls row: Fetch (accent) · Stop (warning, pause icon: halts all
  searching instantly, windows/sessions stay open) · Exit (danger: ends the
  session). Plus Refresh All and START GLOBAL.
- Tile header: account label + Graph, Config, Close(×) buttons — icon-only
  when narrow, never wrapping or overflowing. Close asks for confirmation,
  stops that profile and animates its Nutshell row away. Config panel =
  per-profile settings + manual "Refresh Prompts". Graph = per-profile
  analytics chart.
- Nutshell click-to-locate: tapping a profile in the Nutshell scrolls to its
  tile and glows the tile edges for ~2.5s; long-press/hover hint "Click to
  locate".
- Account slot management replaces Edge profile discovery: Add account
  (opens a sign-in WebView at the Microsoft login, then Bing), user-entered
  label (best-effort read of the signed-in name from the page, but never
  depend on it), rename, delete (confirm; wipes that Profile's data).
- Reminders: port scheduler.js — first reminder ~1 min after launch, every 2
  hours, urgency casual→medium→high→critical approaching 10:30 PM, none after
  10:30 PM or before 6 AM, stop when all accounts are done, snooze. Use
  WorkManager/AlarmManager notifications with Yes / Not now / Later actions.
- Do NOT port: Edge profile discovery, window tiling/always-on-top, BlueStacks,
  the Browse-with-Edge companion, CDP/puppeteer, the draggable divider (leave
  a TODO for a collapsible header later). Mark Chart/Graph as phase 3 if time-
  boxed.

## Build order — de-risk FIRST. Stop and report to me after Steps 1 and 2.
1. SPIKE A — isolation: scaffold project; two WebViews on two different
   Profiles; prove isolation (set a cookie/localStorage value on bing.com in
   one Profile, confirm the other can't see it; confirm after app restart).
   Report device, Android version, WebView version, and result.
2. SPIKE B — background execution: foreground service calling
   evaluateJavascript every ~30s on a collapsed (1dp) WebView, app in
   background, screen off, 10+ minutes. Log timestamps; report whether it was
   throttled or paused. If it fails, propose fixes and ASK before changing the
   architecture.
3. Engine: models, tracker, backend client (auth/prompts/gate/single-flight/
   retry/fallback), delay generator, search runner + unit tests (spec above).
4. Compose theme + portrait dashboard modules (static data first).
5. Account slots: add/rename/delete, sign-in flow, persistence.
6. Session screen: tiles, Fetch/Stop/Exit, Nutshell + locate, Console,
   per-profile Config, close-with-animation, Collapse Previews toggle.
7. Foreground service integration, wake lock, battery-optimization prompt,
   reminders.
8. Per-profile overrides (max points, timeframe, delays), settings screen.
9. Real-device verification checklist: 3 accounts, one full 30-minute run;
   confirm searches actually ran in background and that the real Microsoft
   Rewards page shows the points. I will do the final points check myself.

## Definition of done per step
Builds clean, unit tests pass, and you tell me exactly what you ran and what
you could not verify. If you cannot test something on a device, say so
plainly — never claim it works.