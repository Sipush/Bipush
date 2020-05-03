package me.enterman.bipush.resolvers

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import java.nio.charset.StandardCharsets
import java.security.Key
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.Cipher.DECRYPT_MODE
import javax.crypto.spec.SecretKeySpec

object Decryptor : ClassResolver() {
	
	
	override fun resolve(classNode: ClassNode): Boolean {
		var changes: Boolean // Prevent recursive calls to decryptor method
		do{
			changes = false
			val possibleMethods = classNode.methods.filter {
				it.access == 10
						&& it.desc == "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"
						&& it.instructions.first is LabelNode
						&& it.instructions.first.next.opcode == Opcodes.NEW
						&& it.instructions.first.next is TypeInsnNode
			}.filter { (it.instructions.first.next as TypeInsnNode).desc == "javax/crypto/spec/SecretKeySpec" }.also {
				if(it.size > 2) throw AssertionError()
			}
			
			val blowfishMethod = possibleMethods.firstOrNull { methodNode -> methodNode.instructions.any { it is LdcInsnNode && it.cst == "Blowfish" } }
			val desMethod = possibleMethods.firstOrNull { methodNode -> methodNode.instructions.any { it is LdcInsnNode && it.cst == "DES" } }
			classNode.methods.forEach p@ { methodNode ->
				val replacements = mutableMapOf<List<AbstractInsnNode>, AbstractInsnNode>()
				methodNode.instructions.filter { it is MethodInsnNode && (it.name == desMethod?.name || it.name == blowfishMethod?.name) }.forEach {
					val arg1 = it.previous
					if (arg1 !is LdcInsnNode || arg1.cst !is String) return@forEach
					val arg0 = arg1.previous
					if (arg0 !is LdcInsnNode || arg0.cst !is String) return@forEach
					println("arg0: ${arg0.cst} arg1: ${arg1.cst} class: ${classNode.name} method: ${methodNode.name}")
					replacements[listOf(arg0,arg1,it)] = LdcInsnNode(
							if((it as MethodInsnNode).name == blowfishMethod?.name)
								blowfishMethod(arg0.cst as String,arg1.cst as String)
							else
								desMethod(arg0.cst as String,arg1.cst as String)
					
					)
					changes = true
				}
				replacements.forEach { (t, u) ->
					methodNode.instructions.insertBefore(t[0], u)
					t.forEach { methodNode.instructions.remove(it) }
				}
			}
			desMethod?.let { classNode.methods.remove(it)}
			blowfishMethod?.let { classNode.methods.remove(it)}
		}while(changes)
		
		return false
	}
	private fun blowfishMethod(string: String, string2: String): String? {
		return try {
			val secretKeySpec = SecretKeySpec(MessageDigest.getInstance("MD5").digest(string2.toByteArray(StandardCharsets.UTF_8)), "Blowfish")
			val cipher = Cipher.getInstance("Blowfish")
			cipher.init(DECRYPT_MODE, secretKeySpec as Key)
			String(cipher.doFinal(Base64.getDecoder().decode(string.toByteArray(StandardCharsets.UTF_8))), StandardCharsets.UTF_8)
		} catch (exception: java.lang.Exception) {
			exception.printStackTrace()
			null
		}
	}
	fun desMethod(string: String, string2: String): String? {
		return try {
			val secretKeySpec = SecretKeySpec((MessageDigest.getInstance("MD5").digest(string2.toByteArray(StandardCharsets.UTF_8)) as ByteArray).copyOf(8), "DES")
			val cipher = Cipher.getInstance("DES")
			cipher.init(DECRYPT_MODE, secretKeySpec as Key)
			String(cipher.doFinal(Base64.getDecoder().decode(string.toByteArray(StandardCharsets.UTF_8))), StandardCharsets.UTF_8)
		} catch (exception: Exception) {
			exception.printStackTrace()
			null
		}
	}
}