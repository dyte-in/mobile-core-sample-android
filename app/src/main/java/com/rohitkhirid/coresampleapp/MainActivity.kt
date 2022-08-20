package com.rohitkhirid.coresampleapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.MeetingParticipantJoined
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.MeetingParticipantLeft
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.MeetingRecordedEnded
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.MeetingRecordedStarted
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.MeetingStateFailed
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.MeetingStateJoined
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.MeetingStateLeft
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.MeetingStateLoading
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.OnAudioUpdated
import com.rohitkhirid.coresampleapp.R.drawable
import com.rohitkhirid.coresampleapp.databinding.ActivityMainBinding
import io.dyte.core.DyteAndroidClientBuilder
import io.dyte.core.Utils
import io.dyte.core.controllers.DyteRecordingState.RECORDING

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
      try {
        if (meeting.self.audioEnabled) {
          meeting.self.disableAudio()
        } else {
          meeting.self.enableAudio()
        }
      } catch (e:Exception) {
        e.printStackTrace()
      }
    }

    binding.ivLeaveCall.setOnClickListener {
      meeting.leaveRoom()
    }

    binding.ivRecord.setOnClickListener {
      if(meeting.recording.recordingState == RECORDING) {
        meeting.recording.stop()
      } else {
        meeting.recording.start()
      }
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
          binding.nameInitials1.text = initials
          binding.nameTv1.text = meeting.self.name
        }

        MeetingStateLeft -> {
          println("DyteMobileClient | MainActivity onCreate Meeting state left")
          startActivity(Intent(this@MainActivity, CallLeftActivity::class.java))
          finishAffinity()
        }

        is MeetingParticipantJoined -> {
          println("DyteMobileClient | MainActivity onCreate MeetingParticipantJoined ${state.participant.name}")
          binding.llView2.visibility = View.VISIBLE
          val initials = Utils.getInitialsFromName(state.participant.name)
          binding.nameInitials2.text = initials
          binding.nameTv2.text = state.participant.name
        }

        is MeetingParticipantLeft -> {
          binding.llView2.visibility = View.GONE
        }

        is OnAudioUpdated -> {
          val audioDrawable = if (state.isEnabled) {
            drawable.ic_baseline_mic_24
          } else {
            drawable.ic_baseline_mic_off_24
          }
          binding.ivMic.setImageResource(audioDrawable)
        }
        MeetingRecordedStarted -> {
          binding.ivRecord.setColorFilter(
            ContextCompat.getColor(
              this,
              android.R.color.holo_red_dark
            ), android.graphics.PorterDuff.Mode.SRC_IN
          )
        }
        MeetingRecordedEnded -> {
          binding.ivRecord.setColorFilter(
            ContextCompat.getColor(
              this,
              android.R.color.white
            ), android.graphics.PorterDuff.Mode.SRC_IN
          )
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