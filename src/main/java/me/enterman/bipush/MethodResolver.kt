package me.enterman.bipush

import me.enterman.bipush.resolvers.ClassResolver
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

abstract class MethodResolver:ClassResolver() {
	abstract fun resolve(methodNode: MethodNode): Boolean
	override fun resolve(classNode: ClassNode): Boolean {
		var change = false
		classNode.methods.filter { it.instructions.first != null }.forEach {
			if(resolve(it) && !change)
				change = true
		}
		return change
	}
}