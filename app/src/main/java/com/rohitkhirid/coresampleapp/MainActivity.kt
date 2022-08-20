package com.rohitkhirid.coresampleapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.MeetingParticipantJoined
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.MeetingStateFailed
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.MeetingStateJoined
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.MeetingStateLeft
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.MeetingStateLoading
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.OnAudioUpdated
import com.rohitkhirid.coresampleapp.databinding.ActivityMainBinding
import io.dyte.core.DyteAndroidClientBuilder
import io.dyte.core.Utils

class MainActivity : AppCompatActivity() {
  private lateinit var binding: ActivityMainBinding

  private lateinit var viewModel: MainViewModel

  private val meeting by lazy {
    DyteAndroidClientBuilder.build(this)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
    setContentView(binding.root)

    viewModel = ViewModelProvider(this)[MainViewModel::class.java]

    binding.ivMic.setOnClickListener {
      if (meeting.self.audioEnabled) {
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
          val initials = Utils.getInitialsFromName(meeting.self.name)
          binding.nameTv1.text = initials
        }
        MeetingStateLeft -> {
          println("DyteMobileClient | MainActivity onCreate Meeting state left")
          startActivity(Intent(this@MainActivity, CallLeftActivity::class.java))
          finishAffinity()
        }

        is MeetingParticipantJoined -> {
          println("DyteMobileClient | MainActivity onCreate MeetingParticipantJoined ${state.participant.name}")
          val initials = Utils.getInitialsFromName(state.participant.name)
          binding.nameTv2.text = initials
        }

        is OnAudioUpdated -> {
          val audioDrawable = if (state.isEnabled) {
            R.drawable.ic_baseline_mic_24
          } else {
            R.drawable.ic_baseline_mic_off_24
          }
          binding.ivMic.setImageResource(audioDrawable)
        }
      }
    }
    viewModel.start(meeting)
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