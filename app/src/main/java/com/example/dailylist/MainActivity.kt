package com.example.dailylist

import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val tasks = mutableListOf<Task>()
    private lateinit var adapter: TaskAdapter
    private lateinit var statusLine: TextView

    private var timerMinutes = 25
    private var timerSecondsLeft = 25 * 60
    private var timerRunning = false
    private var countDownTimer: CountDownTimer? = null

    private lateinit var timerDisplay: TextView
    private lateinit var timerStartBtn: Button
    private lateinit var timerMessage: TextView
    private lateinit var customMinutesInput: EditText
    private lateinit var preset5: Button
    private lateinit var preset15: Button
    private lateinit var preset25: Button

    private var centiseconds = 0
    private var stopwatchRunning = false
    private var stopwatchTimer: java.util.Timer? = null
    private val laps = mutableListOf<Int>()

    private lateinit var stopwatchDisplay: TextView
    private lateinit var stopwatchStartBtn: Button
    private lateinit var stopwatchLapBtn: Button
    private lateinit var lapsList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MobileAds.initialize(this) {}
        val adView: AdView = findViewById(R.id.adView)
        adView.loadAd(AdRequest.Builder().build())

        setupDateLabel()
        setupTaskList()
        setupTabs()
        setupTimer()
        setupStopwatch()
    }

    private fun setupDateLabel() {
        val dateLabel: TextView = findViewById(R.id.dateLabel)
        val fmt = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        dateLabel.text = fmt.format(Date())
    }

    private fun setupTaskList() {
        statusLine = findViewById(R.id.statusLine)
        val recyclerView: RecyclerView = findViewById(R.id.taskRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TaskAdapter(tasks) { updateStatusLine() }
        recyclerView.adapter = adapter

        val taskInput: EditText = findViewById(R.id.taskInput)
        val addTaskBtn: Button = findViewById(R.id.addTaskBtn)

        val addAction = {
            val text = taskInput.text.toString().trim()
            if (text.isNotEmpty()) {
                tasks.add(Task(System.currentTimeMillis(), text))
                adapter.notifyItemInserted(tasks.size - 1)
                taskInput.text.clear()
                updateStatusLine()
            }
        }

        addTaskBtn.setOnClickListener { addAction() }
        taskInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)
            ) {
                addAction()
                true
            } else false
        }

        updateStatusLine()
    }

    private fun updateStatusLine() {
        val remaining = tasks.count { !it.done }
        statusLine.text = when {
            tasks.isEmpty() -> "Nothing on the page yet."
            remaining == 0 -> "All done for today."
            else -> "$remaining left"
        }
    }

    private fun setupTabs() {
        val tabTimer: Button = findViewById(R.id.tabTimerBtn)
        val tabStopwatch: Button = findViewById(R.id.tabStopwatchBtn)
        val timerPanel: LinearLayout = findViewById(R.id.timerPanel)
        val stopwatchPanel: LinearLayout = findViewById(R.id.stopwatchPanel)

        tabTimer.setOnClickListener {
            timerPanel.visibility = android.view.View.VISIBLE
            stopwatchPanel.visibility = android.view.View.GONE
            tabTimer.setBackgroundColor(0xFF22201B.toInt())
            tabTimer.setTextColor(0xFFFFFFFF.toInt())
            tabStopwatch.setBackgroundColor(0x00000000)
            tabStopwatch.setTextColor(0xFF8A8578.toInt())
        }

        tabStopwatch.setOnClickListener {
            stopwatchPanel.visibility = android.view.View.VISIBLE
            timerPanel.visibility = android.view.View.GONE
            tabStopwatch.setBackgroundColor(0xFF22201B.toInt())
            tabStopwatch.setTextColor(0xFFFFFFFF.toInt())
            tabTimer.setBackgroundColor(0x00000000)
            tabTimer.setTextColor(0xFF8A8578.toInt())
        }
    }

    private fun setupTimer() {
        timerDisplay = findViewById(R.id.timerDisplay)
        timerStartBtn = findViewById(R.id.timerStartBtn)
        val timerResetBtn: Button = findViewById(R.id.timerResetBtn)
        timerMessage = findViewById(R.id.timerMessage)
        customMinutesInput = findViewById(R.id.customMinutesInput)
        preset5 = findViewById(R.id.preset5Btn)
        preset15 = findViewById(R.id.preset15Btn)
        preset25 = findViewById(R.id.preset25Btn)

        preset5.setOnClickListener { setTimerMinutes(5) }
        preset15.setOnClickListener { setTimerMinutes(15) }
        preset25.setOnClickListener { setTimerMinutes(25) }

        customMinutesInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                applyCustomMinutes()
                true
            } else false
        }
        customMinutesInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        timerStartBtn.setOnClickListener {
            if (timerSecondsLeft <= 0) return@setOnClickListener
            timerRunning = !timerRunning
            if (timerRunning) startCountdown() else countDownTimer?.cancel()
            renderTimer()
        }

        timerResetBtn.setOnClickListener {
            countDownTimer?.cancel()
            timerRunning = false
            timerSecondsLeft = timerMinutes * 60
            renderTimer()
        }

        renderTimer()
    }

    private fun applyCustomMinutes() {
        val value = customMinutesInput.text.toString().toIntOrNull()
        if (value != null && value > 0) {
            setTimerMinutes(value)
        } else {
            customMinutesInput.setText(timerMinutes.toString())
        }
    }

    private fun setTimerMinutes(m: Int) {
        countDownTimer?.cancel()
        timerRunning = false
        timerMinutes = m
        timerSecondsLeft = m * 60
        customMinutesInput.setText(m.toString())
        renderTimer()
    }

    private fun startCountdown() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer((timerSecondsLeft * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timerSecondsLeft = (millisUntilFinished / 1000).toInt() + 1
                renderTimer()
            }

            override fun onFinish() {
                timerSecondsLeft = 0
                timerRunning = false
                renderTimer()
            }
        }.start()
    }

    private fun renderTimer() {
        val mm = (timerSecondsLeft / 60).toString().padStart(2, '0')
        val ss = (timerSecondsLeft % 60).toString().padStart(2, '0')
        timerDisplay.text = "$mm:$ss"

        val done = timerSecondsLeft <= 0
        timerDisplay.setTextColor(if (done) 0xFFC0453A.toInt() else 0xFF22201B.toInt())
        timerStartBtn.text = if (timerRunning) "❙❙" else "▶"
        timerStartBtn.isEnabled = !done
        timerMessage.visibility = if (done) android.view.View.VISIBLE else android.view.View.GONE

        val presets = listOf(5 to preset5, 15 to preset15, 25 to preset25)
        for ((m, btn) in presets) {
            if (m == timerMinutes) {
                btn.setBackgroundColor(0xFF22201B.toInt())
                btn.setTextColor(0xFFFFFFFF.toInt())
            } else {
                btn.setBackgroundColor(0xFFFFFDF7.toInt())
                btn.setTextColor(0xFF8A8578.toInt())
            }
        }
    }

    private fun setupStopwatch() {
        stopwatchDisplay = findViewById(R.id.stopwatchDisplay)
        stopwatchStartBtn = findViewById(R.id.stopwatchStartBtn)
        stopwatchLapBtn = findViewById(R.id.stopwatchLapBtn)
        lapsList = findViewById(R.id.lapsList)

        stopwatchStartBtn.setOnClickListener {
            stopwatchRunning = !stopwatchRunning
            if (stopwatchRunning) {
                stopwatchTimer = java.util.Timer()
                stopwatchTimer?.scheduleAtFixedRate(object : java.util.TimerTask() {
                    override fun run() {
                        centiseconds += 1
                        runOnUiThread { renderStopwatch() }
                    }
                }, 10, 10)
            } else {
                stopwatchTimer?.cancel()
            }
            renderStopwatch()
        }

        stopwatchLapBtn.setOnClickListener {
            if (stopwatchRunning) {
                laps.add(0, centiseconds)
                renderLaps()
            } else {
                centiseconds = 0
                laps.clear()
                renderStopwatch()
                renderLaps()
            }
        }

        renderStopwatch()
    }

    private fun formatStopwatch(c: Int): String {
        val mm = (c / 6000).toString().padStart(2, '0')
        val ss = ((c % 6000) / 100).toString().padStart(2, '0')
        val ms = (c % 100).toString().padStart(2, '0')
        return "$mm:$ss.$ms"
    }

    private fun renderStopwatch() {
        stopwatchDisplay.text = formatStopwatch(centiseconds)
        stopwatchStartBtn.text = if (stopwatchRunning) "❙❙" else "▶"
        stopwatchLapBtn.text = if (stopwatchRunning) "Lap" else "⟲"
    }

    private fun renderLaps() {
        lapsList.removeAllViews()
        laps.forEachIndexed { index, lapValue ->
            val row = TextView(this)
            row.text = "Lap ${laps.size - index}   ${formatStopwatch(lapValue)}"
            row.setTextColor(0xFF8A8578.toInt())
            row.textSize = 13f
            lapsList.addView(row)
        }
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        stopwatchTimer?.cancel()
        super.onDestroy()
    }
}
