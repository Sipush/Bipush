package me.enterman.bipush.resolvers.general

import me.enterman.bipush.NodeUtils.insnListOf
import me.enterman.bipush.NodeUtils.isConstantInt
import me.enterman.bipush.NodeUtils.toInt
import me.enterman.bipush.resolvers.SimpleReplacementResolver
import org.objectweb.asm.Opcodes.INVOKESTATIC
import org.objectweb.asm.tree.*

object MethodFold:SimpleReplacementResolver(){
	override fun doOn(classes: MutableMap<String, ClassNode>): Boolean {
		val bol = super.doOn(classes)
		classes.remove("remix/agN.class")
		return bol
	}
	override fun doReplace(methodNode: MethodNode, replacements: MutableMap<List<AbstractInsnNode>, InsnList>) {
		//val ct = counters.addCounter("remix.agN")
		methodNode.instructions.filter { it.opcode == INVOKESTATIC && it is MethodInsnNode && it.owner == "remix/agN" && it.name == "void" }.forEach {
			val intNode = it.previous
			if(!intNode.isConstantInt()) throw AssertionError()
			val ldc = intNode.previous
			if(ldc !is LdcInsnNode || ldc.cst !is String) throw AssertionError()
			replacements[listOf(intNode,ldc,it)] = insnListOf(LdcInsnNode(method(ldc.cst as String,intNode.toInt())))
			//ct.inc()
		}
	}
	private fun method(str:String, i:Int):String{
		val var2: CharArray = str.toCharArray()
		val var3 = StringBuilder()
		var var4 = 0
		while (var4 < var2.size) {
				var3.append((var2[var4].toInt() xor i).toChar())
				++var4
		}
		return var3.toString()
		
		
	}
	
}