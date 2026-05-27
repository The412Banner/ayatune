// Reconstructed from RE of com.ayaneo.gamewindow 1.5.84 (vc204) — 2026-05-26
// Source: ~/ayaneo-re/gamewindow_jadx/sources/com/ayaneo/gamewindow/AyaAidlInterface.java
//
// Bind: Intent.setClassName("com.ayaneo.gamewindow",
//                           "com.ayaneo.gamewindow.utils.aidl.AyaAidlService")
// Service is android:exported="true" with NO permission; any app may bind.
//
// Message format: "<msg_type>:<payload>"
// On registerCallback, the service sends back "msg_type_register:<clientId>"
// to inform you of your assigned clientId (used to route per-client responses).
//
// See docs/AIDL_SURFACE.md for the full payload/COM_* catalog.

package com.ayaneo.gamewindow;

import com.ayaneo.gamewindow.AyaAidlCallback;

interface AyaAidlInterface {
    // Transaction 1: fire-and-forget message send. Reply via your registered callback.
    void sendMsg(String message);

    // Transaction 2: register a callback. Service responds with "msg_type_register:<id>".
    void registerCallback(AyaAidlCallback callback);

    // Transaction 3: unregister a previously-registered callback.
    void unregisterCallback(AyaAidlCallback callback);
}
