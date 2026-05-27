package banner.tune.core.sys

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Root-grant gate for the deep-tuning pane.
 *
 * [hasRoot] is a cheap synchronous check — true once the user has tapped
 * Allow in Magisk/KernelSU. Use it to gate UI visibility.
 *
 * [requestRoot] is the on-demand prompt — only call from a user-initiated
 * action (e.g., a "Unlock deep tuning" button). It opens a shell, which
 * triggers the root daemon's prompt on first use; the grant is persistent
 * across app restarts.
 */
object Privilege {

    /** true = a root grant for this app exists right now. */
    val hasRoot: Boolean
        get() = Shell.isAppGrantedRoot() == true

    /**
     * Forces libsu to open a shell, triggering the root daemon's
     * permission prompt if no grant exists yet. Resumes with the result
     * (true = grant present after the attempt).
     */
    suspend fun requestRoot(): Boolean = suspendCancellableCoroutine { cont ->
        Shell.getShell { _ ->
            cont.resume(hasRoot)
        }
    }
}
