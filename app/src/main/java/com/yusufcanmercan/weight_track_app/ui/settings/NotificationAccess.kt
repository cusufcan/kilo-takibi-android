package com.yusufcanmercan.weight_track_app.ui.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.yusufcanmercan.weight_track_app.R
import com.yusufcanmercan.weight_track_app.platform.WeeklyNotifications
import com.yusufcanmercan.weight_track_app.ui.theme.Spacing

/**
 * Bir bildirim anahtarının izin akışı: açarken izin yoksa ister, reddedilirse [denied] olur.
 * [allowed] her ekrana dönüşte yenilenir; kullanıcı izni sistem ayarından sonradan kapatabilir.
 */
@Stable
internal class NotificationAccess {
    var allowed by mutableStateOf(true)
        internal set
    var denied by mutableStateOf(false)
        internal set
    internal var request: (onGranted: () -> Unit) -> Unit = {}

    /** Açmak isteniyorsa ve izin eksikse önce izin ister; kapatmak her zaman serbest. */
    fun change(enabled: Boolean, apply: (Boolean) -> Unit) {
        denied = false
        if (enabled) request { apply(true) } else apply(false)
    }
}

@Composable
internal fun rememberNotificationAccess(): NotificationAccess {
    val context = LocalContext.current
    val access = remember { NotificationAccess() }
    var onGranted by remember { mutableStateOf<() -> Unit>({}) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        access.denied = !granted
        access.allowed = WeeklyNotifications.notificationsAllowed(context)
        if (granted) onGranted()
    }
    access.request = { granted ->
        if (Build.VERSION.SDK_INT >= 33 && !WeeklyNotifications.hasPermission(context)) {
            onGranted = granted
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            granted()
        }
    }
    LifecycleResumeEffect(Unit) {
        access.allowed = WeeklyNotifications.notificationsAllowed(context)
        onPauseOrDispose {}
    }
    return access
}

/** Hatırlatıcı açık ama bildirimler engelli: neden gelmeyeceğini söyler ve ayara götürür. */
@Composable
internal fun NotificationsBlockedWarning(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            stringResource(R.string.notifications_blocked),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        TextButton(onClick = { openNotificationSettings(context) }) {
            Text(stringResource(R.string.notifications_open_settings))
        }
    }
}

private fun openNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= 26) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    }
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // Ayar ekranı olmayan nadir cihazlarda uyarı metni yeterli.
    }
}
