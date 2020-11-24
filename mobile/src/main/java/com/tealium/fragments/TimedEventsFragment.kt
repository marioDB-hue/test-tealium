package com.tealium.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.tealium.core.Tealium
import com.tealium.mobile.BuildConfig
import com.tealium.mobile.R
import java.util.concurrent.TimeUnit

class TimedEventsFragment: Fragment() {

    private val invalidStartTime = -1L
    private var timerStarted: Boolean = false
    private var startTime: Long = invalidStartTime

    private lateinit var triggerTimedEventButton: Button
    private lateinit var eventNameEditText: EditText
    private lateinit var timedEventStatusText: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_timed_events, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        triggerTimedEventButton = view.findViewById(R.id.button_trigger_timed_event)
        triggerTimedEventButton.setOnClickListener {
            onTriggerTimedEvent()
        }

        eventNameEditText = view.findViewById(R.id.edit_timed_event_name)
        timedEventStatusText = view.findViewById(R.id.txt_timed_event_status)
    }

    private fun onTriggerTimedEvent() {
        val eventNameText = eventNameEditText.text.toString()
        if (timerStarted) {
            stopTimer(eventNameText)
        } else {
            if (eventNameText.isBlank()) {
                Toast.makeText(this.context, "Please supply an Event Name", Toast.LENGTH_SHORT).show()
                return
            }
            startTimer(eventNameText)
        }
    }

    private fun startTimer(name: String) {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.startTimedEvent(name, null)?.let {
            startTime = it
            timerStarted = true
            eventNameEditText.isEnabled = false

            triggerTimedEventButton.text = getString(R.string.timed_events_stop)
            timedEventStatusText.text = getString(R.string.timed_events_status_template_start, name)
        }
    }

    private fun stopTimer(name: String) {
        Tealium[BuildConfig.TEALIUM_INSTANCE]?.stopTimedEvent(name)?.let {
            timerStarted = false
            eventNameEditText.isEnabled = true

            triggerTimedEventButton.text = getString(R.string.timed_events_start)
            timedEventStatusText.text = getString(R.string.timed_events_status_template_stop, name, TimeUnit.MILLISECONDS.toSeconds(it - startTime))
            startTime = invalidStartTime
        }
    }


}