package me.enterman.bipush.resolvers.general

import me.enterman.bipush.NodeUtils.insnListOf
import me.enterman.bipush.NodeUtils.isConstant
import me.enterman.bipush.NodeUtils.isConstantInt
import me.enterman.bipush.NodeUtils.toInt
import me.enterman.bipush.NodeUtils.toNode
import me.enterman.bipush.resolvers.SimpleReplacementResolver
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

class BipushFold : SimpleReplacementResolver() {
    //val logger = LoggerFactory.getLogger(this.javaClass)
    
    override fun doReplace(methodNode: MethodNode, replacements: MutableMap<List<AbstractInsnNode>, InsnList>) {
        //val nops = counters.addCounter("nop")
        //val constantStringLength = counters.addCounter("\"string\".length() call")
        ///val constantStringHashCode = counters.addCounter("\"string\".hashCode() call")
        //val constantIntCalc = counters.addCounter("useless arithemetic operation")
        methodNode.instructions.forEach {ain->
        
            
            //logger.debug("AIN is {}",i)
            when (ain.opcode) {
                NOP -> { // We don't want NOPs to get in our way, mainly because NOP does exactly nothing.
                    replacements[listOf(ain)] = InsnList()
                    //nops.getAndIncrement()
                }
                INVOKESTATIC -> {
                    if(ain is MethodInsnNode)
                        if(ain.owner == "java/lang/Integer")
                            if(ain.name == "reverse"){
                                val before = ain.previous?: return@forEach
                                if(before.isConstantInt()){
                                    replacements[listOf(before,ain)] = InsnList().also { it.add(toNode(Integer.reverse(before.toInt()))) }
                                }
                            }
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
                                            //constantStringLength.getAndIncrement()
                                        }
                            } else if(ain.name == "hashCode") {
                                val before = ain.previous
                                if (before.opcode == LDC)
                                    if (before is LdcInsnNode) // Constant String's hashCode is always constant.
                                        if (before.cst is String) {
                                            val replacement = InsnList()
                                            replacement.add(toNode((before.cst as String).hashCode()))
                                            replacements[listOf(before, ain)] = replacement
                                            //constantStringHashCode.getAndIncrement()
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
                    if (oneBefore.isConstantInt() && twoBefore.isConstantInt()) {
                        /* If the two opcodes before this arithmetic operational instruction are
							All defining a constant number, such as int a = 1+1, we will then make 'a' always 2.
					*/
                        val replace = InsnList()
                        replace.add(toNode(when (ain.opcode) {
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
                        //constantIntCalc.getAndIncrement()
                    }
                }
                INEG -> {
                    val oneBefore = ain.previous
                    if(oneBefore.isConstantInt()){
                        val replace = InsnList()
                        replace.add(toNode(-oneBefore.toInt()))
                        replacements[listOf(oneBefore,ain)] = replace
                        //constantIntCalc.getAndIncrement()
                    }
                }
                POP2 -> {
                    val oneBefore = ain.previous
                    val twoBefore = oneBefore?.previous
                    if(oneBefore?.opcode == DUP_X1 && twoBefore?.opcode == SWAP){
                        replacements[listOf(twoBefore,oneBefore,ain)] = InsnList().also{it.add(InsnNode(POP))}
                    } else if(oneBefore?.isConstant() == true && twoBefore?.isConstant() == true) {
                        replacements[listOf(twoBefore,oneBefore,ain)] = InsnList()
                    }
                }
                POP -> {
                    val oneBefore = ain.previous
                    if(oneBefore.isConstant())
                        replacements[listOf(oneBefore,ain)] = InsnList()
                }
                IFEQ -> {
                    val oneBefore = ain.previous
                    if(oneBefore.opcode == ICONST_0)
                        replacements[listOf(oneBefore,ain)] = insnListOf(JumpInsnNode(GOTO,(ain as JumpInsnNode).label))
                }
            }
        
        }
    }
    
    
}