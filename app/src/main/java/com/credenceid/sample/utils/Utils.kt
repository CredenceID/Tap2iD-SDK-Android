package com.credenceid.sample.utils

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Utils {

    fun millisToDateString(dateInMillis: Long?): String {
        return dateInMillis?.let {
            val instant = Instant.ofEpochMilli(it)
            val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            return localDate.format(formatter)
        } ?: ""
    }

    @SuppressLint("HardwareIds")
    fun getAndroidId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

}
