package me.enterman.bipush.resolvers.general

import me.enterman.bipush.NodeUtils.toInt
import me.enterman.bipush.NodeUtils.toNode
import me.enterman.bipush.resolvers.ClassResolver
import me.enterman.bipush.utils.OpcodeThing
import me.enterman.bipush.utils.OpcodeThing.Companion.aaload
import me.enterman.bipush.utils.OpcodeThing.Companion.aastore
import me.enterman.bipush.utils.OpcodeThing.Companion.anewarray
import me.enterman.bipush.utils.OpcodeThing.Companion.constint
import me.enterman.bipush.utils.OpcodeThing.Companion.getstatic
import me.enterman.bipush.utils.OpcodeThing.Companion.iaload
import me.enterman.bipush.utils.OpcodeThing.Companion.iastore
import me.enterman.bipush.utils.OpcodeThing.Companion.newarray
import me.enterman.bipush.utils.OpcodeThing.Companion.putstatic
import me.enterman.bipush.utils.matchFromStart
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.LdcInsnNode

object ConstArray: ClassResolver(){
	override fun resolve(classNode: ClassNode): Boolean {
		ConstIntArr.resolve(classNode)
		ConstArrStr.resolve(classNode)
		return false
	}
	
}
object ConstArrStr : ClassResolver(){
	override fun resolve(classNode: ClassNode): Boolean {
		val remove = mutableSetOf<ArrInfo<*>>()
		val clinit = classNode.methods.firstOrNull { it.name == "<clinit>" } ?: return false
		val arrStrFields = mutableMapOf<String,MutableMap<Int,ArrInfo<String>>>()
		clinit.instructions.filter(matchFromStart(
				getstatic(classNode.name,"[Ljava/lang/String;"),
				constint,
				OpcodeThing.ldcstr,
				aastore,
		)).forEach {
			if(it !is FieldInsnNode) throw AssertionError()
			arrStrFields.getOrPut(it.name, {mutableMapOf()})[it.next.toInt()] = ArrInfo((it.next.next as LdcInsnNode).cst as String, listOf(it,it.next,it.next.next,it.next.next.next),it.name)
		}
		classNode.methods.forEach p@ { methodNode ->
			val replacements = mutableMapOf<List<AbstractInsnNode>, AbstractInsnNode>()
			methodNode.instructions.filter(matchFromStart(
					getstatic(classNode.name,"[Ljava/lang/String;"),
					constint,
					aaload,
			)).forEach {
				if(it !is FieldInsnNode) throw AssertionError()
				if(!arrStrFields.contains(it.name)) return@forEach
				if(!arrStrFields[it.name]!!.containsKey(it.next.toInt())) return@forEach
				replacements[listOf(it,it.next,it.next.next)] = LdcInsnNode(arrStrFields[it.name]!![it.next.toInt()]!!.value)
				remove.add(arrStrFields[it.name]!![it.next.toInt()]!!)
			}
			replacements.forEach { (listReplace,replacement) ->
				methodNode.instructions.insertBefore(listReplace[0],replacement)
				listReplace.forEach {
					methodNode.instructions.remove(it)
				}
			}
			
		}
		remove.forEach { arrInfo ->
			arrInfo.nodes.forEach {
				clinit.instructions.remove(it)
			}
			classNode.fields.removeIf { arrInfo.arrName == it.name }
		}
		val newArrayInstructions = mutableListOf<AbstractInsnNode>()
		clinit.instructions.filter(matchFromStart(
				constint,
				anewarray("java/lang/String"),
				putstatic(classNode.name,"[Ljava/lang/String;")
		)).forEach { node ->
			if(remove.map { it.arrName }.contains((node.next.next as FieldInsnNode).name)){
				newArrayInstructions.addAll(listOf(node,node.next,node.next.next))
			}
		}
		newArrayInstructions.forEach {
			clinit.instructions.remove(it)
		}
		return false
	}
	
}
object ConstIntArr : ClassResolver(){
	override fun resolve(classNode: ClassNode): Boolean {
		val remove = mutableSetOf<ArrInfo<*>>()
		val clinit = classNode.methods.firstOrNull { it.name == "<clinit>" } ?: return false
		
		val arrIntFields = mutableMapOf<String,MutableMap<Int,ArrInfo<Int>>>()
		clinit.instructions.filter(matchFromStart(
				getstatic(classNode.name,"[I"),
				constint,
				constint,
				iastore,
		)).forEach {
			if(it !is FieldInsnNode) throw AssertionError()
			val arrinfo =ArrInfo(it.next.next.toInt(), listOf(it,it.next,it.next.next,it.next.next.next),it.name)
			arrIntFields.getOrPut(it.name, {mutableMapOf()})[it.next.toInt()] = arrinfo
			remove.add(arrinfo)
		}
		classNode.methods.forEach p@ { methodNode ->
			val replacements = mutableMapOf<List<AbstractInsnNode>, AbstractInsnNode>()
			methodNode.instructions.filter(matchFromStart(
					getstatic(classNode.name,"[I"),
					constint,
					iaload,
			)).forEach {
				if(it !is FieldInsnNode) throw AssertionError()
				if(!arrIntFields.contains(it.name)) return@forEach
				if(!arrIntFields[it.name]!!.containsKey(it.next.toInt())) return@forEach
				replacements[listOf(it,it.next,it.next.next)] = toNode(arrIntFields[it.name]!![it.next.toInt()]!!.value)
				//remove.add(arrIntFields[it.name]!![it.next.toInt()]!!)
			}
			replacements.forEach { (listReplace,replacement) ->
				methodNode.instructions.insertBefore(listReplace[0],replacement)
				listReplace.forEach {
					methodNode.instructions.remove(it)
				}
			}
			
		}
		
		remove.forEach { arrInfo ->
			arrInfo.nodes.forEach {
				clinit.instructions.remove(it)
			}
			classNode.fields.removeIf { arrInfo.arrName == it.name }
		}
		val newArrayInstructions = mutableListOf<AbstractInsnNode>()
		clinit.instructions.filter(matchFromStart(
				constint,
				newarray(10),
				putstatic(classNode.name,"[I")
		)).forEach { node ->
			if(remove.map { it.arrName }.contains((node.next.next as FieldInsnNode).name)){
				newArrayInstructions.addAll(listOf(node,node.next,node.next.next))
			}
		}
		newArrayInstructions.forEach {
			clinit.instructions.remove(it)
		}
		return false
	}
	
}
data class ArrInfo<T>(val value:T,val nodes:List<AbstractInsnNode>, val arrName:String)