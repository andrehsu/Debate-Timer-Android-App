@file:Suppress("NOTHING_TO_INLINE")

package andre.debatetimer.extensions

import andre.debatetimer.MainActivity
import andre.debatetimer.extensions.EnvVars.shortAnimTime
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.ViewPropertyAnimator
import android.widget.EditText
import java.util.*

/**
 * Created by Andre on 3/25/2017.
 */
object EnvVars {
	var shortAnimTime: Long = -1
		private set
	var mediumAnimTime: Long = -1
		private set
	var longAnimTime: Long = -1
		private set
	
	lateinit var applicationContext: Context
		private set
	
	fun init(activity: MainActivity) {
		applicationContext = activity.applicationContext
		with(applicationContext) {
			clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
			with(resources) {
				shortAnimTime = getInteger(android.R.integer.config_shortAnimTime).toLong()
				mediumAnimTime = getInteger(android.R.integer.config_mediumAnimTime).toLong()
				longAnimTime = getInteger(android.R.integer.config_longAnimTime).toLong()
			}
		}
	}
}

private var clipboard: ClipboardManager? = null

fun copyText(view: View, label: String, textToCopy: String, textToShow: String? = null): Boolean {
	clipboard!!.primaryClip = ClipData.newPlainText(label, textToCopy)
	Snackbar.make(view, (textToShow ?: textToCopy) + " copied", Snackbar.LENGTH_SHORT).show()
	return true
}

inline var EditText.textStr: String
	get() = this.text.toString()
	set(value) = this.setText(value)

inline fun EditText.clearText() = this.setText("")

inline fun EditText.isEmpty(): Boolean = this.text.toString().isEmpty()

inline fun View.setVisible() {
	this.visibility = View.VISIBLE
}

inline fun View.setInvisible() {
	this.visibility = View.INVISIBLE
}

inline fun View.setGone() {
	this.visibility = View.GONE
}

inline fun <E> Set<E>.unmodifiable(): Set<E> = Collections.unmodifiableSet(this)

inline fun <E> List<E>.unmodifiable(): List<E> = Collections.unmodifiableList(this)

inline fun <E> Collection<E>.unmodifiable(): Collection<E> = Collections.unmodifiableCollection(this)

inline fun <K, V> Map<K, V>.unmodifiable(): Map<K, V> = Collections.unmodifiableMap(this)

inline fun <E> SortedSet<E>.unmodifiable(): SortedSet<E> = Collections.unmodifiableSortedSet(this)

inline fun <K, V> SortedMap<K, V>.unmodifiable(): SortedMap<K, V> = Collections.unmodifiableSortedMap(this)

operator fun String.invoke(start: Int = 0, end: Int = length): String = substring(start, if (end >= 0) end else end + length)

operator fun String.invoke(start: Int = 0, end: Int = length, step: Int): String = slice(start..(if (end >= 0) end else end + length) step step)

fun ViewPropertyAnimator.setOnEnd(onEnd: (Animator?) -> Unit): ViewPropertyAnimator {
	setListener(object : AnimatorListenerAdapter() {
		override fun onAnimationEnd(animation: Animator?) {
			onEnd(animation)
		}
	})
	return this
}

inline fun View.fadeIn(animTime: Long = shortAnimTime): ViewPropertyAnimator = this.let {
	it.alpha = 0.0f
	it.setVisible()
	it.animate().alpha(1.0f).setDuration(animTime).setListener(null)
}

inline fun View.fadeOut(animTime: Long = shortAnimTime): ViewPropertyAnimator = this.animate().alpha(0.0f).setDuration(animTime).setOnEnd { this.setGone();this.alpha = 1.0f }

infix fun View.crossfadeTo(to: View): CrossfadeAnimator = CrossfadeAnimator(this.fadeOut(), to.fadeIn())

class CrossfadeAnimator internal constructor(private val outAnimator: ViewPropertyAnimator,
                                             private val inAnimator: ViewPropertyAnimator) {
	infix fun withDuration(animTime: Long) {
		apply { this.duration = animTime }
	}
	
	private inline fun apply(func: ViewPropertyAnimator.() -> Unit) {
		outAnimator.func()
		inAnimator.func()
	}
}

inline infix fun View.replaceWith(to: View) {
	this.setGone()
	to.setVisible()
}

inline fun Int.abs() = if (this < 0) -this else this

fun secondsToString(seconds: Int): String {
	val abs = seconds.abs()
	val minutes = abs / 60
	val secondsOnly = abs % 60
	return if (seconds >= 0) {
		""
	} else {
		"-"
	} + "$minutes:${secondsOnly.toString().padStart(2, '0')}"
}

fun Context.getColorCompat(id:Int) = ContextCompat.getColor(this,id)