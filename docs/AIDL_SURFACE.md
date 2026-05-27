# AYANEO Game Window AIDL surface — reference

Reconstructed by static RE of `com.ayaneo.gamewindow` v1.5.84 (vc204) on AYANEO Pocket FIT (AR14, SD8G2), Android 14 build `01.00_20251218`, 2026-05-26. Decompiled APKs at `~/ayaneo-re/`.

## Endpoint

| Property | Value |
|---|---|
| Package | `com.ayaneo.gamewindow` |
| Service | `com.ayaneo.gamewindow.utils.aidl.AyaAidlService` |
| Manifest | `android:exported="true"`, **no permission** |
| Process UID | `android.uid.system` (shared) |
| Default action | unspecified intent → binds via the AIDL binder |
| Alternate action | `"com.ayaneo.aidl.server"` → returns a `LocalBinder` (intra-package use, do not target) |

Bind from a 3rd-party app with:

```kotlin
val intent = Intent().setClassName(
    "com.ayaneo.gamewindow",
    "com.ayaneo.gamewindow.utils.aidl.AyaAidlService",
)
context.bindService(intent, conn, Context.BIND_AUTO_CREATE)
```

## Interface (binder transactions)

`com.ayaneo.gamewindow.AyaAidlInterface`:

| txn | Method | Direction | Notes |
|---|---|---|---|
| 1 | `sendMsg(String)` | client → service | Fire-and-forget. Service routes to handlers based on `msg_type`. |
| 2 | `registerCallback(AyaAidlCallback)` | client → service | Service replies with `msg_type_register:<clientId>` on the callback. |
| 3 | `unregisterCallback(AyaAidlCallback)` | client → service | |

`com.ayaneo.gamewindow.AyaAidlCallback`:

| txn | Method | Direction | Notes |
|---|---|---|---|
| 1 | `onAidlMessage(String)` | service → client | Format: `"<msg_type>:<payload>"`. |

## Message envelope

Outgoing messages have the form **`<msg_type>:<COM_command>:<value-or-json>`**. The service tokenizes on the first colon for the type and on the second colon for the command/payload split.

### Message types

From `com.ayaneo.settings.utils.aidl.AyaAidlManager` & `com.ayaneo.gamewindow.utils.aidl.AYAAidlManager`:

| Type | Used for |
|---|---|
| `msg_type_register` | Service → client only; payload is the assigned clientId |
| `msg_type_performance` | CPU/GPU/fan/scheduler/mode commands |
| `msg_type_fan` | Fan-strategy commands (linear vs curve, strategy id) |
| `msg_type_rgb` | RGB enable/disable, mode |
| `msg_type_controller` | ABXY/dpad/L1L2/R1R2, mapping, slot state |
| `msg_type_device` | Device-wide identity / firmware version init |
| `msg_type_system` | Magic-touch overlay, APK-install-done, etc. |
| `msg_type_close_wifi` | Special-cased per-game WiFi-off (config file `TurnOffWifi.ini`) |
| `msg_type_unknown` | Anything that doesn't have a `:` prefix |

### COM_ commands

From `com.ayaneo.settings.utils.aidl.AidlConstants`:

| Constant | String value | Payload shape |
|---|---|---|
| `COM_SET_PERFORMANCE_MODE` | `com_set_performance_mode` | Gson-serialized `ConfigData` (see schema below) |
| `COM_SET_PERFORMANCE_RESET` | `com_set_performance_reset` | (none) |
| `COM_SET_PERFORMANCE_FAN` | `com_set_performance_fan` | enum `FAN_MODE` name string |
| `COM_SET_PERFORMANCE_CPU` | `com_set_performance_cpu` | list of `CPUFrequency` |
| `COM_SET_PERFORMANCE_SCHEDULER` | `com_set_performance_scheduler` | enum `CPUSchedulerMode` |
| `COM_SET_PERFORMANCE_GPU` | `com_set_performance_gpu` | `GPUFrequency` |
| `COM_SET_PERFORMANCE_GPU_IS_FIXED` | `com_set_performance_gpu_is_fixed` | "true" \| "false" |
| `COM_SET_FAN_SPEED_STRATEGY` | `com_set_fan_speed_strategy` | int strategy id |
| `COM_SET_FAN_SPEED_IS_LINEAR` | `com_set_fan_speed_is_linear` | "true" \| "false" |
| `COM_SET_RGB_IS_OPEN` | `com_set_rgb_is_open` | "true" \| "false" |
| `COM_SET_CONTROLLER_STATE` | `com_set_controller_state` | controller state JSON |
| `COM_SET_CONTROLLER_STYLE` | `com_set_controller_style` | controller style |
| `COM_SET_KEY_MOUSE_QUICK_KEY` | `com_set_key_mouse_quick_key` | key-mouse map |
| `COM_GET_SINGLE_KEY_MAPPING` | `com_get_single_key_mapping` | single-key query |
| `COM_SET_SINGLE_KEY_MAPPING` | `com_set_single_key_mapping` | single-key write |
| `COM_INIT_LOCAL_FIRMWARE_VERSION` | `com_init_local_firmware_version` | (none) |
| `COM_GET_ABXY_MODE` / `COM_SET_ABXY_MODE` | `com_{get,set}_abxy_mode` | layout idx |
| `COM_GET_L1L2R1R2_MODE` / `COM_SET_L1L2R1R2_MODE` | `com_{get,set}_l1l2r1r2_mode` | layout idx |
| `COM_GET_DIRECTION_DPAD_MODE` / `COM_SET_DIRECTION_DPAD_MODE` | `com_{get,set}_direction_dpad_mode` | layout idx |
| `COM_WIFI_CLOSE` | `com_wifi_close` | "true" \| "false" |
| `COM_APK_INSTALL_DONE` | `com_apk_install_done` | filename |
| `COM_SHOW_MAGIC_TOUCH` | `com_show_magic_touch` | "true" \| "false" |

