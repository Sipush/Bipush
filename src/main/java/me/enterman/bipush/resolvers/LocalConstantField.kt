package me.enterman.bipush.resolvers

import com.google.common.collect.MultimapBuilder
import org.objectweb.asm.Opcodes.ISTORE
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.VarInsnNode

class LocalConstantField : Resolver() {

    override fun doOn(classes: MutableMap<String, ClassNode>): Boolean {
        classes.values.forEach { node ->

            node.methods.stream().filter { it.instructions.first != null }.forEach { methodNode ->
                val constantLocalFields = MultimapBuilder
                        .SetMultimapBuilder
                        .hashKeys()
                        .hashSetValues()
                        .build<ConstField, AbstractInsnNode>()
                val notConstant = mutableSetOf<Int>()
                methodNode.instructions.forEach { insnNode ->
                    if (insnNode is VarInsnNode)
                        if (insnNode.opcode == ISTORE) {
                            //if(constantLocalFields.keys().any{it.node}){
                            //   constantLocalFields.keySet().removeAll { it.node.equals() insnNode }
                            notConstant.add(insnNode.`var`)
                            //}
                            //if(isConstantInt(insnNode.previous) && !notConstant.contains(insnNode.`var`))
                            //     constantLocalFields.put(insnNode,)
                        }

                }
            }

        }
        return false
    }

}

data class ConstField(val node: VarInsnNode, val index: Int)