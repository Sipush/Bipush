package me.enterman.bipush.resolvers.general

import me.enterman.bipush.resolvers.ClassResolver
import org.objectweb.asm.Opcodes.ACC_BRIDGE
import org.objectweb.asm.Opcodes.ACC_SYNTHETIC
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

object SyntheticBridgeRemover:ClassResolver() {
	override fun resolve(classNode: ClassNode): Boolean {
		removeSyntheticOrBridge(classNode)
		classNode.fields.forEach { removeSyntheticOrBridge(it) }
		classNode.methods.forEach { removeSyntheticOrBridge(it) }
		return false
	}
	private fun removeSyntheticOrBridge(classNode: ClassNode){
		if ((classNode.access and ACC_SYNTHETIC) != 0)
			classNode.access = classNode.access and ACC_SYNTHETIC.inv()
		if ((classNode.access and ACC_BRIDGE) != 0)
			classNode.access = classNode.access and ACC_BRIDGE.inv()
		
	}
	private fun removeSyntheticOrBridge(methodNode: MethodNode){
		if ((methodNode.access and ACC_SYNTHETIC) != 0)
			methodNode.access = methodNode.access and ACC_SYNTHETIC.inv()
		if ((methodNode.access and ACC_BRIDGE) != 0)
			methodNode.access = methodNode.access and ACC_BRIDGE.inv()
	}
	private fun removeSyntheticOrBridge(fieldNode: FieldNode){
		if ((fieldNode.access and ACC_SYNTHETIC) != 0)
			fieldNode.access = fieldNode.access and ACC_SYNTHETIC.inv()
		if ((fieldNode.access and ACC_BRIDGE) != 0)
			fieldNode.access = fieldNode.access and ACC_BRIDGE.inv()
	}
}