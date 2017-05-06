package andre.debatetimer

import andre.debatetimer.extensions.*
import andre.debatetimer.extensions.EnvVars.init
import andre.debatetimer.extensions.EnvVars.longAnimTime
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Vibrator
import android.support.constraint.ConstraintLayout
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.properties.Delegates


class MainActivity : AppCompatActivity(), DebugLogger {
	//<editor-fold desc="Stete classes">
	interface State
	
	interface HasTimerOption : State {
		val timerOption: TimerOption
	}
	
	object WaitingToBegin : State
	
	data class WaitingToStart(override val timerOption: TimerOption) : State, HasTimerOption
	
	data class TimerStarted(override val timerOption: TimerOption, val timer: DebateTimer, val running: Boolean = false) : State, HasTimerOption
	//</editor-fold>
	
	//<editor-fold desc="Fields">
	private lateinit var vibrator: Vibrator
	private lateinit var buttons: List<Button>
	private lateinit var timerTexts: List<TextView>
	private lateinit var action_debateBell: MenuItem
	private var timerDisplayCountUp = false
	private var state: State by Delegates.observable(WaitingToBegin as State) { _, oldValue, newValue ->
		require(newValue !is WaitingToBegin)
		
		if (oldValue is WaitingToBegin) {
			cl_timer.setVisible()
			bt_startPause.setVisible()
			action_debateBell.isVisible = true
			tv_startingText.setGone()
		}
		
		if (oldValue is TimerStarted && (newValue !is TimerStarted || newValue is TimerStarted && oldValue.timer !== newValue.timer)) {
			oldValue.timer.stop()
		}
	}
	//</editor-fold>
	
	//<editor-fold desc="Activity callbacks">
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		
		//<editor-fold desc="Field initialization">
		init(this)
		
		vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
		buttons = listOf(bt_3sec, bt_2min, bt_3min, bt_4min, bt_5min, bt_7min, bt_8min)
		timerTexts = listOf(tv_timerNegative, tv_timer_m, tv_timer_s, tv_timer_colon)
		//</editor-fold>
		
		//<editor-fold desc="Show 3 second button if debugging">
		if (BuildConfig.DEBUG) {
			bt_3sec.setVisible()
		}
		//</editor-fold>
		
		//<editor-fold desc="StartPause button onClick">
		bt_startPause.setOnClickListener {
			bt_startPause crossfadeTo bt_startPause withDuration longAnimTime
			val state = state
			when (state) {
				is WaitingToStart -> {
					if (state is HasTimerOption) {
						val timer = object : DebateTimer(state.timerOption) {
							override fun onSecond() = refreshTimer()
						}
						
						this.state = TimerStarted(state.timerOption, timer)
						
						bt_startPause.callOnClick()
					}
				}
				is TimerStarted -> {
					onTap()
					if (state.running) {
						bt_startPause.text = getString(R.string.resume)
						this.state = state.copy(running = false)
						state.timer.pause()
					} else {
						bt_startPause.text = getString(R.string.pause)
						this.state = state.copy(running = true)
						state.timer.resume()
					}
				}
			}
		}
		//</editor-fold>
		
		//<editor-fold desc="Time buttons onClick">
		clearButtonsSelection()
		buttons.forEach {
			it.setOnClickListener { view ->
				val state = state
				if (state !is TimerStarted || state is TimerStarted && !state.running) {
					onTap()
					
					clearButtonsSelection()
					
					view as Button
					view.alpha = 1.0f
					view.setTextColor(getColor(R.color.buttonSelected))
					
					bt_startPause.text = getString(R.string.start)
					
					val timerOption = TimerOption.parseKey(view.tag as String)
					
					this.state = WaitingToStart(timerOption)
					
					refreshTimer()
					refreshBellLabel()
				}
			}
		}
		//</editor-fold>
		
