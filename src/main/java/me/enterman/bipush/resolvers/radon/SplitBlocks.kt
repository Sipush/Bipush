package me.enterman.bipush.resolvers.radon

import me.enterman.bipush.Main
import me.enterman.bipush.NodeUtils.prev
import me.enterman.bipush.NodeUtils.terminates
import me.enterman.bipush.resolvers.Resolver
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger


class SplitBlocks : Resolver() {
    override fun doOn(classes: MutableMap<String, ClassNode>): Boolean {
        val counter = AtomicInteger()
        classes.values.forEach { classNode ->
            classNode.methods.stream().filter { methodNode -> methodNode.instructions.first != null }.forEach { methodNode ->
                var modified: Boolean
                outer@
                do{
                    modified = false
                    val references = mutableMapOf<LabelNode, Int>()
                    for (instruction in methodNode.instructions) {
                        when(instruction){
                            is JumpInsnNode -> {
                                references.merge(instruction.label,1,Integer::sum)
                            }

                            is TableSwitchInsnNode -> {
                                references.merge(instruction.dflt,1,Integer::sum)
                                instruction.labels.forEach {
                                    references.merge(it,1,Integer::sum)
                                }
                            }
                            is LookupSwitchInsnNode -> {
                                references.merge(instruction.dflt,1,Integer::sum)
                                instruction.labels.forEach {
                                    references.merge(it,1,Integer::sum)
                                }
                            }
                        }
                    }
                    if(methodNode.tryCatchBlocks != null){
                        methodNode.tryCatchBlocks.forEach { tryCatchBlockNode ->
                            references[tryCatchBlockNode.start] = 999
                            references[tryCatchBlockNode.end] = 999
                            references[tryCatchBlockNode.handler] = 999
                        }
                    }
                    for (i in 0 until methodNode.instructions.size()) {
                        val node = methodNode.instructions[i]
                        if (node.opcode == Opcodes.GOTO) {
                            val cast = node as JumpInsnNode

                            if (references[cast.label] == 1) {
                                var next: AbstractInsnNode? = cast.label
                                val prev: AbstractInsnNode? = next?.prev()
                                if (prev != null) {
                                    var ok: Boolean = prev.terminates()
                                    while (next != null) {
                                        if (next === node) {
                                            ok = false // If it jumps to itself
                                        }
                                        if (methodNode.tryCatchBlocks != null) {
                                            for (tryCatchBlock in methodNode.tryCatchBlocks) {
                                                val start = methodNode.instructions.indexOf(tryCatchBlock.start)
                                                val mid = methodNode.instructions.indexOf(next)
                                                val end = methodNode.instructions.indexOf(tryCatchBlock.end)
                                                if (mid in start until end) {
                                                    // it's not ok if we're relocating the basic block outside the try-catch block
                                                    val startIndex = methodNode.instructions.indexOf(node)
                                                    if (startIndex < start || startIndex >= end) {
                                                        ok = false
                                                    }
                                                }
                                            }
                                        }
                                        if (next !== cast.label && references.getOrDefault(next, 0) > 0) {
                                            ok = false // if next is a label that is referenced more than once.
                                        }
                                        if (!ok) {
                                            break
                                        }
                                        if (next.terminates()) {
                                            break
                                        }
                                        next = next.next
                                    }
                                    next = cast.label.next
                                    if (ok) {
                                        val remove: MutableList<AbstractInsnNode> = ArrayList()
                                        while (next != null) {
                                            remove.add(next)
                                            if (next.terminates()) {
                                                break
                                            }
                                            next = next.next
                                        }
                                        val list = InsnList()
                                        remove.forEach(methodNode.instructions::remove)
                                        remove.forEach(list::add)
                                        methodNode.instructions.insert(node, list)
                                        methodNode.instructions.remove(node)
                                        modified = true
                                        counter.incrementAndGet()
                                        continue@outer
                                    }
                                }
                            }
                        }
                    }
                    references.forEach { (t, u) ->
                        Main.INSTANCE.logger.debug("LabelNode {} has {} references",t.label,u)
                    }
                }while(modified)
            }
        }
        return counter.get() != 0
    }
}