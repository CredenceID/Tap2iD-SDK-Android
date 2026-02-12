package com.credenceid.sample.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory

internal val Any.TAG: String
    get() {
        return if (!javaClass.isAnonymousClass) {
            val name = javaClass.simpleName
            if (name.length <= 23) {
                name
            } else {
                name.substring(0, 23) // first 23 chars
            }
        } else {
            val name = javaClass.name
            if (name.length <= 23) {
                name
            } else {
                name.substring(name.length - 23, name.length) // last 23 chars
            }
        }
    }
