package me.enterman.bipush.resolvers.para

import me.enterman.bipush.resolvers.Resolver
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode

object ParaConstMethodAlias : Resolver() {
	override fun doOn(classes: MutableMap<String, ClassNode>): Boolean {
		val clzes = mutableListOf<String>()
		classes.values.forEach { classNode ->
			classNode.methods.forEach { methodNode ->
				val replacePool = mutableListOf<MethodInsnNode>()
				methodNode.instructions.forEach {
					if(it is MethodInsnNode && it.opcode == Opcodes.INVOKESTATIC && (it.owner.startsWith("c/") || it.owner.startsWith("w/")))
						replacePool.add(it)
				}
				//assert(replacePool.isNotEmpty())
				replacePool.forEach { methodInsnNode ->
					//println(methodInsnNode.owner)
					val invokeOrLdc = classes[methodInsnNode.owner + ".class"]!!
							.methods.firstOrNull { it.name ==  methodInsnNode.name}!!.instructions.last
							.previous!!.clone(mutableMapOf())
					clzes.add(methodInsnNode.owner+".class")
					methodNode.instructions.insertBefore(methodInsnNode,invokeOrLdc)
					methodNode.instructions.remove(methodInsnNode)
				}
			}
		}
		clzes.forEach {
			classes.remove(it)
		}
		return false
	}
}