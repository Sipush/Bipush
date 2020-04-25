package me.enterman.bipush.resolvers.radon


import me.enterman.bipush.resolvers.Resolver
import org.objectweb.asm.Opcodes.PUTSTATIC
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodNode
import java.lang.reflect.Modifier


/**
 * Replaces Radon Obfuscations that check for always true or always false values.
 * <br></br>
 * See `me.itzsomebody.radon.transformers.obfuscator.flow.GotoReplacer`
 */
class FinalBoolean:Resolver(){
	override fun doOn(classes: Map<String, ClassNode>): Boolean {
		classes.values.forEach {classNode ->
			classNode.fields.forEach { fieldNode ->
				if(fieldNode.desc == "Z" && Modifier.isFinal(fieldNode.access) && Modifier.isPrivate(fieldNode.access) && Modifier.isStatic(fieldNode.access)){
					val good = classNode.methods.all { methodNode ->
						return@all methodNode.instructions.all noAccess@ {
							if(it is FieldInsnNode && it.opcode == PUTSTATIC){
								return@noAccess !(it.owner == classNode.name && it.desc == "Z" && it.name == fieldNode.name)
							}
							true
						}
					}
					if(good){

					}

				}

			}
		}
		return false
	}

}