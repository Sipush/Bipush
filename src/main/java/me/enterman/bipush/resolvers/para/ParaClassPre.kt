package me.enterman.bipush.resolvers.para

import me.enterman.bipush.MethodResolver
import org.objectweb.asm.Opcodes.DUP
import org.objectweb.asm.Opcodes.NEW
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode

object ParaClassPre:MethodResolver() {
	override fun resolve(methodNode: MethodNode): Boolean {
		methodNode.instructions.filter { it.opcode == NEW && (it as TypeInsnNode).desc == "java/lang/StringBuilder" }.forEach {
			if(it.next.opcode != DUP) return@forEach
			val ldc = it.next?.next ?: return@forEach
			if(ldc !is LdcInsnNode || ldc.cst !is String) return@forEach
			val init = ldc.next ?: return@forEach
			if(init !is MethodInsnNode || init.name != "<init>") return@forEach
			val reverse = init.next ?: return@forEach
			val toString = reverse.next  ?: return@forEach
			
		}
		return false
	}
	
}