package me.enterman.bipush.resolvers.para

import me.enterman.bipush.MethodResolver
import me.enterman.bipush.NodeUtils.insnListOf
import me.enterman.bipush.NodeUtils.isConstantInt
import me.enterman.bipush.NodeUtils.toInt
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

object ParaClassPre:MethodResolver() {
	override fun resolve(methodNode: MethodNode): Boolean {
		return false
	}
	
	override fun resolve(classNode: ClassNode): Boolean {
		var change = false
		classNode.methods.filter { it.instructions.first != null }.forEach {
			if(resolve(it,classNode) && !change)
				change = true
		}
		return change
	}
	private fun resolve(methodNode: MethodNode, classNode: ClassNode): Boolean {
		var foundPara = false
		var type:String = ""
		methodNode.instructions.filter { it.opcode == NEW && (it as TypeInsnNode).desc == "java/lang/StringBuilder" }.forEach { node ->
			val dup = node.next ?: return@forEach
			if(dup.opcode != DUP) return@forEach
			val ldc = dup.next ?: return@forEach
			if(ldc !is LdcInsnNode || ldc.cst !is String) return@forEach
			val init = ldc.next ?: return@forEach
			if(init !is MethodInsnNode || init.name != "<init>") return@forEach
			val reverse = init.next ?: return@forEach
			if(reverse !is MethodInsnNode || reverse.name != "reverse") return@forEach
			val toString = reverse.next  ?: return@forEach
			if(toString !is MethodInsnNode || toString.name != "toString") return@forEach
			ldc.cst = StringBuilder(ldc.cst as String).reverse().toString()
			val constantInt = toString.next ?: return@forEach
			if(!constantInt.isConstantInt()) return@forEach
			val i = constantInt.toInt()
			ldc.cst = paraFun(ldc.cst as String,i)
			val invokeStatic = constantInt.next ?: return@forEach
			if(invokeStatic !is MethodInsnNode || invokeStatic.opcode != INVOKESTATIC) throw AssertionError()
			
			
			methodNode.instructions.also { list ->
				list.remove(node)
				list.remove(dup)
				list.remove(init)
				list.remove(reverse)
				list.remove(toString)
				list.remove(constantInt)
				list.remove(invokeStatic)
			}
			foundPara = true
			if(ldc.cst == "?") return@forEach
			val methodNode1 = classNode.methods.firstOrNull { it.name == invokeStatic.name } ?: throw AssertionError("No method found for classNode: ${classNode.name} for MethodNode: ${invokeStatic.name}, there are Only methods ${classNode.methods.joinToString(", ") { it.name }}")
			classNode.methods.remove(methodNode1)
			type = ldc.cst as String
			type = type.replace(".class","")
			ParaClassLoaderResolver.dump(ldc.cst as String,ldc.next.toInt())
			
		}
		if(foundPara){
			val list = methodNode.instructions
			while(list.first.opcode != PUTSTATIC){
				list.remove(list.first)
			}
			//val fieldInsnNode = list.first as FieldInsnNode
			if(type == "") throw AssertionError()
			//val desc = fieldInsnNode.desc.substring(1,fieldInsnNode.desc.length - 1)
			list.insert(insnListOf(
					TypeInsnNode(NEW,type),
					InsnNode(DUP),
					MethodInsnNode(INVOKESPECIAL,type,"<init>","()V")
			))
		}
		return false
	}
	fun paraFun(s: String, n: Int): String {
		val length = s.length
		val n2 = (n % length + length) % length
		return s.substring(n2, length) + s.substring(0, n2)
	}
}