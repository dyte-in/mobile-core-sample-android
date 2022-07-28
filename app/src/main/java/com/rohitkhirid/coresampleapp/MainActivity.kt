package com.rohitkhirid.coresampleapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.dyte.core.DyteAndroidClientBuilder
import com.dyte.core.listeners.DyteMeetingRoomEventsListener
import com.dyte.core.listeners.DyteParticipantEventsListener
import com.dyte.core.listeners.DyteSelfEventsListener
import com.dyte.core.media.VideoView
import com.dyte.core.models.DyteMeetingInfo
import com.dyte.core.models.DyteMeetingParticipant
import com.dyte.core.models.DyteRoomParticipants
import com.rohitkhirid.coresampleapp.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
  private lateinit var binding: ActivityMainBinding

  private val job = Job()
  private val uiScope = CoroutineScope(Dispatchers.Main + job)

  private val participantsToViews = hashMapOf<String, VideoView>()

  private var isAudioEnabled = true
  private var isVideoEnabled = true

  private val meeting by lazy {
    DyteAndroidClientBuilder.build(this)
  }
  
  private val meetingRoomEventListener = object : DyteMeetingRoomEventsListener {
    override fun onMeetingRoomJoinStarted() {
      super.onMeetingRoomJoinStarted()
      showLoader()
    }

    override fun onMeetingRoomJoinFailed(exception: Exception) {
      super.onMeetingRoomJoinFailed(exception)
      hideLoader()
      showMeetingJoiningError(exception.localizedMessage)
    }

    override fun onMeetingRoomJoined(meetingStartedAt: String) {
      super.onMeetingRoomJoined(meetingStartedAt)
      hideLoader()
    }

    override fun onMeetingRoomLeft() {
      super.onMeetingRoomLeft()
      startActivity(Intent(this@MainActivity, CallLeftActivity::class.java))
      finishAffinity()
    }
  }

  private val selfEventsListener = object : DyteSelfEventsListener {
    override fun onAudioUpdate(audioEnabled: Boolean) {
      super.onAudioUpdate(audioEnabled)
      isAudioEnabled = audioEnabled
      val audioDrawable = if(audioEnabled) {
        R.drawable.ic_baseline_mic_24
      } else {
        R.drawable.ic_baseline_mic_off_24
      }
      binding.ivMic.setImageResource(audioDrawable)
    }

    override fun onVideoUpdate(videoEnabled: Boolean) {
      super.onVideoUpdate(videoEnabled)
      isVideoEnabled = videoEnabled
      val videoDrawable = if(videoEnabled) {
        R.drawable.ic_baseline_videocam_24
      } else {
        R.drawable.ic_baseline_videocam_off_24
      }
      binding.ivCamera.setImageResource(videoDrawable)
    }
  }

  private val participantEventsListener = object : DyteParticipantEventsListener {
    override fun audioUpdate(audioEnabled: Boolean, participant: DyteMeetingParticipant) {
      super.audioUpdate(audioEnabled, participant)
      updateParticipant(participant)
    }

    override fun videoUpdate(videoEnabled: Boolean, participant: DyteMeetingParticipant) {
      super.videoUpdate(videoEnabled, participant)
      updateParticipant(participant)
    }

    override fun onParticipantsUpdated(
      participants: DyteRoomParticipants,
      enabledPaginator: Boolean
    ) {
      super.onParticipantsUpdated(participants, enabledPaginator)
      refreshGrid(participants.active)
    }

    override fun onParticipantUpdated(participant: DyteMeetingParticipant) {
      super.onParticipantUpdated(participant)
      updateParticipant(participant)
    }

    override fun onParticipantJoin(participant: DyteMeetingParticipant) {
      super.onParticipantJoin(participant)
      val videoView = VideoView(this@MainActivity)
      participantsToViews[participant.id] = videoView
    }
  }


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
    setContentView(binding.root)

    meeting.addMeetingRoomEventsListener(meetingRoomEventListener)
    meeting.addSelfEventsListener(selfEventsListener)
    meeting.addParticipantEventsListener(participantEventsListener)

    binding.ivCamera.setOnClickListener {
      if (isVideoEnabled) {
        meeting.self.disableVideo()
      } else {
        meeting.self.enableVideo()
      }
    }

    binding.ivMic.setOnClickListener {
      if (isAudioEnabled) {
        meeting.self.disableAudio()
      } else {
        meeting.self.enableAudio()
      }
    }

    binding.ivSwitchCamera.setOnClickListener {
      meeting.self.switchCamera()
    }

    binding.ivLeaveCall.setOnClickListener {
      meeting.leaveRoom()
    }

    val meetingInfo = DyteMeetingInfo(
      orgId = ORGNIZATION_ID,
      roomName = MEETING_ROOM_NAME,
      authToken = AUTH_TOKEN,
    )

    uiScope.launch(Dispatchers.IO) {
      runBlocking {
        withContext(Dispatchers.Main) {
          showLoader()
        }
        withContext(Dispatchers.IO) {
          meeting.init(meetingInfo)
          meeting.joinRoom()
        }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()

    job.cancel()
    uiScope.cancel()
  }
  
  private fun updateParticipant(participant: DyteMeetingParticipant) {
    val view = participantsToViews[participant.id]
    view?.render(participant, meeting)
  }

  private fun refreshGrid(activeParticipants: List<DyteMeetingParticipant>) {
    binding.llViewContainer.removeAllViews()
    activeParticipants.forEach { dyteMeetingParticipant ->
      val videoView = participantsToViews[dyteMeetingParticipant.id]
      videoView?.let {
        binding.llViewContainer.addView(videoView)
        videoView.render(dyteMeetingParticipant, meeting)
      }
    }
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