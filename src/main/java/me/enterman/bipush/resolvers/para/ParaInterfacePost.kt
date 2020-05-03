package me.enterman.bipush.resolvers.para

import me.enterman.bipush.resolvers.Resolver
import me.tongfei.progressbar.ProgressBar
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

object ParaInterfacePost : Resolver() {
	private val map = mutableMapOf<String, String>()
	private val map2 = mutableMapOf<String, AbstractInsnNode>()
	override fun doOn(classes: MutableMap<String, ClassNode>): Boolean {
		// TODO 这里有错误 请修复一下
		val list = classes.values.toList()
		for (classNode in ProgressBar.wrap(list, "Class #1")) {
			//println(list[0].name)
			if (!classNode.name.startsWith("c") && !classNode.name.startsWith("w")) continue
			val clinit = classNode.methods.firstOrNull { it.name == "<clinit>" } ?: continue
			val first = clinit.instructions.first ?: continue
			if (first.opcode != NEW) continue
			val actualClass = (first as TypeInsnNode).desc
			val invokeSpecial = first.next.next ?: continue
			if (invokeSpecial !is MethodInsnNode || invokeSpecial.opcode != INVOKESPECIAL) continue
			val putStatic = invokeSpecial.next ?: continue
			if (putStatic !is FieldInsnNode || putStatic.opcode != PUTSTATIC) continue
			val interfaceClass = putStatic.desc.substring(1, putStatic.desc.length - 1)
			putStatic.desc = "L${actualClass};"
			val field = classNode.fields.firstOrNull { it.name == "\uD83E\uDD14" } ?: continue
			field.desc = "L${actualClass};"
			classNode.methods.flatMap { methodNode -> methodNode.instructions.filter { it.opcode == INVOKEINTERFACE && (it as MethodInsnNode).owner == interfaceClass } }.forEach {
				if (it !is MethodInsnNode) return@forEach
				it.owner = actualClass
				it.opcode = INVOKEVIRTUAL
				it.itf = false
				var prev = it.previous
				while (prev !is FieldInsnNode) {
					if (prev.previous == null)
						throw AssertionError()
					prev = prev.previous
				}
				
				
				prev.desc = "L${actualClass};"
			}
			//println("TypeTo is $typeTo")
			//println("One of the classes is ${classes.keys.iterator().next()}")
			//classes.keys.forEach {
			//	println(it)
			//}
			val actualClassNode = classes["${actualClass}.class"]!!
			actualClassNode.interfaces.clear()
			actualClassNode.methods.removeIf {
				it.name == "<init>"
			} // Remove constructor as we are making it static
			actualClassNode.methods.forEach q@{ methodNode ->
				val clonedLabels = mutableMapOf<LabelNode,LabelNode>()
				methodNode.access = 9 // ACC_PUBLIC and ACC_STATIC
				methodNode.instructions.forEach {
					if (it !is VarInsnNode) return@forEach
					it.`var`--
					
				}
				map2["${actualClassNode.name}-${methodNode.name}"] = methodNode.instructions.filter {
					it !is VarInsnNode && when (it.opcode) {
						IRETURN,
						DRETURN,
						RETURN,
						LRETURN,
						FRETURN,
						ARETURN -> false
						else -> true
					}
				}.also { assert(it.size == 1) }[0].let {
					when(it){
						is MethodInsnNode ->{
							MethodInsnNode(it.opcode,it.owner,it.name,it.desc,it.itf)
						}
						is LdcInsnNode -> {
							LdcInsnNode(it.cst)
						}
						else -> throw AssertionError()
					}
				}
			} // Make all methods static
			map[classNode.name] = actualClassNode.name
			classes.remove("${classNode.name}.class")!!
			classes.remove("${interfaceClass}.class")!!
			
		}
		ProgressBar.wrap(classes.values, "Remapping Wrapper Method to Actual Method...").forEach { classNode ->
			classNode.methods.forEach { methodNode ->
				methodNode.instructions.forEach {
					if (it is MethodInsnNode && it.opcode == INVOKESTATIC && map.contains(it.owner)) {
						it.owner = map[it.owner]!!
					}
				}
			}
			
		}
		map2.keys.forEach {
			//println(it)
		}
		/*
		mutableMapOf<String,InsnList>().also{
			it.putAll(map2)
					it.forEach { (t, u) ->
						val iter = u.iterator()
						while(iter.hasNext()){
							val abstractInsnNode = iter.next()
							println("$t - $abstractInsnNode")
						}
						
						
						
					}
		}*/
		
		/*
		ProgressBar.wrap(classes.values, "Replacing Actual Methods with opcode").forEach { classNode ->
			
			classNode.methods.forEach { methodNode ->
				val replacements = mutableMapOf<AbstractInsnNode, AbstractInsnNode>()
				methodNode.instructions.forEach {
					if (it is MethodInsnNode && it.opcode == INVOKESTATIC) {
						if (map2.contains("${it.owner}-${it.name}")) {
							val node = map2["${it.owner}-${it.name}"]!!
							//println(node)
							replacements[it] = node
						}
					}
					
				}
				
				replacements.forEach { (t, u) ->
					methodNode.instructions.insertBefore(t, u)
					methodNode.instructions.remove(t)
				}
			}
		}*/
		return false
	}
}