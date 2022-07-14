package com.rohitkhirid.coresampleapp

import android.app.Application
import com.dyte.mobilecorekmm.DyteAndroidClientBuilder
import com.dyte.mobilecorekmm.DyteMobileClient

class SampleApp: Application() {
  lateinit var dyteClient: DyteMobileClient

  override fun onCreate() {
    super.onCreate()
    dyteClient = DyteAndroidClientBuilder.build(this)
  }
}