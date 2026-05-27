package banner.tune.core.ayaneo

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.ayaneo.gamewindow.AyaAidlCallback
import com.ayaneo.gamewindow.AyaAidlInterface
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Client wrapper around AYANEO Game Window's exported [AyaAidlInterface].
 *
 * Messages are "<msg_type>:<COM_command>[:<payload>]" — see
 * docs/AIDL_SURFACE.md. The service replies to [registerCallback] with
 * "msg_type_register:<clientId>"; subsequent broadcasts from any other
 * client (AYANEO Settings, Game Launcher, ourselves) arrive on the same
 * callback.
 *
 * Lifecycle: [connect] binds; [disconnect] unbinds; on service death the
 * connection auto-clears and [state] flips back to Disconnected.
 *
 * This wrapper does NOT translate domain messages — that's the job of
 * higher-level facades (PerformanceFacade, FanFacade, etc.). Send/receive
 * raw strings here, parse upstream.
 */
class AyaBridge(private val appContext: Context) {

    sealed interface State {
        data object Disconnected : State
        data object Connecting : State
        data class Connected(val clientId: String?) : State
        data class Failed(val reason: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Disconnected)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    /** Every broadcast from the service (including the initial register reply). */
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var service: AyaAidlInterface? = null
    private var clientId: String? = null

    private val callback = object : AyaAidlCallback.Stub() {
        override fun onAidlMessage(message: String) {
            Timber.tag(TAG).d("recv: %s", message)
            if (message.startsWith(MSG_TYPE_REGISTER + ":")) {
                clientId = message.removePrefix(MSG_TYPE_REGISTER + ":")
                _state.value = State.Connected(clientId)
            }
            _messages.tryEmit(message)
        }
    }

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            Timber.tag(TAG).i("onServiceConnected %s", name)
            val svc = AyaAidlInterface.Stub.asInterface(binder)
            service = svc
            try {
                svc.registerCallback(callback)
                // State stays Connecting until the service's register reply
                // lands on the callback above and sets Connected(clientId).
            } catch (t: Throwable) {
                Timber.tag(TAG).e(t, "registerCallback failed")
                _state.value = State.Failed("registerCallback: ${t.message}")
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Timber.tag(TAG).w("onServiceDisconnected %s", name)
            service = null
            clientId = null
            _state.value = State.Disconnected
        }

        override fun onBindingDied(name: ComponentName) {
            Timber.tag(TAG).w("onBindingDied %s", name)
            disconnect()
        }

        override fun onNullBinding(name: ComponentName) {
            Timber.tag(TAG).e("onNullBinding %s", name)
            _state.value = State.Failed("null binding from $name")
        }
    }

    fun connect(): Boolean {
        if (_state.value !is State.Disconnected && _state.value !is State.Failed) return true
        _state.value = State.Connecting
        val intent = Intent().setClassName(GAMEWINDOW_PKG, AIDL_SERVICE_CLS)
        val bound = try {
            appContext.bindService(intent, conn, Context.BIND_AUTO_CREATE)
        } catch (t: SecurityException) {
            Timber.tag(TAG).e(t, "bindService SecurityException")
            _state.value = State.Failed("SecurityException: ${t.message}")
            return false
        }
        if (!bound) {
            _state.value = State.Failed("bindService returned false (is AYANEO Game Window installed?)")
        }
        return bound
    }

    fun disconnect() {
        val svc = service
        if (svc != null) {
            try { svc.unregisterCallback(callback) } catch (_: Throwable) { }
        }
        try { appContext.unbindService(conn) } catch (_: IllegalArgumentException) { }
        service = null
        clientId = null
        _state.value = State.Disconnected
    }

    /** Send a "<type>:<payload>" message; bail silently if not connected. */
    fun send(type: String, payload: String): Boolean {
        val svc = service ?: return false
        return try {
            svc.sendMsg("$type:$payload")
            true
        } catch (t: Throwable) {
            Timber.tag(TAG).e(t, "send failed type=%s", type)
            false
        }
    }

    companion object {
        private const val TAG = "AyaBridge"
        const val GAMEWINDOW_PKG = "com.ayaneo.gamewindow"
        const val AIDL_SERVICE_CLS = "com.ayaneo.gamewindow.utils.aidl.AyaAidlService"

        const val MSG_TYPE_REGISTER = "msg_type_register"
        const val MSG_TYPE_PERFORMANCE = "msg_type_performance"
        const val MSG_TYPE_FAN = "msg_type_fan"
        const val MSG_TYPE_RGB = "msg_type_rgb"
        const val MSG_TYPE_CONTROLLER = "msg_type_controller"
        const val MSG_TYPE_DEVICE = "msg_type_device"
        const val MSG_TYPE_SYSTEM = "msg_type_system"
        const val MSG_TYPE_CLOSE_WIFI = "msg_type_close_wifi"
    }
}
