package me.enterman.bipush.resolvers

import org.objectweb.asm.tree.ClassNode

abstract class ClassResolver:Resolver() {
	var classes:MutableMap<String,ClassNode> = mutableMapOf()
	abstract fun resolve(classNode: ClassNode):Boolean
	override fun doOn(classes: MutableMap<String, ClassNode>): Boolean {
		this.classes = classes
		var change = false
		classes.values.forEach {
			if(resolve(it) && !change)
				change = true
		}
		return change
	}
}