package me.enterman.bipush.resolvers.para

import me.enterman.bipush.MethodResolver
import me.enterman.bipush.NodeUtils.insnListOf
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

// don't use this
class ParaRemapper:MethodResolver() {

	
	override fun resolve(methodNode: MethodNode): Boolean {
		val first = methodNode.instructions.first
		if(first is TypeInsnNode && first.opcode == NEW){
			if(first.desc == "c/t/d/t"){
				val node = methodNode.instructions.find { it.opcode == INVOKESPECIAL && it is MethodInsnNode && it.owner == "c/t/d/t" && it.desc == "(Ljava/lang/ClassLoader;Ljava/lang/String;I)V" }?:throw AssertionError()
				methodNode.instructions.insertBefore(node.previous.previous,insnListOf(
						InsnNode(DUP),
						VarInsnNode(ASTORE,1)
				))
				val list = mutableListOf<AbstractInsnNode>()
				var next = node.next
				while(next.opcode != ICONST_1){
					list.add(next)
					next = next.next
				}
				list.add(next)
				list.add(next.next)
				methodNode.instructions.insert(next.next, insnListOf(
						VarInsnNode(ALOAD,1),
						LdcInsnNode("/"),
						LdcInsnNode("."),
						MethodInsnNode(INVOKEVIRTUAL,"java/lang/String","replace","(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;"),
						LdcInsnNode(".class"),
						LdcInsnNode(""),
						MethodInsnNode(INVOKEVIRTUAL,"java/lang/String","replace","(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;")
						
						))
				list.forEach {
					methodNode.instructions.remove(it)
				}
			}
		}
		return false
	}
}