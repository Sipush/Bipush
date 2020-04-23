package me.enterman.bipush.resolvers

import com.google.common.collect.MultimapBuilder
import com.google.common.graph.EndpointPair
import com.google.common.graph.GraphBuilder
import guru.nidi.graphviz.attribute.Attributes
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.model.Factory
import guru.nidi.graphviz.model.Factory.*
import guru.nidi.graphviz.model.Graph
import guru.nidi.graphviz.model.Link
import guru.nidi.graphviz.model.MutableGraph
import guru.nidi.graphviz.parse.Parser
import me.enterman.bipush.NodeUtils.terminates
import org.checkerframework.checker.units.qual.g
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.guava.MutableGraphAdapter
import org.jgrapht.nio.dot.DOTExporter
import org.objectweb.asm.Label
import org.objectweb.asm.tree.*
import java.io.File
import java.io.StringWriter
import javax.swing.JFrame


// const val  = true
@Suppress("UnstableApiUsage")
class GraphCF : Resolver() {
    override fun doOn(classes: Map<String, ClassNode>): Boolean {
        classes.values.forEach { classNode ->
            classNode.methods.forEach { methodNode ->


                val graph = GraphBuilder
                        .directed()
                        .allowsSelfLoops(true)
                        .build<Int>()
                val labelMap = mutableMapOf<Int,Label>()

                /**
                 * A map that stores referenced labels that has not giving an int yet
                 * Key: Label that is referenced
                 * Value(s): Labels that referred to another label
                 * */
                val accumulatingLabels = MultimapBuilder
                        .hashKeys()
                        .hashSetValues()
                        .build<Label,Int>()

// The initial start does not require a label
                labelMap[-1] = Label()
				graph.addNode(-1)
                methodNode.instructions.forEach { inst ->
                    when(inst){
                        is LabelNode -> {

                            var/*l*/ thisInt = labelMap.values.size - 1


                            // Go back to see if the previous label never goes to this label
                            var prev = inst.previous

                            while(true){
                                if(prev == null || prev is LabelNode){
                                    graph.putEdge(thisInt - 1, thisInt)
                                    break
                                }
                                if(prev.terminates())
                                    break
                                prev = prev.previous
                            }
							graph.addNode(thisInt)



                            labelMap[thisInt] = inst.label

                            if(accumulatingLabels.containsKey(inst.label)){
                                accumulatingLabels.get(inst.label).forEach {
                                    graph.putEdge(it,thisInt)
                                }
                                accumulatingLabels.removeAll(inst.label)
                            }

                        }
                        else -> {
                            val labelInt = labelMap.values.size - 2
                            @Suppress("Duplicates")
                            when (inst){
                                is JumpInsnNode -> {
                                    val numbers = labelMap.filterValues { it == inst.label.label }.keys
                                    if(numbers.isEmpty())
                                        accumulatingLabels.put(inst.label.label,labelInt)
                                    else
                                        numbers.forEach { graph.putEdge(labelInt,it) }
                                }

                                is LookupSwitchInsnNode -> {
                                    val numbers = labelMap.filterValues { label -> label == inst.dflt.label || inst.labels.map{it.label}.contains(label) }.keys
                                    if(numbers.isEmpty())
                                        accumulatingLabels.put(inst.dflt.label,labelInt)
                                    else
                                        numbers.forEach { graph.putEdge(labelInt,it) }
                                }
                                is TableSwitchInsnNode -> {
                                    val numbers = labelMap.filterValues { label -> label == inst.dflt.label || inst.labels.map{it.label}.contains(label) }.keys
                                    if(numbers.isEmpty())
                                        accumulatingLabels.put(inst.dflt.label,labelInt)
                                    else
                                        numbers.forEach { graph.putEdge(labelInt,it) }
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
                    labels.map { label -> labelMap.filterValues { it == label } }.map { it.keys }.flatten().forEach { from ->
                        labelMap.filterValues { it == node.handler.label }.keys.forEach {
                            graph.putEdge(from,it)
                        }

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
        // TODO Implement JUNG visualization
    }

}