package me.enterman.bipush.asm

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodNode
import java.util.*
import java.util.function.BiConsumer


class InstructionModifier {
    private val replacements: MutableMap<AbstractInsnNode, InsnList> = mutableMapOf()
    private val appends: MutableMap<AbstractInsnNode, InsnList> = mutableMapOf()
    private val prepends: MutableMap<AbstractInsnNode, InsnList> = mutableMapOf()
    fun append(original: AbstractInsnNode, append: InsnList) {
        appends[original] = append
    }

    fun prepend(original: AbstractInsnNode, append: InsnList) {
        prepends[original] = append
    }

    fun replace(original: AbstractInsnNode, vararg insns: AbstractInsnNode?) {
        val singleton = InsnList()
        for (replacement in insns) {
            singleton.add(replacement)
        }
        replacements[original] = singleton
    }

    fun replace(original: AbstractInsnNode, replacements: InsnList) {
        this.replacements[original] = replacements
    }

    fun remove(original: AbstractInsnNode) {
        replacements[original] = EMPTY_LIST
    }

    fun removeAll(toRemove: List<AbstractInsnNode>) {
        for (insn in toRemove) {
            remove(insn)
        }
    }

    fun apply(methodNode: MethodNode) {
        replacements.forEach { (insn: AbstractInsnNode?, list: InsnList?) ->
            methodNode.instructions.insert(insn, list)
            methodNode.instructions.remove(insn)
        }
        prepends.forEach { (insn: AbstractInsnNode?, list: InsnList?) -> methodNode.instructions.insertBefore(insn, list) }
        appends.forEach { (insn: AbstractInsnNode?, list: InsnList?) -> methodNode.instructions.insert(insn, list) }
    }

    companion object {
        private val EMPTY_LIST: InsnList = InsnList()
    }
}