package me.enterman.bipush.resolvers.unknown

import com.google.common.collect.MultimapBuilder
import me.enterman.bipush.resolvers.Resolver
import org.objectweb.asm.tree.ClassNode

object InterfaceMonitor:Resolver() {
	override fun doOn(classes: MutableMap<String, ClassNode>): Boolean {
		val mapInheritance = MultimapBuilder.hashKeys().arrayListValues().build<String,String>()
		classes.values.forEach { classNode ->
			classNode.interfaces.forEach {
				mapInheritance.put(it,classNode.name)
			}
		}
		
		val oneInherit = mapInheritance.asMap().filterValues { it.size == 1 }
		println("There are ${oneInherit.size} interfaces that have only one inheritor")
		oneInherit.forEach{
			val from = it.key
			val to = it.value.first()
		}
		return false
	}
}