		//<editor-fold desc="Set on toggle for elapsed/ remaining">
		cl_timer.setOnClickListener {
			timerDisplayCountUp = !timerDisplayCountUp
			if (timerDisplayCountUp) {
				tv_timerDisplayMode.text = getString(R.string.timer_display_count_up)
			} else {
				tv_timerDisplayMode.text = getString(R.string.timer_display_count_down)
			}
			refreshTimer()
			refreshBellLabel()
		}
		//</editor-fold>
	}
	
	class ExitDialogFragment : DialogFragment() {
		override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
			return AlertDialog.Builder(activity)
					.setTitle(R.string.exit_question)
					.setPositiveButton(android.R.string.yes, { _, _ -> activity.finish() })
					.setNegativeButton(android.R.string.no, { _, _ -> dialog.cancel() })
					.create()
		}
	}
	
	override fun onBackPressed() {
		if (isTaskRoot) {
			ExitDialogFragment().show(supportFragmentManager, null)
		} else {
			super.onBackPressed()
		}
	}
	
	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.activity_main, menu)
		action_debateBell = menu.findItem(R.id.action_debate_bell)
		return true
	}
	
	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_debate_bell -> {
				debateBellEnabled = !debateBellEnabled
				if (debateBellEnabled) {
					action_debateBell.icon = getDrawable(R.drawable.ic_notifications_active_white_24dp)
				} else {
					action_debateBell.icon = getDrawable(R.drawable.ic_notifications_off_white_24dp)
				}
				refreshBellLabel()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}
	//</editor-fold>
	
	//<editor-fold desc="UI fields">
	private var ui_minutes: Int by Delegates.observable(0) { _, oldValue, minutes ->
		if (oldValue != minutes) {
			tv_timer_m.text = minutes.toString()
		}
	}
	private var ui_seconds: Int by Delegates.observable(0) { _, oldValue, seconds ->
		if (oldValue != seconds) {
			tv_timer_s.text = seconds.toString().padStart(2, '0')
		}
	}
	private var ui_isNegative: Boolean by Delegates.observable(false) { _, oldValue, isNegative ->
		if (oldValue != isNegative) {
			if (isNegative) {
				tv_timerNegative.setVisible()
				guideline.layoutParams = (guideline.layoutParams as ConstraintLayout.LayoutParams).also { it.guidePercent = 0.5f }
			} else {
				tv_timerNegative.setGone()
				guideline.layoutParams = (guideline.layoutParams as ConstraintLayout.LayoutParams).also { it.guidePercent = 0.44f }
			}
		}
	}
	private var ui_isOvertime: Boolean by Delegates.observable(false) { _, oldValue, isOvertime ->
		if (oldValue != isOvertime) {
			if (isOvertime) {
				timerTexts.forEach { it.setTextColor(getColor(R.color.timerOvertime)) }
			} else {
				timerTexts.forEach { it.setTextColor(getColor(R.color.timerNormal)) }
			}
		}
	}
	//</editor-fold>
	
	//<editor-fold desc="UI functions">
	private fun refreshTimer() {
		debug { "refreshTimer()" }
		val state = state
		if (state is WaitingToStart) {
			if (timerDisplayCountUp) {
				ui_minutes = 0
				ui_seconds = 0
			} else {
				ui_minutes = state.timerOption.minutesOnly
				ui_seconds = state.timerOption.secondsOnly
			}
			ui_isNegative = false
			ui_isOvertime = false
		} else if (state is TimerStarted) {
			val timer = state.timer
			if (timerDisplayCountUp) {
				ui_minutes = timer.minutesSinceStart
				ui_seconds = timer.secondsSinceStart
				ui_isNegative = false
				ui_isOvertime = timer.isOvertime
			} else {
				ui_minutes = timer.minutesUntilEnd
				ui_seconds = timer.secondsUntilEnd
				ui_isNegative = timer.isOvertime
				ui_isOvertime = timer.isOvertime
			}
		}
	}
	
	private fun refreshBellLabel() {
		val state = state
		if (debateBellEnabled && state is HasTimerOption) {
			val timerOption = state.timerOption
			if (timerOption.countUpPoiString.isEmpty()) {
				tv_bellsAt.setInvisible()
			} else {
				tv_bellsAt.setVisible()
				
				val format = if (timerOption.bellsSinceStart.count() == 1) {
					getString(R.string.poi_bell)
				} else {
					getString(R.string.poi_bells)
				}
				
				val string = if (timerDisplayCountUp) {
					timerOption.countUpPoiString
				} else {
					timerOption.countDownPoiString
				}
				
				tv_bellsAt.text = format.format(string)
			}
		} else {
			tv_bellsAt.setInvisible()
		}
	}
	
	private fun onTap() {
		vibrator.vibrate(30)
	}
	
	private fun clearButtonsSelection() {
		buttons.forEach {
			it.alpha = 0.54f
			it.setTextColor(getColor(R.color.buttonUnselected))
		}
	}
	//</editor-fold>
}