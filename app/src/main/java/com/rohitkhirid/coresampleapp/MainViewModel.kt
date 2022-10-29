package com.rohitkhirid.coresampleapp

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.MeetingStateFailed
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.MeetingStateJoined
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.MeetingStateLeft
import com.rohitkhirid.coresampleapp.MainViewModel.MeetingRoomState.MeetingStateLoading
import io.dyte.core.DyteMobileClient
import io.dyte.core.listeners.DyteMeetingRoomEventsListener
import io.dyte.core.listeners.DyteSelfEventsListener
import io.dyte.core.models.DyteMeetingInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
  sealed class MeetingRoomState {
    object MeetingStateLoading : MeetingRoomState()
    object MeetingStateJoined : MeetingRoomState()
    object MeetingStateFailed : MeetingRoomState()
    object MeetingStateLeft : MeetingRoomState()
  }

  private val meetingInfo = DyteMeetingInfo(
    orgId = ORGNIZATION_ID,
    roomName = MEETING_ROOM_NAME,
    authToken = AUTH_TOKEN,
  )

  val meetingStateLiveData = MutableLiveData<MeetingRoomState>()

  private lateinit var meeting: DyteMobileClient

  private val meetingRoomEventsListner = object : DyteMeetingRoomEventsListener {
    override fun onMeetingInitStarted() {
      super.onMeetingInitStarted()
      viewModelScope.launch(Dispatchers.Main) {
        meetingStateLiveData.value = MeetingStateLoading
      }
    }

    override fun onMeetingInitCompleted() {
      super.onMeetingInitCompleted()
      meeting.joinRoom()
    }

    override fun onMeetingInitFailed(exception: Exception) {
      super.onMeetingInitFailed(exception)
      viewModelScope.launch(Dispatchers.Main) {
        meetingStateLiveData.value = MeetingStateFailed
      }
    }

    override fun onMeetingRoomJoinStarted() {
      viewModelScope.launch(Dispatchers.Main) {
        meetingStateLiveData.value = MeetingStateLoading
      }
    }

    override fun onMeetingRoomLeft() {
      viewModelScope.launch(Dispatchers.Main) {
        meetingStateLiveData.value = MeetingStateLeft
      }
    }

    @SuppressLint("SetTextI18n")
    override fun onMeetingRoomJoinFailed(exception: Exception) {
      viewModelScope.launch(Dispatchers.Main) {
        meetingStateLiveData.value = MeetingStateFailed
      }
    }
  }

  private val selfEventListener = object : DyteSelfEventsListener {
    override fun onRoomJoined() {
      super.onRoomJoined()
      viewModelScope.launch(Dispatchers.Main) {
        meetingStateLiveData.value = MeetingStateJoined
      }
    }
  }

  fun start(meeting: DyteMobileClient) {
    this.meeting = meeting

    meeting.addMeetingRoomEventsListener(meetingRoomEventsListner)
    meeting.addSelfEventsListener(selfEventListener)
    meeting.init(meetingInfo)
  }

  override fun onCleared() {
    super.onCleared()
    meeting.removeMeetingRoomEventsListener(meetingRoomEventsListner)
    meeting.removeSelfEventsListener(selfEventListener)
  }
}