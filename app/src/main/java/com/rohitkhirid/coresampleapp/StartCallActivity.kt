package com.rohitkhirid.coresampleapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import com.rohitkhirid.coresampleapp.databinding.ActivityStartCallBinding

class StartCallActivity : AppCompatActivity() {
  private lateinit var binding: ActivityStartCallBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityStartCallBinding.inflate(LayoutInflater.from(this))
    setContentView(binding.root)

    binding.tvMeetingInfo.text = "Joining $MEETING_ROOM_NAME"
    binding.btnJoin.setOnClickListener {
      startActivity(Intent(this@StartCallActivity, CallActivity::class.java))
      finishAffinity()
    }
  }
}