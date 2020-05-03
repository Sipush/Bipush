package me.enterman.bipush.resolvers.unknown

import me.enterman.bipush.resolvers.ClassResolver
import me.enterman.bipush.utils.MethodMatcher
import me.enterman.bipush.utils.OpcodeThing.Companion.dup
import me.enterman.bipush.utils.OpcodeThing.Companion.invokestatic
import me.enterman.bipush.utils.OpcodeThing.Companion.new
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import java.nio.charset.StandardCharsets
import java.util.*

object Base64Thing :ClassResolver(){
	val base64methoddecryptor = MethodMatcher(
			new("java/lang/String"),
			dup,
			invokestatic("java/util/Base64", "getDecoder", "()Ljava/util/Base64\$Decoder;")
	)
	
	
	override fun resolve(classNode: ClassNode): Boolean {
		val base64method = classNode.methods.firstOrNull(base64methoddecryptor) ?: return false
		println("class: ${classNode.name} method: ${base64method.name}")
		classNode.methods.forEach a@ { methodNode ->
			val replacements = mutableMapOf<List<AbstractInsnNode>,AbstractInsnNode>()
			methodNode.instructions.filter {
				invokestatic(classNode.name,base64method.name,"(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;")(it)
			}.forEach {
				val prev = it.previous
				val prev1 = prev.previous
				if(prev1 !is LdcInsnNode || prev1.cst !is String)
					return@forEach
				if(prev !is LdcInsnNode || prev.cst !is String)
					return@forEach
				replacements[listOf(prev1,prev,it)] = LdcInsnNode(base64Meth(prev1.cst as String, prev.cst as String))
			}
			replacements.forEach {(list,node) ->
				methodNode.instructions.insertBefore(list[0],node)
				list.forEach { methodNode.instructions.remove(it) }
			}
		}
		classNode.methods.remove(base64method)
		return false
	}
	private fun base64Meth(string: String, string2: String): String {
		val stringDecoded = String(Base64.getDecoder().decode(string.toByteArray(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)
		val stringBuilder = StringBuilder()
		val arrc = string2.toCharArray()
		val arrc2 = stringDecoded.toCharArray()
		val n2 = arrc2.size
		for ((n, i) in (0 until n2).withIndex()) {
			val c = arrc2[i]
			stringBuilder.append((c.toInt() xor arrc[n % arrc.size].toInt()).toChar())
		}
		return stringBuilder.toString()
	}
	
}