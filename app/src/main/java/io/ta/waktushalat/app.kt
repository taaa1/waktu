package io.ta.waktushalat

import android.app.Application
import com.google.android.material.color.DynamicColors

class waktushalat : Application() {
    override fun onCreate() {
        super.onCreate()

        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}