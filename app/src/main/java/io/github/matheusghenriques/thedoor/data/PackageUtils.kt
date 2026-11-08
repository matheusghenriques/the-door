package io.github.matheusghenriques.thedoor.data

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

fun getLauncherPackages(pm: PackageManager): Set<String> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION") pm.queryIntentActivities(intent, 0)
    }
    return activities.mapNotNull { it.activityInfo?.packageName }.toSet()
}
