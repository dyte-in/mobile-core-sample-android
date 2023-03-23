package io.dyte.coresampleapp

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.dyte.core.DyteMobileClient
import io.dyte.core.listeners.DyteMeetingRoomEventsListener
import io.dyte.core.models.DyteMeetingInfo
import io.dyte.coresampleapp.MainViewModel.MeetingRoomState.MeetingStateFailed
import io.dyte.coresampleapp.MainViewModel.MeetingRoomState.MeetingStateJoined
import io.dyte.coresampleapp.MainViewModel.MeetingRoomState.MeetingStateLeft
import io.dyte.coresampleapp.MainViewModel.MeetingRoomState.MeetingStateLoading
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
    roomName = MEETING_ROOM_NAME,
    authToken = AUTH_TOKEN,
    baseUrl = "https://api.cluster.dyte.in"
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
      println("DyteMobileClient | MainViewModel onMeetingInitFailed ${exception.localizedMessage}")
      meetingStateLiveData.value = MeetingStateFailed
    }

    override fun onMeetingRoomJoinStarted() {
      super.onMeetingRoomJoinStarted()
      meetingStateLiveData.value = MeetingStateLoading
    }

    override fun onMeetingRoomJoinCompleted() {
      super.onMeetingRoomJoinCompleted()
      meetingStateLiveData.value = MeetingStateJoined
    }

    override fun onMeetingRoomJoinFailed(exception: Exception) {
      super.onMeetingRoomJoinFailed(exception)
      meetingStateLiveData.value = MeetingStateFailed
    }

    override fun onMeetingRoomLeaveCompleted() {
      super.onMeetingRoomLeaveCompleted()
      meetingStateLiveData.value = MeetingStateLeft
    }
  }


  fun start(meeting: DyteMobileClient) {
    this.meeting = meeting

    meeting.addMeetingRoomEventsListener(meetingRoomEventsListner)
    meeting.init(meetingInfo)
  }

  override fun onCleared() {
    super.onCleared()
    meeting.removeMeetingRoomEventsListener(meetingRoomEventsListner)
  }
}