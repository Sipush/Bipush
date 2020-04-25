package me.enterman.bipush.resolvers

import org.objectweb.asm.tree.ClassNode

abstract class ClassResolver:Resolver() {
	abstract fun resolve(classNode: ClassNode):Boolean
	override fun doOn(classes: Map<String, ClassNode>): Boolean {
		var change = false
		classes.values.forEach {
			if(resolve(it) && !change)
				change = true
		}
		return change
	}
}