package com.shieldcheck.app.util

import android.content.Context
import android.content.SharedPreferences
import android.telephony.TelephonyManager
import androidx.core.content.getSystemService
import java.util.UUID

object DeviceIdentifier {

    private const val PREFS_NAME = "shield_check_prefs"
    private const val IMEI_KEY = "device_imei"

    /**
     * Récupère l'IMEI du téléphone ou génère un UUID de secours
     */
    fun getDeviceIdentifier(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Vérifier si un identifiant est déjà stocké
        var imei = prefs.getString(IMEI_KEY, "")
        if (!imei.isNullOrEmpty()) {
            return imei
        }

        // Tentative de récupération via TelephonyManager
        try {
            val telephonyManager = context.getSystemService<TelephonyManager>()
            if (telephonyManager != null && telephonyManager.imei != null) {
                imei = telephonyManager.imei
                saveIMEI(context, imei)
                return imei
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Mode de secours : générer un UUID
        imei = UUID.randomUUID().toString()
        saveIMEI(context, imei)
        return imei
    }

    /**
     * Sauvegarde l'IMEI dans SharedPreferences
     */
    fun saveIMEI(context: Context, imei: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(IMEI_KEY, imei).apply()
    }

    /**
     * Récupère l'IMEI stocké
     */
    fun getStoredIMEI(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(IMEI_KEY, null)
    }

    /**
     * Vérifie si l'IMEI a été défini manuellement
     */
    fun isIMEIManuallySet(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("imei_manually_set", false)
    }

    /**
     * Marque l'IMEI comme défini manuellement
     */
    fun markIMEIAsManuallySet(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("imei_manually_set", true).apply()
    }
}