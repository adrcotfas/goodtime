# Process kill recovery on aggressive OEMs

Notes from investigating an ACRA report: `ForegroundServiceStartNotAllowedException` on a
OnePlus/ColorOS device, 2026-07-30.

## The crash

```
android.app.ForegroundServiceStartNotAllowedException: Service.startForeground() not allowed
due to mAllowStartForeground false: service com.apps.adrcotfas.goodtime/.bl.TimerService
```

Timeline from the report, all inside 600ms of a cold process start:

| time | event |
|---|---|
| `08:44:48.730` | process start — system sticky-restart of `TimerService`, null intent |
| `08:44:48.938` | `TimerManager` restores the profile |
| `08:44:48.957` | `I/TimerService: re-adopting the in-progress notification` |
| `08:44:48.987` | `startForeground()` throws |
| `08:44:49.195` | fatal |

That log line exists only in `reconcileAfterProcessDeath()`, reachable only from the
`intent == null` branch of `onStartCommand` — the sticky restart. Introduced in `cbc18592`
("recover running sessions when the OS kills the app").

Android 12+ denies a foreground start when the app has no background FGS allowance; a
sticky restart has none. The exception says so itself: `mAllowStartForeground false`.

The report does **not** say what killed the previous process. LMK, an OEM battery manager,
or an app update all look identical from here. It was not a force-stop — force-stopped apps
don't get sticky restarts.

### What the user saw

The throw happened before `timerManager.onSendToBackground()`, so the coroutine died there
(`Suppressed: sc1: [b36{Cancelling}...]` in the trace) and no `SendToBackground` event ever
reached `AlarmManagerHandler` — nothing called `setAlarm`. Nothing else had armed one
either: if the app was in the foreground when killed, `isForeground` was `true` and
`BringToForeground` had already run `cancelAlarm()` (`AlarmManagerHandler.kt:38-42`).

Meanwhile the notification is built with `setUsesChronometer(true)` +
`setChronometerCountDown(true)` + `setWhen(now + baseTime)`
(`NotificationArchManager.kt:114-118`). SystemUI keeps rendering past `when` as negative.
Nobody owns it, so it counts down forever. Session never finishes, never saved.

## The fix

`TimerService.startInProgressForeground()` wraps both `startForeground` call sites in
`try/catch (IllegalStateException)` — `ForegroundServiceStartNotAllowedException` extends
it, so no API-level guard needed.

The catch lets `onSendToBackground()` run. `AlarmManagerHandler` is in its
`isForeground = false` branch in a fresh process (`AlarmManagerHandler.kt:35-36`), gets
`SendToBackground` with `isTimerRunning = true`, and calls `setAlarm(endTime)`. When it
fires: `AlarmReceiver` → `finish()` → `Event.Finished` → the service's `Finished` branch →
`clearInProgressNotification()`, which cancels by ID and takes the orphan with it. Until
then the orphan is a countdown to the correct `endTime`, so it stays accurate.

The `StartOrUpdate` call site is not an equivalent gap: the alarm there is already armed by
the `Event.Start` that triggered the service (`AlarmManagerHandler.kt:60-63`). Losing the
foreground promotion costs the visible notification, not the session.

Listeners are wired at `TimerManager` construction (`AppModule.android.kt:117-121`), so
they're live in a service-only process — no Activity needed for any of this.

Not covered by a test: `mAllowStartForeground false` depends on OEM/device state.
`:appKillTest` exercises the kill path but takes the allowed branch on a normal device.
Would need fault injection to reach the catch.

## Still open, ranked

### 1. ~~The finish moment depends on a service start~~ — fixed

It used to: `TimerServiceStarter` caught the denial but only cleaned up for `Reset`; for
`Finished` it swallowed and returned — chime but no notification, and the orphan
countdown never cleared.

Fixed by `FinishedNotificationHandler`, an in-process `EventListener`: it posts
`notifyFinished` and clears the finished/in-progress notifications directly on
`Finished`/`Start`/`Reset`, so none of it depends on a service start anymore.
`TimerService` handles only the foreground lifecycle; the post-vs-clear race
(`pendingFinishedNotificationJob`) moved into the handler with it.

### 2. Nothing more needed for the re-adoption denial

See above — the orphan stays accurate and the re-armed alarm cleans it up. A fallback
plain notification would be motion, not progress.

### 3. OEM force-stop is unfixable in-process

"Deep clear" on ColorOS/MIUI cancels pending alarms and blocks sticky restart. None of our
code runs, ever. Two honest mitigations:

- **Heal on open** — already works. `TimerForegroundMonitor.listenForeground()`
  force-finishes an expired countdown and saves it (`TimerForegroundMonitor.kt:64-68`).
  The session survives; only the timely notification was lost.
- **Tell the user once, with proof.** The battery-optimization request already exists
  (`ActionsSection.android.kt:58`, `PermissionsState.kt:40`) but sits passively in
  settings. Set a flag when a kill is actually detected — `reconcileAfterProcessDeath()`
  ran, the new catch fired, or heal-on-open finished a session that expired minutes ago —
  and on next open show a dismissible banner pointing at that prompt, plus
  dontkillmyapp.com for the OEM. Reuses existing UI, fires only for people it's
  demonstrably true for. Needs a "when do we stop showing it" decision first.

## Skip

Watchdog service, periodic wakelocks, repeating keep-alive alarms. They burn battery, and
on these phones they make the OEM killer more aggressive, not less.
`setRequestPromotedOngoing(true)` is already set (`NotificationArchManager.kt:109`), which
is the right lever on Android 16.
