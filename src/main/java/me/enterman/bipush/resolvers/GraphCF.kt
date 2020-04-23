package me.enterman.bipush.resolvers

import com.google.common.graph.EndpointPair
import com.google.common.graph.GraphBuilder
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.parse.Parser
import me.enterman.bipush.NodeUtils.terminates
import org.jgrapht.graph.guava.MutableGraphAdapter
import org.jgrapht.nio.dot.DOTExporter
import org.objectweb.asm.Label
import org.objectweb.asm.tree.*
import java.io.File
import java.io.StringWriter


// const val  = true
@Suppress("UnstableApiUsage")
class GraphCF : Resolver() {
    override fun doOn(classes: Map<String, ClassNode>): Boolean {
        classes.values.forEach { classNode ->
            classNode.methods.forEach { methodNode ->

                //val graph1 = mutGraph().setDirected(true)
                val graph = GraphBuilder
                        .directed()
                        .allowsSelfLoops(true)
                        .build<Int>()
                if(methodNode.instructions.first !is LabelNode)
                    graph.addNode(-1)

                val labelMap = mutableMapOf<Label,Int>()
                methodNode.instructions.forEach {
                    if(it is LabelNode){
                        graph.addNode(labelMap.size)
                        labelMap[it.label] = labelMap.size
                    }
                }
                methodNode.instructions.forEach { inst ->
                    when(inst){
                        is LabelNode -> {
                            val thisInt = labelMap[inst.label]!!
                            var prev = inst.previous
                            while(true){
                                if(prev == null || prev is LabelNode){
                                    if(graph.nodes().contains(thisInt - 1))
                                        graph.putEdge(thisInt - 1, thisInt)
                                    break
                                }
                                if(prev.terminates())
                                    break
                                prev = prev.previous
                            }
                        }
                        else -> {
                            val thisInt:Int
                            var prev = inst.previous
                            while (true){
                                if(prev == null){
                                    thisInt = -1
                                    break
                                }
                                if(prev is LabelNode){
                                    thisInt = labelMap[prev.label]!!
                                    break
                                }
                                prev = prev.previous
                            }
                            //@Suppress("Duplicates")
                            when (inst){
                                is JumpInsnNode -> {
                                    graph.putEdge(thisInt,labelMap[inst.label.label]!!)
                                }
                                is LookupSwitchInsnNode -> {
                                    inst.labels.forEach {
                                        graph.putEdge(thisInt,labelMap[it.label]!!)
                                    }
                                    graph.putEdge(thisInt,labelMap[inst.dflt.label]!!)
                                }
                                is TableSwitchInsnNode -> {
                                    inst.labels.forEach {
                                        graph.putEdge(thisInt,labelMap[it.label]!!)
                                    }
                                    graph.putEdge(thisInt,labelMap[inst.dflt.label]!!)
                                }
                            }
                        }

                    }
                }
                methodNode.tryCatchBlocks?.let{ list ->
                    list.forEach { node ->
                    val labels = mutableListOf<Label>(node.start.label)
                    var next = node.start.next
                    while(next != null){
                        if(next == node.end) break
                        if(next is LabelNode)
                            labels.add(next.label)
                        next = next.next
                    }
                    labels.map { label -> labelMap[label]!! }.forEach { from ->
                        graph.putEdge(from,labelMap[node.handler.label]!!)
                    }
                }}
                val exporter = DOTExporter<Int, EndpointPair<Int>>()
                val writer = StringWriter()
                exporter.exportGraph(MutableGraphAdapter(graph),writer)
                // println(writer.toString())
                val graph2 = Parser().read(writer.toString())
                graph2.nodes().forEach {
                    it.setName((Integer.parseInt(it.name().toString()) - 2).toString())
                }
                Graphviz.fromGraph(graph2)
                        .render(Format.PNG).toFile(File("photos/${classNode.name}/" +
                        methodNode.name
                                .replace("<","")
                                .replace(">","") +
                        methodNode.desc
                                .replace("/",".")
                                .replace("[","Arr")
                                .replace("(","")
                                .replace(")","-")
                                +
                        ".png"))
            }
        }
        return false
    }

}