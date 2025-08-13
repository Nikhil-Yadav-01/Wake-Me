package com.nikhil.wakeme

import android.app.Application
import com.nikhil.wakeme.util.NotificationHelper

class WakeMeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannelIfNeeded(this)
    }
}