## ConfigData JSON schema

```json
{
  "currentMode": 0,           // 0=Saving, 1=Balance, 2=Game, 3=Max, 4=Streaming
  "modeConfigurations": {
    "0": {
      "cpuFrequencies": [
        {"cpuId": 0, "minFrequency": 364800, "maxFrequency": 2265600, "selectedFrequency": 1500000, "frequencies": [...]},
        ...                   // 8 entries, one per logical CPU
      ],
      "cpuSchedulerMode": "POWER_SAVING",   // POWER_SAVING | BALANCED | HIGH_PERFORMANCE
      "gpuFrequency": {
        "minFrequency": 231000000,
        "maxFrequency": 1050000000,
        "selectedFrequency": 1050000000,
        "isFixed": false
      },
      "fanMode": "FAN_MODE_BALANCE",        // FAN_MODE_OFF | _MUTE | _BALANCE | _TURBO | _CUSTOM
      "lastFanMode": "FAN_MODE_BALANCE"
    },
    "1": { ... },
    ...
  }
}
```

Persisted via `AyaShareConfUtilKt.e()` to a per-device key (`iAyaDevices.i1()` — for AR14 this is `"aya_performance_mode_config_ar14"` or similar; verify by file watch).

## What ends up at the sysfs layer

`AyaDevicesUtil.applyCPUFrequencies/SchedulerMode/GPUFrequency` translate the JSON above into shell writes. On the Pocket FIT (QCOM path, `AyaDevicesUtilKt.r || .s` true):

- **Governor (per policy)**: `echo <gov> > /sys/devices/system/cpu/cpufreq/policy{0,2,5,7}/scaling_governor`
  - Values: `powersave` / `walt` / `schedutil` / `performance`
- **Per-CPU max**: `echo <hz> > /sys/devices/system/cpu/cpu<N>/cpufreq/scaling_max_freq` (N = 0..7)
- **Per-CPU min**: `echo <hz> > /sys/devices/system/cpu/cpu<N>/cpufreq/scaling_min_freq` (only in HIGH_PERFORMANCE: min=max)
- **GPU idle timer**: `echo {80|10000000} > /sys/class/kgsl/kgsl-3d0/idle_timer` (low = aggressive downclock, high = effectively disabled)
- **GPU max**: `echo <hz> > /sys/class/kgsl/kgsl-3d0/max_gpuclk` AND `… > devfreq/max_freq`
- **GPU min**: `echo <hz> > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq` (= max when isFixed)

Other branches in `AyaDevicesUtil` cover MediaTek (`/proc/gpufreqv2/fix_target_opp_index`, `/proc/gpufreq/gpufreq_opp_freq`) and the YT family (boost switch, SystemSettings keys) — not relevant on Pocket FIT.

Shell exec is `ProcessBuilder("sh","-c", cmd).start()` from the system_uid process. **An app that wants to write these sysfs nodes directly must be uid.system or root**; standard `shell` uid (Shizuku) hits EACCES on the writes (reads are fine). See `docs/PRIVILEGED_WRITES.md` for ayatune's libsu-runtime-root approach.

## Notes & caveats

- The service replies to `registerCallback` with `msg_type_register:<id>` — your client must wait for that envelope before assuming the registration is live (the AYANEO code does this asynchronously and queues outgoing messages until `mBound` is true).
- `AyaAidlService.sendBroadcastMsg(type, msg)` fan-outs to *all* registered callbacks — so listening clients (including ours) get every mode change Settings or Game Launcher pushes. Useful for keeping our overlay UI in sync.
- The gamewindow Application has multiple other services in its manifest. Stick to `AyaAidlService` for tuning; the rest are key-event injection, screen recording, accessibility, etc.
- `com.ayaneo.provider.SharedPrefsProvider` and `AyaSettingProvider` are also `exported="true"` and expose the same KV settings (keys in `com.ayaneo.provider.AyaSetting`). Use these for state mirror / read-only queries that don't warrant a service binding.
