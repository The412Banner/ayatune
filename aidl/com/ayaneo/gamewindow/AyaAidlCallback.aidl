// Reconstructed from RE of com.ayaneo.gamewindow 1.5.84 (vc204) — 2026-05-26
// Source: ~/ayaneo-re/gamewindow_jadx/sources/com/ayaneo/gamewindow/AyaAidlCallback.java
//
// Implementation receives broadcast notifications from AyaAidlService when
// any client (including AYANEO Settings, Game Launcher, Home) changes a
// performance/fan/rgb/controller setting. Payload uses the same
// "<msg_type>:<COM_command>:<json or value>" format as outgoing messages,
// e.g. "msg_type_performance:com_set_performance_mode:{\"currentMode\":2,...}"

package com.ayaneo.gamewindow;

interface AyaAidlCallback {
    void onAidlMessage(String message);
}
