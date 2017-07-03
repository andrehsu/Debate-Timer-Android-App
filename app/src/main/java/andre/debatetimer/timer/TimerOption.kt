package andre.debatetimer.timer

import andre.debatetimer.extensions.secondsToString
import andre.debatetimer.timer.DebateBell.Once
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug

class TimerOption(val seconds: Int, val bellsSinceStart: Map<Int, DebateBell>) {
	companion object : AnkoLogger {
		private val DEFAULT = TimerOption(420, mapOf(60 to Once, 360 to Once))
		private val cache = mutableMapOf<String, TimerOption>()
		
		fun parseTag(tag: String): TimerOption {
			@Suppress("NAME_SHADOWING")
			val tag = tag.filterNot { it == ' ' }
			return cache.getOrPut(tag) {
				val tokens = tag.split(';')
				
				try {
					val seconds = tokens[0].toInt()
					
					val bells = if (tokens[1].isEmpty()) {
						mapOf()
					} else if (tokens[1].toIntOrNull() == -1) {
						mapOf(60 to Once, seconds - 60 to Once)
					} else {
						val bellTokens = tokens[1].split(',')
						bellTokens.map { it.toInt() to Once }.toMap()
					}
					
					TimerOption(seconds, bells)
				} catch(e: RuntimeException) {
					debug { "Error occurred while parsing \"$tag\" for TimerOption" }
					DEFAULT
				}
			}
		}
	}
	
	val countUpString = bellsSinceStart.filter { (_, v) -> v == Once }.map { secondsToString(it.key) }.joinToString()
	val countDownString = bellsSinceStart.filter { (_, v) -> v == Once }.map { secondsToString(seconds - it.key) }.joinToString()
	
	val minutesOnly = seconds / 60
	val secondsOnly = seconds % 60
}