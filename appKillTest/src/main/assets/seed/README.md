# Kill-recovery test seed fixtures

`KillRecoveryTest` pushes these straight into the freshly-cleared app so it starts past
onboarding/the tutorial with a **1-minute focus profile** — no setup UI to drive.

- `productivity_settings.preferences_pb` — DataStore prefs with `showOnboardingKey=false`
  and `showTutorialKey=false`.
- `productivity_settings_autostart.preferences_pb` — same, plus `autoStartBreakKey=true`
  (used by the auto-start tests).
- `goodtime-db` — Room DB with the default label's `workDuration=1`.

## Regenerating (needed if the DataStore keys or the Room schema change)

The DB carries Room's schema identity hash, so a schema bump makes the app reject this
fixture (the tests then fail on launch). Regenerate on a device/emulator:

```sh
P=com.apps.adrcotfas.goodtime
adb shell pm clear $P
adb shell am start -n $P/.settings.GoodtimeLauncherAlias
# dismiss onboarding + the tutorial with the top-right X, leave everything else default
adb exec-out run-as $P cat files/productivity_settings.preferences_pb > productivity_settings.preferences_pb
adb exec-out run-as $P cat databases/goodtime-db > goodtime-db
sqlite3 goodtime-db "UPDATE localLabel SET workDuration=1 WHERE name='PRODUCTIVITY_DEFAULT_LABEL'; VACUUM;"

# for the auto-start fixture: with the app seeded, enable Settings > "Auto start break", then
adb exec-out run-as $P cat files/productivity_settings.preferences_pb > productivity_settings_autostart.preferences_pb
```
