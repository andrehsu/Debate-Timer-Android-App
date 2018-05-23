package andre.debatetimer.livedata

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

abstract class NLiveData<T>(init: T) : LiveData<T>() {
	init {
		value = init
	}
	
	public override fun setValue(value: T) {
		super.setValue(value)
	}
	
	override fun getValue(): T {
		return super.getValue()!!
	}
	
	fun observe(owner: LifecycleOwner, observer: (T) -> Unit) {
		val obv = Observer<T> { observer(it!!) }
		super.observe(owner, obv)
	}
}

class BooleanLiveData(boolean: Boolean = false) : NLiveData<Boolean>(boolean)


class IntLiveData(int: Int = 0) : NLiveData<Int>(int)

class StringLiveData(string: String = "") : NLiveData<String>(string)