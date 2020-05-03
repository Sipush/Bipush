package me.enterman.bipush.resolvers

import me.enterman.bipush.NodeUtils.isInstruction
import me.enterman.bipush.NodeUtils.nextNode
import me.enterman.bipush.asm.InstructionModifier
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.BasicValue
import org.objectweb.asm.tree.analysis.Frame

class DeadCodeRemover :Resolver(){
    @Throws(Throwable::class)
    override fun doOn(classes:MutableMap<String,ClassNode>): Boolean {
        var deadInstructions = 0
        for (classNode in classes.values) {
            for (methodNode in classNode.methods) {
                if (methodNode.instructions.first == null) continue
                val modifier = InstructionModifier()
                try {
                    val frames: Array<Frame<BasicValue>?> = Analyzer(BasicInterpreter()).analyze(classNode.name, methodNode)
                    for (i in 0 until methodNode.instructions.size()) {
                        if (!methodNode.instructions.get(i).isInstruction()) continue
                        if (frames[i] != null) continue
                        modifier.remove(methodNode.instructions.get(i))
                        deadInstructions++
                    }
                } catch (x: Exception) {
                    logger.error("Error analyzing frames for method ", x)
                    continue
                }
                modifier.apply(methodNode)

                // empty try catch nodes are illegal
                if (methodNode.tryCatchBlocks != null) {
                    methodNode.tryCatchBlocks.removeIf { tryCatchBlockNode -> tryCatchBlockNode.start.nextNode() === tryCatchBlockNode.end.nextNode() }
                }
            }
        }
        logger.info("Removed {} dead instructions", deadInstructions)
        return deadInstructions > 0
    }
}