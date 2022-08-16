package com.rohitkhirid.coresampleapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.MeetingStateFailed
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.MeetingStateJoined
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.MeetingStateLeft
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.MeetingStateLoading
import com.rohitkhirid.coresampleapp.databinding.ActivityMainBinding
import io.dyte.core.DyteAndroidClientBuilder
import io.dyte.core.controllers.PageViewMode.GRID
import io.dyte.core.controllers.PageViewMode.PAGINATED
import io.dyte.core.listeners.DyteParticipantEventsListener
import io.dyte.core.listeners.DyteSelfEventsListener
import io.dyte.core.media.VideoView
import io.dyte.core.models.DyteMeetingParticipant
import io.dyte.core.models.DyteRoomParticipants

class MainActivity : AppCompatActivity() {
  private lateinit var binding: ActivityMainBinding

  private val participantsToViews = hashMapOf<String, VideoView>()

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
      updateParticipant(meeting.self)
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
      updateParticipant(meeting.self)

      meeting.participants.setMode(PAGINATED)
      meeting.participants.joined.first().disableVideo()
    }
  }

  private val participantEventsListener = object : DyteParticipantEventsListener {
    override fun audioUpdate(
      audioEnabled: Boolean,
      participant: DyteMeetingParticipant
    ) {
      super.audioUpdate(audioEnabled, participant)
      updateParticipant(participant)
    }

    override fun videoUpdate(
      videoEnabled: Boolean,
      participant: DyteMeetingParticipant
    ) {
      super.videoUpdate(videoEnabled, participant)
      updateParticipant(participant)
    }

    override fun onParticipantsUpdated(
      participants: DyteRoomParticipants,
      isNextPagePossible: Boolean,
      isPreviousPagePossible: Boolean
    ) {
      super.onParticipantsUpdated(participants, isNextPagePossible, isPreviousPagePossible)
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

    override fun onScreenSharesUpdated() {
      super.onScreenSharesUpdated()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
    setContentView(binding.root)

    viewModel = ViewModelProvider(this)[MainViewModel::class.java]

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
          updateParticipant(meeting.self)
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
    meeting.removeParticipantEventsListener(participantEventsListener)
  }

  private fun updateParticipant(participant: DyteMeetingParticipant) {
    println("DyteMobileClient | MainActivity updateParticipant ${participant.name}")
    val view = participantsToViews[participant.id]
    view?.render(participant, meeting)
  }

  private fun refreshGrid(activeParticipants: List<DyteMeetingParticipant>) {
    when (activeParticipants.size) {
      1 -> {
        val p1 = activeParticipants[0]
        val videoView1 = participantsToViews[p1.id]
        binding.llView1.removeAllViews()
        videoView1?.let {
          removeFromParent(videoView1)
          binding.llView1.addView(videoView1)
          videoView1.render(p1, meeting, false, p1.screenShareTrack != null)
        }


        binding.llView2.removeAllViews()
        binding.llView3.removeAllViews()
        binding.llView4.removeAllViews()
        binding.llView5.removeAllViews()
        binding.llView6.removeAllViews()

        binding.ll1.visibility = View.VISIBLE
        // binding.ll2.visibility = View.GONE
        // binding.ll3.visibility = View.GONE
      }

      2 -> {
        binding.llView3.removeAllViews()
        binding.llView4.removeAllViews()
        binding.llView5.removeAllViews()
        binding.llView6.removeAllViews()

        val p1 = activeParticipants[0]
        val videoView1 = participantsToViews[p1.id]
        binding.llView1.removeAllViews()
        videoView1?.let {
          removeFromParent(videoView1)
          binding.llView1.addView(videoView1)
          videoView1.render(p1, meeting, false, p1.screenShareTrack != null)
        }

        val p2 = activeParticipants[1]
        val videoView2 = participantsToViews[p2.id]
        binding.llView2.removeAllViews()
        videoView2?.let {
          removeFromParent(videoView2)
          binding.llView2.addView(videoView2)
          videoView2.render(p2, meeting, false, p2.screenShareTrack != null)
        }

        binding.ll1.visibility = View.VISIBLE
        // binding.ll2.visibility = View.GONE
        // binding.ll3.visibility = View.GONE
      }

      3 -> {
        binding.llView4.removeAllViews()
        binding.llView5.removeAllViews()
        binding.llView6.removeAllViews()

        val p1 = activeParticipants[0]
        val videoView1 = participantsToViews[p1.id]
        binding.llView1.removeAllViews()
        videoView1?.let {
          removeFromParent(videoView1)
          binding.llView1.addView(videoView1)
          videoView1.render(p1, meeting, false, p1.screenShareTrack != null)
        }

        val p2 = activeParticipants[1]
        val videoView2 = participantsToViews[p2.id]
        binding.llView2.removeAllViews()
        videoView2?.let {
          removeFromParent(videoView2)
          binding.llView2.addView(videoView2)
          videoView2.render(p2, meeting, false, p2.screenShareTrack != null)
        }

        val p3 = activeParticipants[2]
        val videoView3 = participantsToViews[p3.id]
        binding.llView3.removeAllViews()
        videoView3?.let {
          removeFromParent(videoView3)
          binding.llView3.addView(videoView3)
          videoView3.render(p3, meeting, false, p3.screenShareTrack != null)
        }

        binding.ll1.visibility = View.VISIBLE
        binding.ll2.visibility = View.VISIBLE
        binding.ll3.visibility = View.GONE
      }

      4 -> {
        binding.llView5.removeAllViews()
        binding.llView6.removeAllViews()

        val p1 = activeParticipants[0]
        val videoView1 = participantsToViews[p1.id]
        binding.llView1.removeAllViews()
        videoView1?.let {
          removeFromParent(videoView1)
          binding.llView1.addView(videoView1)
          videoView1.render(p1, meeting, false, p1.screenShareTrack != null)
        }

        val p2 = activeParticipants[1]
        val videoView2 = participantsToViews[p2.id]
        binding.llView2.removeAllViews()
        videoView2?.let {
          removeFromParent(videoView2)
          binding.llView2.addView(videoView2)
          videoView2.render(p2, meeting, false, p2.screenShareTrack != null)
        }

        val p3 = activeParticipants[2]
        val videoView3 = participantsToViews[p3.id]
        binding.llView3.removeAllViews()
        videoView3?.let {
          removeFromParent(videoView3)
          binding.llView3.addView(videoView3)
          videoView3.render(p3, meeting, false, p3.screenShareTrack != null)
        }

        val p4 = activeParticipants[3]
        val videoView4 = participantsToViews[p4.id]
        binding.llView4.removeAllViews()
        videoView4?.let {
          removeFromParent(videoView4)
          binding.llView4.addView(videoView4)
          videoView4.render(p4, meeting, false, p4.screenShareTrack != null)
        }

        binding.ll1.visibility = View.VISIBLE
        binding.ll2.visibility = View.VISIBLE
        binding.ll3.visibility = View.GONE
      }

      5 -> {
        binding.llView6.removeAllViews()

        val p1 = activeParticipants[0]
        val videoView1 = participantsToViews[p1.id]
        binding.llView1.removeAllViews()
        videoView1?.let {
          removeFromParent(videoView1)
          binding.llView1.addView(videoView1)
          videoView1.render(p1, meeting, false, p1.screenShareTrack != null)
        }

        val p2 = activeParticipants[1]
        val videoView2 = participantsToViews[p2.id]
        binding.llView2.removeAllViews()
        videoView2?.let {
          removeFromParent(videoView2)
          binding.llView2.addView(videoView2)
          videoView2.render(p2, meeting, false, p2.screenShareTrack != null)
        }

        val p3 = activeParticipants[2]
        val videoView3 = participantsToViews[p3.id]
        binding.llView3.removeAllViews()
        videoView3?.let {
          removeFromParent(videoView3)
          binding.llView3.addView(videoView3)
          videoView3.render(p3, meeting, false, p3.screenShareTrack != null)
        }

        val p4 = activeParticipants[3]
        val videoView4 = participantsToViews[p4.id]
        binding.llView4.removeAllViews()
        videoView4?.let {
          removeFromParent(videoView4)
          binding.llView4.addView(videoView4)
          videoView4.render(p4, meeting, false, p4.screenShareTrack != null)
        }

        val p5 = activeParticipants[4]
        val videoView5 = participantsToViews[p5.id]
        binding.llView5.removeAllViews()
        videoView5?.let {
          removeFromParent(videoView5)
          binding.llView5.addView(videoView5)
          videoView5.render(p5, meeting, false, p5.screenShareTrack != null)
        }

        binding.ll1.visibility = View.VISIBLE
        binding.ll2.visibility = View.VISIBLE
        binding.ll3.visibility = View.VISIBLE
      }

      6 -> {
        val p1 = activeParticipants[0]
        val videoView1 = participantsToViews[p1.id]
        binding.llView1.removeAllViews()
        videoView1?.let {
          removeFromParent(videoView1)
          binding.llView1.addView(videoView1)
          videoView1.render(p1, meeting, false, p1.screenShareTrack != null)
        }

        val p2 = activeParticipants[1]
        val videoView2 = participantsToViews[p2.id]
        binding.llView2.removeAllViews()
        videoView2?.let {
          removeFromParent(videoView2)
          binding.llView2.addView(videoView2)
          videoView2.render(p2, meeting, false, p2.screenShareTrack != null)
        }

        val p3 = activeParticipants[2]
        val videoView3 = participantsToViews[p3.id]
        binding.llView3.removeAllViews()
        videoView3?.let {
          removeFromParent(videoView3)
          binding.llView3.addView(videoView3)
          videoView3.render(p3, meeting, false, p3.screenShareTrack != null)
        }

        val p4 = activeParticipants[3]
        val videoView4 = participantsToViews[p4.id]
        binding.llView4.removeAllViews()
        videoView4?.let {
          removeFromParent(videoView4)
          binding.llView4.addView(videoView4)
          videoView4.render(p4, meeting, false, p4.screenShareTrack != null)
        }

        val p5 = activeParticipants[4]
        val videoView5 = participantsToViews[p5.id]
        binding.llView5.removeAllViews()
        videoView5?.let {
          removeFromParent(videoView5)
          binding.llView5.addView(videoView5)
          videoView5.render(p5, meeting, false, p5.screenShareTrack != null)
        }

        val p6 = activeParticipants[5]
        val videoView6 = participantsToViews[p6.id]
        binding.llView6.removeAllViews()
        videoView6?.let {
          removeFromParent(videoView6)
          binding.llView6.addView(videoView6)
          videoView6.render(p6, meeting, false, p6.screenShareTrack != null)
        }

        binding.ll1.visibility = View.VISIBLE
        binding.ll2.visibility = View.VISIBLE
        binding.ll3.visibility = View.VISIBLE
      }
    }
  }

  private fun removeFromParent(child: View) {
    if (child.parent != null) {
      (child.parent as ViewGroup).removeView(child) // <- fix
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