package com.shieldcheck.app.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class DeviceAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "DeviceAdminReceiver"
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "✓ Device Admin activé avec succès")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "✗ Device Admin désactivé")
    }

    override fun onPasswordChanged(context: Context, intent: Intent) {
        super.onPasswordChanged(context, intent)
        Log.d(TAG, "Mot de passe système modifié")
    }

    override fun onLockTaskModeEntering(context: Context, intent: Intent, pkg: String) {
        super.onLockTaskModeEntering(context, intent, pkg)
        Log.d(TAG, "Mode verrou tâche entré: $pkg")
    }

    override fun onLockTaskModeExiting(context: Context, intent: Intent) {
        super.onLockTaskModeExiting(context, intent)
        Log.d(TAG, "Mode verrou tâche quitté")
    }
}