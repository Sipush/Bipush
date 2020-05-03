package me.enterman.bipush.utils

import me.enterman.bipush.NodeUtils.isConstantInt
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

class MethodMatcher(private val l: List<OpcodeThing>) : (MethodNode) -> Boolean {
	constructor(vararg opcodeThing: OpcodeThing) : this(opcodeThing.toList())
	
	override fun invoke(p1: MethodNode): Boolean {
		if (p1.instructions.size() < l.size)
			return false
		val iter = l.iterator()
		return p1.instructions.all {
			if (!iter.hasNext())
				return true
			if(it is LabelNode || it is FrameNode)
				return@all true
			iter.next()(it)
		}
	}
	
}

class Matcher(private val matchFun: (AbstractInsnNode) -> Boolean) : (AbstractInsnNode) -> Boolean {
	override fun invoke(p1: AbstractInsnNode): Boolean {
		return matchFun(p1)
	}
}

fun matchFromStart(vararg nodes: OpcodeThing): Matcher {
	if (nodes.isEmpty())
		throw IllegalArgumentException()
	return Matcher { node ->
		val iter = nodes.iterator()
		var next = node
		while (iter.hasNext()) {
			if (iter.next()(next))
				next = next.next ?: return@Matcher false
			else
				return@Matcher false
		}
		true
	}
}

class OpcodeThing(val op: (AbstractInsnNode) -> Boolean) : (AbstractInsnNode) -> Boolean {
	override fun invoke(p1: AbstractInsnNode): Boolean {
		return op(p1)
	}
	
	companion object {
		val dup = OpcodeThing {
			it.opcode == DUP
		}
		
		fun new(ref: String): OpcodeThing {
			return OpcodeThing {
				it is TypeInsnNode && it.opcode == NEW && it.desc == ref
			}
		}
		
		fun invokestatic(owner: String, name: String, desc: String): OpcodeThing {
			return OpcodeThing {
				it is MethodInsnNode && it.opcode == INVOKESTATIC && it.owner == owner && it.desc == desc && it.name == name
			}
		}
		fun invokestatic(desc: String): OpcodeThing {
			return OpcodeThing {
				it is MethodInsnNode && it.opcode == INVOKESTATIC && it.desc == desc
			}
		}
		
		fun getstatic(owner: String, desc: String): OpcodeThing {
			return OpcodeThing {
				it is FieldInsnNode && it.owner == owner && it.desc == desc && it.opcode == GETSTATIC
			}
		}
		fun getstatic(desc: String): OpcodeThing {
			return OpcodeThing {
				it is FieldInsnNode && it.desc == desc && it.opcode == GETSTATIC
			}
		}
		fun putstatic(owner: String, desc: String): OpcodeThing {
			return OpcodeThing {
				it is FieldInsnNode && it.owner == owner && it.desc == desc && it.opcode == PUTSTATIC
			}
		}
		
		fun getstaticArr(owner: String): OpcodeThing {
			return OpcodeThing {
				it is FieldInsnNode && it.owner == owner && it.desc.startsWith("[") && !it.desc.startsWith("[[") && it.opcode == GETSTATIC
			}
		}
		fun anewarray(desc: String):OpcodeThing {
			return OpcodeThing {
				it is TypeInsnNode && it.opcode == ANEWARRAY && it.desc == desc
			}
		}
		fun newarray(primitiveType:Int):OpcodeThing{
			return OpcodeThing {
				it.opcode == NEWARRAY && it is IntInsnNode && it.operand == primitiveType
			}
		}
		fun astore(varInt:Int):OpcodeThing{
			return OpcodeThing{
				it.opcode == ASTORE && it is VarInsnNode && it.`var` == varInt
			}
		}
		val constint = OpcodeThing {
			it.isConstantInt()
		}
		val iastore = OpcodeThing {
			it.opcode == IASTORE
		}
		val iaload = OpcodeThing {
			it.opcode == IALOAD
		}
		val ldcstr = OpcodeThing {
			it is LdcInsnNode && it.cst is String
		}
		val aaload = OpcodeThing {
			it.opcode == AALOAD
		}
		val aastore = OpcodeThing {
			it.opcode == AASTORE
		}
	}
	
}