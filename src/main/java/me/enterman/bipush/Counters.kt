package me.enterman.bipush

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Counters(private val template:String) {
	val counters = mutableMapOf<String,Counter>()
	private lateinit var logger:Logger
	
	//@CallerSensitive
	fun addCounter(ct:String):Counter{
		if(!this::logger.isInitialized)
			logger = LoggerFactory.getLogger(this::class.java)
		val atomicint = Counter()
		counters[ct] = atomicint
		return atomicint
	}
	fun announce(){
		counters.forEach { (st, int) ->
			logger.info(template,int.i,st + if(int.i != 1) "s" else "")
		}
	}
	fun changes():Boolean{
		return counters.values.any { it.i != 0 }
	}
}
class Counter(var i:Int = 0){
	fun inc(){i++}
}

