package com.example.otoservice.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class UserApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?
)

object AppFilterHelper {
    
    fun getFilteredUserApps(context: Context): List<UserApp> {
        val packageManager = context.packageManager
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        val filteredApps = installedApps
            .filter { appInfo ->
                isUserInstalledApp(appInfo) &&
                hasLauncherActivity(packageManager, appInfo.packageName)
            }
            .map { appInfo ->
                UserApp(
                    packageName = appInfo.packageName,
                    label = appInfo.loadLabel(packageManager).toString(),
                    icon = appInfo.loadIcon(packageManager)
                )
            }
            .sortedBy { it.label.lowercase() }
        
        return filteredApps
    }
    
    private fun isUserInstalledApp(appInfo: ApplicationInfo): Boolean {
        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        return !isSystemApp || isUpdatedSystemApp
    }
    
    private fun hasLauncherActivity(packageManager: PackageManager, packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(packageName)
        }
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)
        return resolveInfos.isNotEmpty()
    }
}
