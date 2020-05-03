package me.enterman.bipush.resolvers.general

import me.enterman.bipush.resolvers.ClassResolver
import org.objectweb.asm.Opcodes.INVOKESTATIC
import org.objectweb.asm.Opcodes.RETURN
import org.objectweb.asm.tree.*

object ClinitToClinit:ClassResolver(){
	override fun resolve(classNode: ClassNode): Boolean {
		val clinit = classNode.methods.firstOrNull { it.name=="<clinit>" } ?: return false
		val first = clinit.instructions.first // Might be int init method
		if(first !is MethodInsnNode || first.opcode != INVOKESTATIC || first.desc != "()V" || first.owner != classNode.name)
			return false
		var second:AbstractInsnNode? = first.next // Might be String init method
		if(second !is MethodInsnNode || second.opcode != INVOKESTATIC || second.desc != "()V" || second.owner != classNode.name)
			second = null
		if(second != null && second is MethodInsnNode) {
			val secondmeth = classNode.methods.first { it.name == second.name }
			if(secondmeth.instructions.last.opcode == RETURN)
				secondmeth.instructions.remove(secondmeth.instructions.last)
			clinit.instructions.insert(secondmeth.instructions)
			clinit.instructions.remove(second)
			classNode.methods.remove(secondmeth)
		}
		val firstmeth = classNode.methods.first { it.name == first.name }
		if(firstmeth.instructions.last.opcode == RETURN)
			firstmeth.instructions.remove(firstmeth.instructions.last)
		clinit.instructions.insert(firstmeth.instructions)
		clinit.instructions.remove(first)
		classNode.methods.remove(firstmeth)
		return false
	}
}

private fun InsnList.clone(): InsnList {
	val mv = MethodNode()
	accept(mv)
	return mv.instructions
}
