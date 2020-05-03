package me.enterman.bipush.resolvers.unknown

import me.enterman.bipush.resolvers.Resolver
import me.enterman.bipush.utils.MethodMatcher
import me.enterman.bipush.utils.OpcodeThing.Companion.astore
import me.enterman.bipush.utils.OpcodeThing.Companion.getstatic
import me.enterman.bipush.utils.OpcodeThing.Companion.invokestatic
import me.enterman.bipush.utils.OpcodeThing.Companion.ldcstr
import me.enterman.bipush.utils.matchFromStart
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import sun.misc.BASE64Decoder
import java.security.Key
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec


object RC4Decrypter : Resolver() {
	override fun doOn(classes: MutableMap<String, ClassNode>): Boolean {
		classes.values.forEach { it ->
			it.methods.forEach { methodNode ->
				val removeClasses = mutableListOf<String>()
				
				val replacements = mutableMapOf<List<AbstractInsnNode>, AbstractInsnNode>()
				methodNode.instructions.filter(matchFromStart(
						ldcstr,
						invokestatic("(Ljava/lang/String;)Ljava/lang/String;"),
				)).forEach loop@{ node ->
					val stringToDecrypt = (node as LdcInsnNode).cst as String
					val methodInsn = node.next as MethodInsnNode
					val decryptClassName = methodInsn.owner
					val decryptClassNode = classes["$decryptClassName.class"]
							?: return@loop
					val decryptMethod = decryptClassNode.methods.first { it.name == methodInsn.name && it.desc == methodInsn.desc }
					if (!MethodMatcher(
									ldcstr,
									astore(2),
									getstatic("Ljava/lang/String;"),
									invokestatic("javax/crypto/Cipher", "getInstance", "(Ljava/lang/String;)Ljavax/crypto/Cipher;")
							)(decryptMethod)) {
						// println("in class $decryptClassName")
						return@loop
					}
					val decryptClinit = decryptClassNode.methods.first {
						it.name == "<clinit>"
					}
					var ldcField = decryptClinit.instructions.last.previous.previous
					if(ldcField is LdcInsnNode && ldcField.cst == "ARCFOUR")
						ldcField = ldcField.previous.previous
					if (!ldcstr(ldcField)) throw AssertionError("That wasn't ldc field? it was $ldcField, in class $decryptClassName")
					
					
					val ldcNodeInMethod = decryptMethod.instructions.first!!
					if (!ldcstr(ldcNodeInMethod)) throw AssertionError()
					val str = rc4func(stringToDecrypt, (ldcNodeInMethod as LdcInsnNode).cst as String, (ldcField as LdcInsnNode).cst as String)
					println("Decrypted String: $str in class ${it.name} in Method: ${methodNode.name}" +
							"\nDecryptor Class: $decryptClassName")
					replacements[listOf(node, node.next)] = LdcInsnNode(str)
					removeClasses.add("$decryptClassName.class")
				}
				replacements.forEach { (t, u) ->
					methodNode.instructions.insertBefore(t[0], u)
					t.forEach {
						methodNode.instructions.remove(it)
					}
					
				}
				//removeClasses.forEach { classes.remove(it) }
			}
		}
		return false
	}
}

fun rc4func(toDecrypt: String, ldcInMethod: String, ldcField: String): String {
	return try {
		val cipher = Cipher.getInstance("ARCFOUR")
		val keySpec = SecretKeySpec(ldcField.toByteArray(), "ARCFOUR") // ldc field was the secret key
		cipher.init(2, keySpec as Key)
		// ldc in method was the key to part 2 encrypted with key of ldc field
		val decryptedKey = cipher.doFinal(BASE64Decoder().decodeBuffer(ldcInMethod))
		val decryptedKeySpec = SecretKeySpec(decryptedKey, "ARCFOUR")
		cipher.init(2, decryptedKeySpec as Key)
		// return Final string
		String(cipher.doFinal(BASE64Decoder().decodeBuffer(toDecrypt)))
	} catch (exception: Exception) {
		throw AssertionError(exception)
	}
}
