package com.rohitkhirid.coresampleapp

import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.dyte.mobilecorekmm.Utils.printLog
import com.dyte.mobilecorekmm.listeners.DyteMeetingRoomEventsListener
import com.dyte.mobilecorekmm.listeners.DyteParticipantEventsListener
import com.dyte.mobilecorekmm.listeners.DyteSelfEventsListener
import com.dyte.mobilecorekmm.media.view.VideoView
import com.dyte.mobilecorekmm.models.DyteMeetingInfo
import com.dyte.mobilecorekmm.models.DyteMeetingParticipant
import com.dyte.mobilecorekmm.models.DyteRoomParticipants
import com.dyte.mobilecorekmm.permission.OnPermissionChangeListener
import com.dyte.mobilecorekmm.permission.Permission.PERMISSION_CAMERA
import com.dyte.mobilecorekmm.permission.Permission.PERMISSION_MICROPHONE
import com.dyte.mobilecorekmm.permission.Permission.PERMISSION_STORAGE
import com.dyte.mobilecorekmm.permission.PermissionManager
import com.dyte.mobilecorekmm.permission.PermissionManagerHost
import com.rohitkhirid.coresampleapp.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), PermissionManagerHost, OnPermissionChangeListener {
  private lateinit var binding: ActivityMainBinding
  private lateinit var permissionManager: PermissionManager

  private val job = Job()
  private val uiScope = CoroutineScope(Dispatchers.Main + job)

  private val participantsToViews = hashMapOf<String, VideoView>()

  private var isAudioEnabled = true
  private var isVideoEnabled = true

  private val dyteClient by lazy {
    (application as SampleApp).dyteClient
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
  }

  override fun onPermissionDenied() {
    showPermissionError("Permissions are denied, You need to allow permission to be able to join in a meeting")
  }

  override fun onPermissionDeniedAlways() {
    showPermissionError("Permissions are denied, You need to allow permission to be able to join in a meeting")
  }

  override fun onPermissionGranted() {
    val meetingInfo = DyteMeetingInfo(
      orgId = "60709d2a-c83e-477a-8199-f00ff680c44d",
      baseUrl = "https://api.staging.dyte.in",
      roomName = "jzkmpb-unvpzs",
      authToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjcyNDRiODdlLWY1NDktNGNlYS1hZWI4LWNjZGRhNGRjNzY2NyIsImxvZ2dlZEluIjp0cnVlLCJpYXQiOjE2NTc3MDgxMjcsImV4cCI6MTY2NjM0ODEyN30.hmbKby92cmeFlYpUwiOlVdghvT-oON_B-CJ3zKDO2R5b1tkoD7PFSukJTNcOapIs0ci_0bpO8j_JujN3S6qiIFL70WrgupD7OckL4BKDbbHKzmgtH8eSlVwYumt0ZgxqlnwzrGxDV-gFIweWRPmPpCNLFr1Df5Qso-H8FhdDwMA",
      displayName = null,
      enableAudio = isAudioEnabled,
      enableVideo = isVideoEnabled
    )

    uiScope.launch(Dispatchers.IO) {
      runBlocking {
        withContext(Dispatchers.Main) {
          showLoader()
        }
        withContext(Dispatchers.IO) {
          dyteClient.init(meetingInfo)
          dyteClient.joinRoom()
        }
      }
    }
  }

  override fun requestPermission(permissions: List<String>) {
    if (VERSION.SDK_INT >= VERSION_CODES.M) {
      requestPermissions(permissions.toTypedArray(), 111)
    }
  }

  override fun shouldShowPermissionRational(permission: String): Boolean {
    return if (VERSION.SDK_INT >= VERSION_CODES.M) {
      shouldShowRequestPermissionRationale(permission)
    } else {
      return false
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
    setContentView(binding.root)

    dyteClient.addMeetingRoomEventsListener(meetingRoomEventListener)
    dyteClient.addSelfEventsListener(selfEventsListener)
    dyteClient.addParticipantEventsListener(participantEventsListener)

    binding.ivCamera.setOnClickListener {
      if (isVideoEnabled) {
        dyteClient.getSelf().disableVideo()
      } else {
        dyteClient.getSelf().enableVideo()
      }
    }

    binding.ivMic.setOnClickListener {
      if (isAudioEnabled) {
        dyteClient.getSelf().disableAudio()
      } else {
        dyteClient.getSelf().enableAudio()
      }
    }

    binding.ivSwitchCamera.setOnClickListener {
      dyteClient.getSelf().switchCamera()
    }

    binding.ivLeaveCall.setOnClickListener {
      dyteClient.leaveRoom()
    }

    permissionManager = PermissionManager(
      this,
      arrayListOf(PERMISSION_STORAGE, PERMISSION_MICROPHONE, PERMISSION_CAMERA),
      this
    )
    permissionManager.askPermissions()
  }

  override fun onDestroy() {
    super.onDestroy()

    job.cancel()
    uiScope.cancel()
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    permissionManager.processPermissions(grantResults)
  }

  private fun refreshGrid(activeParticipants: List<DyteMeetingParticipant>) {
    binding.llViewContainer.removeAllViews()
    activeParticipants.forEach { dyteMeetingParticipant ->
      val videoView = participantsToViews[dyteMeetingParticipant.id]
      videoView?.let {
        binding.llViewContainer.addView(videoView)
        videoView.render(dyteMeetingParticipant)
      }
    }
  }

  private fun updateParticipant(participant: DyteMeetingParticipant) {
    val view = participantsToViews[participant.id]
    view?.render(participant) ?: "view not found".printLog()
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
  private fun showPermissionError(msg: String) {
    binding.clDataContainer.visibility = View.GONE
    binding.clLoaderContainer.visibility = View.GONE
    binding.clErrorContainer.visibility = View.VISIBLE
    binding.tvError.text = msg
    binding.btnRetry.setOnClickListener {
      openAppSettings()
    }
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