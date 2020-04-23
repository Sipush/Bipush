package me.enterman.bipush


import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*

object NodeUtils {
    fun AbstractInsnNode.isInstruction(): Boolean {
        return this !is LabelNode && this !is FrameNode && this !is LineNumberNode
    }

    fun AbstractInsnNode.nextNode():AbstractInsnNode?{
        var next: AbstractInsnNode? = next ?: return null
        while (next?.isInstruction() != true)
            next = next?.next
        return next
    }
    fun AbstractInsnNode.prev(): AbstractInsnNode? {
        var prev: AbstractInsnNode? = previous ?: return null
        while (prev?.isInstruction() != true)
            prev = prev?.previous
        return prev
    }
    fun AbstractInsnNode.isConstant(): Boolean {
        return when(opcode){
            BIPUSH, SIPUSH, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5, ICONST_M1, LCONST_0, LCONST_1, DCONST_0, DCONST_1, ACONST_NULL, FCONST_0, FCONST_1, FCONST_2 -> true
            else -> {
                when(this){
                    is LdcInsnNode ->{
                        this.cst !is Type
                    }
                    else -> false
                }
            }
        }
    }
    fun AbstractInsnNode.terminates(): Boolean {
        return when (opcode) {
            RETURN,
            IRETURN,
            ARETURN,
            FRETURN,
            DRETURN,
            LRETURN,
            ATHROW,
            TABLESWITCH,
            LOOKUPSWITCH,
            GOTO -> true
            else -> false
        }
    }

    fun AbstractInsnNode.toInt(): Int {
        return when (this) {
            is IntInsnNode -> {
                operand
            }
            is InsnNode -> {
                when (opcode) {
                    ICONST_5 -> 5
                    ICONST_4 -> 4
                    ICONST_3 -> 3
                    ICONST_2 -> 2
                    ICONST_1 -> 1
                    ICONST_0 -> 0
                    ICONST_M1 -> -1
                    else -> throw IllegalArgumentException("WTF? Node is not number")
                }
            }
            is LdcInsnNode -> {
                cst as Int
            }
            else -> throw IllegalArgumentException("WTF? Node is not InsnNode or IntInsnNode")
        }
    }

    /**
     * Generates the <code>InsnNode</code> for a number.
     * It will find the most Optimistic node for the number.
     * @param number the number
     * @return a insnNode of a constant number instrument
     * @throws IllegalArgumentException if the number is out of range.
     */
    fun fromInt(number: Int): AbstractInsnNode {
        return when (number) {
            -1 -> InsnNode(ICONST_M1)
            0 -> InsnNode(ICONST_0)
            1 -> InsnNode(ICONST_1)
            2 -> InsnNode(ICONST_2)
            3 -> InsnNode(ICONST_3)
            4 -> InsnNode(ICONST_4)
            5 -> InsnNode(ICONST_5)
            else -> {
                if (-128 <= number && number <= 127)
                    IntInsnNode(BIPUSH, number)
                else if (-32768 <= number && number <= 32767)
                    IntInsnNode(SIPUSH, number)
                LdcInsnNode(number)
            }
        }
    }

    fun isConstantInt(node: AbstractInsnNode): Boolean {
        if (node is LdcInsnNode)
            return node.cst is Int
        return when (node.opcode) {
            BIPUSH, SIPUSH, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5, ICONST_M1 -> true
            else -> false
        }
    }
}