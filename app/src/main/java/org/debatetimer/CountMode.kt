package org.debatetimer

enum class CountMode {
    CountUp, CountDown;
    
    override fun toString(): String {
        return when (this) {
            CountUp -> "count_up"
            CountDown -> "count_down"
        }
    }
    
    companion object {
        fun fromString(string: String): CountMode {
            return if (string == "count_down") CountDown else CountUp
        }
    }
}

fun CountMode.other(): CountMode = when (this) {
    CountMode.CountUp -> CountMode.CountDown
    CountMode.CountDown -> CountMode.CountUp
}