package com.rohitkhirid.coresampleapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.MeetingStateFailed
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.MeetingStateJoined
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.MeetingStateLeft
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.MeetingStateLoading
import com.rohitkhirid.coresampleapp.databinding.ActivityMainBinding
import io.dyte.core.DyteAndroidClientBuilder
import io.dyte.core.controllers.PageViewMode.PAGINATED
import io.dyte.core.listeners.DyteSelfEventsListener

class MainActivity : AppCompatActivity() {
  private lateinit var binding: ActivityMainBinding

  private var isAudioEnabled = true
  private var isVideoEnabled = true

  private lateinit var viewModel: MainViewModel

  private val meeting by lazy {
    DyteAndroidClientBuilder.build(this)
  }

  private val selfEventsListener = object : DyteSelfEventsListener {
    override fun onAudioUpdate(audioEnabled: Boolean) {
      super.onAudioUpdate(audioEnabled)
      isAudioEnabled = audioEnabled
      val audioDrawable = if (audioEnabled) {
        R.drawable.ic_baseline_mic_24
      } else {
        R.drawable.ic_baseline_mic_off_24
      }
      binding.ivMic.setImageResource(audioDrawable)
    }

    override fun onVideoUpdate(videoEnabled: Boolean) {
      super.onVideoUpdate(videoEnabled)
      isVideoEnabled = videoEnabled
      val videoDrawable = if (videoEnabled) {
        R.drawable.ic_baseline_videocam_24
      } else {
        R.drawable.ic_baseline_videocam_off_24
      }
      binding.ivCamera.setImageResource(videoDrawable)

      meeting.participants.setMode(PAGINATED)
      meeting.participants.joined.first().disableVideo()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
    setContentView(binding.root)

    viewModel = ViewModelProvider(this)[MainViewModel::class.java]

    meeting.addSelfEventsListener(selfEventsListener)

    binding.ivMic.setOnClickListener {
      if (isAudioEnabled) {
        meeting.self.disableAudio()
      } else {
        meeting.self.enableAudio()
      }
    }

    binding.ivLeaveCall.setOnClickListener {
      meeting.leaveRoom()
    }

    viewModel.meetingStateLiveData.observe(this) { state ->
      when (state) {
        MeetingStateLoading -> {
          println("DyteMobileClient | MainActivity onCreate Meeting Loading state")
          showLoader()
        }
        MeetingStateFailed -> {
          showMeetingJoiningError("Its not you, its us! Something failed, Please try again later")
        }
        MeetingStateJoined -> {
          println("DyteMobileClient | MainActivity onCreate Meeting state joined")
          hideLoader()
        }
        MeetingStateLeft -> {
          println("DyteMobileClient | MainActivity onCreate Meeting state left")
          startActivity(Intent(this@MainActivity, CallLeftActivity::class.java))
          finishAffinity()
        }
      }
    }
    viewModel.start(meeting)
  }

  override fun onDestroy() {
    super.onDestroy()

    meeting.removeSelfEventsListener(selfEventsListener)
  }

  private fun showLoader() {
    binding.clDataContainer.visibility = View.GONE
    binding.clErrorContainer.visibility = View.GONE
    binding.clLoaderContainer.visibility = View.VISIBLE
  }

  private fun hideLoader() {
    binding.clErrorContainer.visibility = View.GONE
    binding.clLoaderContainer.visibility = View.GONE
    binding.clDataContainer.visibility = View.VISIBLE

    binding.clGridContainer.visibility = View.GONE
    binding.clAudioCallContainer.visibility = View.VISIBLE
  }

  @Suppress("SameParameterValue")
  private fun showMeetingJoiningError(msg: String) {
    binding.clDataContainer.visibility = View.GONE
    binding.clLoaderContainer.visibility = View.GONE
    binding.clErrorContainer.visibility = View.VISIBLE
    binding.tvError.text = msg
    binding.btnRetry.setOnClickListener {
      openAppSettings()
    }
  }

  private fun openAppSettings() {
    val intent = Intent().apply {
      action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
      data = Uri.fromParts("package", packageName, null)
    }
    startActivity(intent)
  }
}