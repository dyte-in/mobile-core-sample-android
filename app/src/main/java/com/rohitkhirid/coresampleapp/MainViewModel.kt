package com.rohitkhirid.coresampleapp

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.MeetingParticipantJoined
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.MeetingStateFailed
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.MeetingStateJoined
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.MeetingStateLeft
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.MeetingStateLoading
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.OnAudioUpdated
import io.dyte.core.DyteMobileClient
import io.dyte.core.listeners.DyteMeetingRoomEventsListener
import io.dyte.core.listeners.DyteParticipantEventsListener
import io.dyte.core.listeners.DyteSelfEventsListener
import io.dyte.core.models.DyteMeetingInfo
import io.dyte.core.models.DyteMeetingParticipant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
  sealed class MeetingRoomState {
    object MeetingStateLoading : MeetingRoomState()
    object MeetingStateJoined : MeetingRoomState()
    object MeetingStateFailed : MeetingRoomState()
    object MeetingStateLeft : MeetingRoomState()
    class MeetingParticipantJoined(val participant: DyteMeetingParticipant) : MeetingRoomState()
    class OnAudioUpdated(val isEnabled: Boolean) : MeetingRoomState()
  }

  private val meetingInfo = DyteMeetingInfo(
    orgId = ORGNIZATION_ID,
    roomName = MEETING_ROOM_NAME,
    authToken = AUTH_TOKEN,
    enableAudio = true,
    enableVideo = false
  )

  val meetingStateLiveData = MutableLiveData<MeetingRoomState>()

  private lateinit var meeting: DyteMobileClient

  private val selfEventsListener = object : DyteSelfEventsListener {
    override fun onAudioUpdate(audioEnabled: Boolean) {
      super.onAudioUpdate(audioEnabled)
      meetingStateLiveData.value = OnAudioUpdated(audioEnabled)
    }
  }

  private val participantEventListener = object : DyteParticipantEventsListener {
    override fun onParticipantJoin(participant: DyteMeetingParticipant) {
      super.onParticipantJoin(participant)
      meetingStateLiveData.value = MeetingParticipantJoined(participant)
    }
  }

  private val meetingRoomEventsListner = object : DyteMeetingRoomEventsListener {
    override fun onMeetingInitStarted() {
      super.onMeetingInitStarted()
      println("DyteMobileClient | MainViewModel onMeetingInitStarted ")
      meetingStateLiveData.value = MeetingStateLoading
    }

    override fun onMeetingInitCompleted() {
      super.onMeetingInitCompleted()
      println("DyteMobileClient | MainViewModel onMeetingInitCompleted ")
      meeting.joinRoom()
    }

    override fun onMeetingInitFailed(exception: Exception) {
      super.onMeetingInitFailed(exception)
      meetingStateLiveData.value = MeetingStateFailed
    }

    override fun onMeetingRoomJoinStarted() {
      meetingStateLiveData.value = MeetingStateLoading
    }

    @SuppressLint("SetTextI18n")
    override fun onMeetingRoomJoined(meetingStartedAt: String) {
      viewModelScope.launch(Dispatchers.Main) {
        meetingStateLiveData.value = MeetingStateJoined
      }
    }

    override fun onMeetingRoomLeft() {
      meetingStateLiveData.value = MeetingStateLeft
    }

    @SuppressLint("SetTextI18n")
    override fun onMeetingRoomJoinFailed(exception: Exception) {
      meetingStateLiveData.value = MeetingStateFailed
    }
  }

  fun start(meeting: DyteMobileClient) {
    this.meeting = meeting
    meeting.addMeetingRoomEventsListener(meetingRoomEventsListner)
    meeting.addSelfEventsListener(selfEventsListener)
    meeting.addParticipantEventsListener(participantEventListener)
    meeting.init(meetingInfo)
  }

  override fun onCleared() {
    super.onCleared()
    meeting.removeMeetingRoomEventsListener(meetingRoomEventsListner)
    meeting.removeSelfEventsListener(selfEventsListener)
    meeting.removeParticipantEventsListener(participantEventListener)
  }
}