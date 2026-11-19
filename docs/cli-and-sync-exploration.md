# CLI, agent integration & multi-device sync — exploration

Status: brainstorm, nothing implemented. 2026-11-19.

## Part 1 — goodtime-cli / agent integration

### Seams that already exist

- **Commands**: `TimerManager` exposes `start`, `toggle`, `skip`, `next`, `finish`, `reset`, `addOneMinute`; label/session repos cover CRUD.
- **Events**: `EventListener.onEvent(Event)` (`bl/Event.kt`) — a bridge is just another listener.
- **Gap**: `Event.Finished` only carries `TimerType`, not the persisted `Session`. For a useful "session finished" event (label, duration, notes), emit from `FinishedSessionsHandler.saveSession` instead.

Any integration = adapter: JSON command in → `TimerManager`/repos; `EventListener`/save path → JSON event out.

### Transports per platform

| Platform | Inbound (control) | Outbound (listen) |
|---|---|---|
| Android (today) | Exported broadcast receiver (`goodtime.action.START`, …) guarded by a permission → Tasker, `adb`, Termux. AppFunctions for on-device agents | Webhook POST on session saved (contributors' proposal) |
| iOS | App Intents → Shortcuts/Siri | Shortcuts automation, webhook |
| Desktop (Compose Desktop) | Local daemon: localhost HTTP + token, or Unix socket | NDJSON/SSE stream, exec hooks |
| Web | Needs a backend; CLI talks to cloud API with personal token | SSE + webhooks from server |

Desktop app process = long-lived timer daemon; `goodtime` CLI is a thin client (`docker` ↔ `dockerd`).

### CLI sketch

```
goodtime start [--label deep-work] [--break] [--count-up]
goodtime pause | resume | skip | reset | +1m
goodtime status --json
goodtime label add "deep-work" --color 3 [--work 50 --break 10]
goodtime label ls --json
goodtime session add --label x --start 2026-11-19T09:00 --minutes 50 --notes "..."
goodtime session ls --since today --json
goodtime watch          # NDJSON event stream, one event per line
goodtime mcp            # same commands as MCP tools over stdio
```

### Listening for events (cheapest → heaviest)

1. **`goodtime watch`** — NDJSON stream; composes with `jq`, agents can tail it.
2. **Exec hooks** — `~/.config/goodtime/hooks/session-finished` gets event JSON on stdin. No server config; covers most "do X when a session ends".
3. **Webhooks** — same JSON POSTed to a URL. Only option that works from mobile.

### Recommendation

One JSON schema for commands + events, several thin adapters:
- Desktop: localhost HTTP + bearer token (file with user-only permissions); CLI, `watch`, hooks, `mcp` are wrappers.
- Mobile: webhooks out + intents/App Intents in, same payloads.
- MCP: just maps tools to CLI/HTTP commands, no own logic. Ship after the CLI.

Cheapest useful step today: Android webhook on session save + permission-guarded intent receiver.

### Gotchas

- Timer uses `elapsedRealtime()`; external payloads need epoch wall-clock. Convert at the adapter.
- CLI must not write the Room DB directly (bypasses streak / break-budget / finished-session logic, concurrent writers). Read-only fallback OK.
- Android exported receiver without a permission = any app can reset the timer. Desktop localhost without a token = any web page sending requests to localhost can control it.
- Address labels by name; make `label add` idempotent.
- Web version only makes sense with sync (Part 2).

## Part 2 — real-time sync between devices ("mirrored devices")

Split into two problems with two mechanisms:

1. **Live timer state** — one small doc per user, last-writer-wins, pushed to all devices.
2. **Labels / sessions / profiles / settings** — row sync with `updatedAt` + tombstones, pull-since-cursor; realtime push is only a "changed, pull now" ping.

No tick streaming, no event replay: a running timer is a few fields; each device computes the countdown locally.

### Timer state doc

```json
{ "status": "running|paused|idle", "type": "focus|break|longBreak",
  "label": "deep-work", "sessionId": "uuid",
  "endsAtEpochMs": 1795000000000, "pausedRemainingMs": null,
  "isCountdown": true, "rev": 42, "byDevice": "pixel8" }
```

- Any action → write whole doc with `rev+1`. Receivers overwrite local state and reschedule alarm/notification (existing `reconcile()` path rebuilds from persisted state).
- `elapsedRealtime()` is per-device → convert to wall clock at the boundary; correct skew with a server-time offset measured on connect.
- Duplicate finished sessions: generate `sessionId` at start, finish = idempotent upsert on it.
- Alarms: all devices ring vs. only the active one — product decision / setting.

### Data sync — schema blockers

- `LocalSession.id` is autoincrement `Long` → collides across devices. Add a `uuid` column as sync identity.
- `LocalLabel` PK is `name`, sessions FK-cascade on rename → a rename on A looks like delete+create on B, sessions can fall to the default label. Sync labels by a new `uuid` (name becomes an attribute) or model rename as an explicit op. uuid is cheaper to get right.
- Add `updatedAt` + `deleted` to synced tables; push dirty rows, pull `updatedAt > cursor`, LWW per row.

### Backend options (cheapest first)

| Option | Cost | Notes |
|---|---|---|
| **Cloudflare Durable Object per user** (WebSocket + built-in SQLite) | Free tier; hibernating sockets ≈ free when idle | Best fit. Holds timer doc + change log, fans out over WS. ~200 lines TS. Platform-agnostic, no Google dep (F-Droid friendly) |
| MQTT retained messages (Mosquitto / hosted) | ~0 | Retained msg on `user/<id>/timer` = current state on connect. Timer only; no session storage |
| Firestore | Free tier | Least code (listeners, offline, iOS SDK). Google-only → bad for F-Droid; model drifts from Room |
| Supabase Realtime | Free tier, self-hostable | Postgres + change feed; more moving parts |
| Local-first libs (PowerSync, ElectricSQL, Automerge) | varies | Overkill — data is LWW-friendly, CRDTs buy nothing. PowerSync KMP wants to own SQLite, conflicts with Room |
| LAN P2P (mDNS + WS) | free | Same-network only — skip |

### Suggested path

1. Timer state only: Durable Object with one JSON doc, clients over WebSocket (Ktor client, KMP). Already gives the "mirrored" feel.
2. Data: uuid + `updatedAt`/`deleted` columns (one Room migration), push/pull via the same object, WS as change ping.
3. Auth: pairing code (show on A, enter on B → shared user token). No accounts; backup/RevenueCat untouched.

Skip until needed: CRDTs, op logs, multi-user backend. The same Durable Object can later host the CLI/webhook surface from Part 1.
