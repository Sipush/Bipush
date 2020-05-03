package me.enterman.bipush.resolvers.general

import me.enterman.bipush.resolvers.ClassResolver
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnNode

object ConstBool : ClassResolver() {
	override fun doOn(classes: MutableMap<String, ClassNode>): Boolean {
		println("ConstBool might break")
		return super.doOn(classes)
	}
	
	override fun resolve(classNode: ClassNode): Boolean {
		
		val marked = mutableListOf<String>()
		classNode.fields.forEach { fieldNode ->
			if (fieldNode.access == 25 && fieldNode.desc == "Z") {
				val clinit = classNode.methods.firstOrNull { it.name == "<clinit>" }
				if (clinit == null) {
					marked.add(fieldNode.name)
					return@forEach
				}
				if (clinit.instructions.any {
							it.opcode == PUTSTATIC && (it as FieldInsnNode).name == fieldNode.name
						})
					return@forEach
				marked.add(fieldNode.name)
				
				
			}
		}
		
		classNode.methods.forEach { methodNode ->
			val replacements = mutableMapOf<AbstractInsnNode, AbstractInsnNode>()
			methodNode.instructions.forEach {
				if (it is FieldInsnNode && (it.opcode == GETSTATIC || it.opcode == GETFIELD) && marked.contains(it.name)) {
					replacements[it] = InsnNode(ICONST_0)
				}
				
			}
			replacements.forEach { (replace, replacement) ->
				methodNode.instructions.insertBefore(replace, replacement)
				methodNode.instructions.remove(replace)
			}
		}
		
		classNode.fields.removeAll {
			marked.contains(it.name)
		}
		return false
	}
}