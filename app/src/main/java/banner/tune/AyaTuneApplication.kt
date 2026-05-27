package banner.tune

import android.app.Application
import com.topjohnwu.superuser.Shell
import timber.log.Timber

class AyaTuneApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Shell.enableVerboseLogging = true
        }

        // Set the default libsu shell config. Do NOT eagerly request root
        // here — that would prompt every user at first launch. We request
        // root lazily when the deep-tuning pane is opened for the first
        // time (see core/sys/Privilege.kt#requestRoot).
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER or Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(5)
        )
    }
}
