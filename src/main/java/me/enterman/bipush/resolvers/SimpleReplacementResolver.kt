package me.enterman.bipush.resolvers

import me.enterman.bipush.Counters
import me.enterman.bipush.MethodResolver
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodNode

abstract class SimpleReplacementResolver(private val loop: Boolean = true) : MethodResolver() {
	val counters = Counters("Replaced {} {}")
	override fun resolve(methodNode: MethodNode): Boolean {
		val replacements = mutableMapOf<List<AbstractInsnNode>,InsnList>()
		do{
			replacements.clear()
			doReplace(methodNode,replacements)
			replacements.forEach { (t, u) ->
				methodNode.instructions.insertBefore(t[0], u)
				t.forEach { methodNode.instructions.remove(it) }
			}
		}while (if(loop) replacements.isNotEmpty() else false)
		counters.announce()
		return counters.changes()
	}

	abstract fun doReplace(methodNode:MethodNode, replacements:MutableMap<List<AbstractInsnNode>,InsnList>)
	
}