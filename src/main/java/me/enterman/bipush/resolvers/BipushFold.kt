package me.enterman.bipush.resolvers

import com.google.common.primitives.UnsignedBytes.toInt
import org.objectweb.asm.tree.*
import me.enterman.bipush.NodeUtils.toInt
import me.enterman.bipush.NodeUtils.fromInt
import me.enterman.bipush.NodeUtils.isConstantInt
import org.objectweb.asm.Opcodes.*
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

class BipushFold : Resolver() {
    //val logger = LoggerFactory.getLogger(this.javaClass)

    override fun doOn(classes: Map<String,ClassNode>):Boolean {
        val nops = AtomicInteger(0)
        val constantStringLength = AtomicInteger(0)
        val constantStringHashCode = AtomicInteger(0)
        val constantIntCalc = AtomicInteger(0)
        classes.values.forEach { classNode ->
            classNode.methods.stream().filter { it.instructions.first != null }.forEach { methodNode: MethodNode ->
                val replacements = mutableMapOf<List<AbstractInsnNode>, InsnList>()
                do {
                    replacements.clear()
                    for (i in 0 until methodNode.instructions.size()) {

                        val ain = methodNode.instructions[i]
                        when (ain.opcode) {
                            NOP -> { // We don't want NOPs to get in our way, mainly because NOP does exactly nothing.
                                replacements[listOf(ain)] = InsnList()
                                nops.getAndIncrement()
                            }
                            INVOKEVIRTUAL -> {
                                if (ain is MethodInsnNode)
                                    if (ain.owner == "java/lang/String")
                                        if (ain.name == "length") {
                                            val before = ain.previous
                                            if (before.opcode == LDC)
                                                if (before is LdcInsnNode) // If we are invoking on a constant string's length, we want to eliminate that to just a number constant.
                                                    if (before.cst is String) {
                                                        val replacement = InsnList()
                                                        replacement.add(LdcInsnNode((before.cst as String).length))
                                                        replacements[listOf(before, ain)] = replacement
                                                        constantStringLength.getAndIncrement()
                                                    }
                                        } else if(ain.name == "hashCode") {
                                            val before = ain.previous
                                            if (before.opcode == LDC)
                                                if (before is LdcInsnNode) // Constant String's hashCode is always constant.
                                                    if (before.cst is String) {
                                                        val replacement = InsnList()
                                                        replacement.add(fromInt((before.cst as String).hashCode()))
                                                        replacements[listOf(before, ain)] = replacement
                                                        constantStringHashCode.getAndIncrement()
                                                    }
                                        }


                            }
                            IUSHR,
                            IOR,
                            IAND,
                            IXOR,
                            ISHL,
                            ISHR,
                            IADD,
                            ISUB,
                            IREM,
                            IDIV,
                            IMUL -> {
                                val oneBefore = ain.previous
                                val twoBefore = oneBefore.previous
                                if (isConstantInt(oneBefore) && isConstantInt(twoBefore)) {
                                    /* If the two opcodes before this arithmetic operational instruction are
                                        All defining a constant number, such as int a = 1+1, we will then make 'a' always 2.
                                */
                                    val replace = InsnList()
                                    replace.add(fromInt(when (ain.opcode) {
                                        IADD -> oneBefore.toInt() + twoBefore.toInt()
                                        ISUB -> twoBefore.toInt() - oneBefore.toInt()
                                        ISHR -> twoBefore.toInt() shr oneBefore.toInt()
                                        ISHL -> twoBefore.toInt() shl oneBefore.toInt()
                                        IXOR -> twoBefore.toInt() xor oneBefore.toInt()
                                        IOR -> twoBefore.toInt() or oneBefore.toInt()
                                        IAND -> twoBefore.toInt() and oneBefore.toInt()
                                        IUSHR -> twoBefore.toInt() ushr oneBefore.toInt()
                                        IREM -> twoBefore.toInt() % oneBefore.toInt()
                                        IDIV -> twoBefore.toInt() / oneBefore.toInt()
                                        IMUL -> twoBefore.toInt() * oneBefore.toInt()
                                        else -> throw IllegalStateException("WTF?")
                                    }))
                                    replacements[listOf(twoBefore, oneBefore, ain)] = replace
                                    constantIntCalc.getAndIncrement()
                                }
                            }
                            INEG -> {
                                val oneBefore = ain.previous
                                if(isConstantInt(oneBefore)){
                                    val replace = InsnList()
                                    replace.add(fromInt(-oneBefore.toInt()))
                                    replacements[listOf(oneBefore,ain)] = replace
                                    constantIntCalc.getAndIncrement()
                                }
                            }
                        }

                    }
                    replacements.forEach { (t, u) ->
                        run {
                            methodNode.instructions.insertBefore(t[0], u)
                            t.forEach { methodNode.instructions.remove(it) }
                        }
                    }
                } while (replacements.isNotEmpty())


            }
        }

        logger.info("removed {} nops",nops.get())
        logger.info("removed {} \"string\".length() calls",constantStringLength.get())
        logger.info("removed {} \"string\".hashCode() calls",constantStringHashCode.get())
        logger.info("removed {} useless arithemetic operations",constantIntCalc.get())
        return nops.get() + constantIntCalc.get() + constantStringHashCode.get() + constantStringLength.get() != 0
    }


}