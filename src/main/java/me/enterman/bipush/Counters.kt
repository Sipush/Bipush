package me.enterman.bipush

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

class Counters(private val template:String) {
	val counters = mutableMapOf<String,AtomicInteger>()
	private lateinit var logger:Logger
	
	//@CallerSensitive
	fun addCounter(ct:String):AtomicInteger{
		if(!this::logger.isInitialized)
			logger = LoggerFactory.getLogger(this::class.java)
		val atomicint = AtomicInteger()
		counters[ct] = atomicint
		return atomicint
	}
	fun announce(){
		counters.forEach { (st, int) ->
			logger.info(template,int,st + if(int.get() != 1) "s" else "")
		}
	}
	fun changes():Boolean{
		return counters.values.any { it.get() != 0 }
	}
}

